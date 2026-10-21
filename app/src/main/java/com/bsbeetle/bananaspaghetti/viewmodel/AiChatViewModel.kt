package com.bsbeetle.bananaspaghetti.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.utils.ai.AiProviders
import com.bsbeetle.bananaspaghetti.utils.ai.AiStreamException
import com.bsbeetle.bananaspaghetti.utils.ai.LineDiff
import com.bsbeetle.bananaspaghetti.utils.ai.agent.AgentConversation
import com.bsbeetle.bananaspaghetti.utils.ai.agent.AnthropicAgent
import com.bsbeetle.bananaspaghetti.utils.ai.agent.CommandRunner
import com.bsbeetle.bananaspaghetti.utils.ai.agent.FileChange
import com.bsbeetle.bananaspaghetti.utils.ai.agent.GeminiAgent
import com.bsbeetle.bananaspaghetti.utils.ai.agent.StopKind
import com.bsbeetle.bananaspaghetti.utils.ai.agent.ToolCall
import com.bsbeetle.bananaspaghetti.utils.ai.agent.ToolOutcome
import com.bsbeetle.bananaspaghetti.utils.ai.agent.WorkspaceTools
import com.bsbeetle.bananaspaghetti.utils.preferences.AiPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The AI agent for the editor.
 *
 * The model works on the open project through [WorkspaceTools]: it lists, reads, searches and
 * edits files, and it runs shell commands after the user allows each one. This class runs the
 * loop: send the conversation, run the tools the model asks for, send the results back, and repeat
 * until the model answers without a tool call.
 *
 * Beetle holds no managed key. Every request uses the user's own key from [AiPreferences].
 */
class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AiPreferences(application)

    private val client =
            HttpClient(OkHttp) {
                install(HttpTimeout) {
                    // Each request sets its own streaming timeouts. These are fallbacks.
                    requestTimeoutMillis = 120_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 120_000
                }
            }

    private val commandRunner = CommandRunner(application)

    /** What the agent works on. The panel builds this from the open project and file. */
    data class Workspace(
            val rootPath: String,
            val openFilePath: String?,
            val openFileContent: String?
    )

    /** One row of the transcript. [id] is stable for LazyColumn keys. */
    sealed interface Item {
        val id: Long

        data class User(val text: String, override val id: Long = nextId()) : Item

        data class Assistant(val text: String, override val id: Long = nextId()) : Item

        /** A read-only tool call: list, read or search. */
        data class Activity(
                val label: String,
                val detail: String?,
                val state: State,
                override val id: Long = nextId()
        ) : Item

        data class Edit(
                val change: FileChange,
                val added: Int,
                val removed: Int,
                val preview: List<LineDiff.Line>,
                val undone: Boolean = false,
                override val id: Long = nextId()
        ) : Item

        data class Command(
                val command: String,
                val state: State,
                val output: String? = null,
                override val id: Long = nextId()
        ) : Item

        data class Notice(val text: String, val isError: Boolean = false, override val id: Long = nextId()) : Item
    }

    enum class State {
        WAITING_APPROVAL,
        RUNNING,
        DONE,
        FAILED,
        DENIED
    }

    val items = mutableStateListOf<Item>()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** Set while a command waits for the user. Allow or Deny completes it. */
    private var approval: CompletableDeferred<Boolean>? = null

    private val _allowAllCommands = MutableStateFlow(false)
    val allowAllCommands: StateFlow<Boolean> = _allowAllCommands.asStateFlow()

    /**
     * Each file write, for the panel to copy into an open editor. The panel owns the editor
     * WebViews, so it does that part.
     */
    private val _fileWrites = MutableSharedFlow<FileChange>(extraBufferCapacity = 32)
    val fileWrites: SharedFlow<FileChange> = _fileWrites.asSharedFlow()

    private val _isPanelOpen = MutableStateFlow(false)
    val isPanelOpen: StateFlow<Boolean> = _isPanelOpen.asStateFlow()

    fun togglePanel() {
        _isPanelOpen.value = !_isPanelOpen.value
    }

    fun closePanel() {
        _isPanelOpen.value = false
    }

    private var conversation: AgentConversation? = null
    private var conversationKey: String? = null
    private var tools: WorkspaceTools? = null
    private var activeJob: Job? = null

    /** The open file as the editor holds it. The agent reads this instead of the disk copy. */
    private var liveFile: Pair<String, String>? = null

    fun isConfigured(): Boolean = prefs.isConfigured()

    fun activeModelName(): String {
        val model = prefs.model
        return prefs.provider.models.firstOrNull { it.id == model }?.displayName ?: model
    }

    fun clearConversation() {
        stop()
        items.clear()
        conversation = null
        conversationKey = null
        _allowAllCommands.value = false
        viewModelScope.launch { commandRunner.close() }
    }

    fun answerApproval(allow: Boolean, allowAll: Boolean = false) {
        if (allowAll) _allowAllCommands.value = true
        approval?.complete(allow)
    }

    /** Stops the agent. Work already done stays done. */
    fun stop() {
        activeJob?.cancel()
        activeJob = null
        approval?.cancel()
        approval = null
        _isRunning.value = false
    }

    fun send(text: String, workspace: Workspace) {
        if (text.isBlank() || _isRunning.value) return
        if (!prefs.isConfigured()) {
            items += Item.Notice("Add a ${prefs.provider.displayName} API key in Settings → AI.", isError = true)
            return
        }
        val root = File(workspace.rootPath)
        if (!root.isDirectory || !root.canRead()) {
            items += Item.Notice("Beetle cannot read ${workspace.rootPath}. Allow all files access for Beetle in Android settings.", isError = true)
            return
        }

        liveFile =
                if (workspace.openFilePath != null && workspace.openFileContent != null)
                        workspace.openFilePath to workspace.openFileContent
                else null

        // A new project, provider or model starts a new conversation. The old one refers to
        // other files, and one provider cannot read another provider's history.
        val key = "${prefs.providerId}|${prefs.model}|${root.canonicalPath}"
        if (conversation == null || key != conversationKey) {
            if (conversation != null) items += Item.Notice("New conversation: the project or model changed.")
            val workspaceTools =
                    WorkspaceTools(
                            root = root,
                            liveContent = { path -> liveFile?.takeIf { it.first == path }?.second },
                            runCommand = { command, cwd, timeout -> commandRunner.run(command, cwd, timeout) },
                            approveCommand = ::askApproval,
                            onFileChanged = ::recordWrite
                    )
            tools = workspaceTools
            conversation = newConversation(systemPrompt(workspaceTools.root), workspaceTools)
            conversationKey = key
        }

        items += Item.User(text)
        val prompt = buildString {
            takeNotes()?.let { append("[$it]\n") }
            workspace.openFilePath?.let { path ->
                val rel = runCatching { File(path).relativeTo(root.canonicalFile).path }.getOrNull()
                if (rel != null && !rel.startsWith("..")) append("[Open in the editor: $rel]\n\n")
            }
            append(text)
        }
        conversation!!.addUserText(prompt)
        runLoop()
    }

    private fun newConversation(system: String, tools: WorkspaceTools): AgentConversation {
        val apiKey = prefs.apiKey
        val model = prefs.model
        return when (prefs.providerId) {
            AiProviders.GEMINI -> GeminiAgent(client, apiKey, model, system, tools.specs)
            else -> AnthropicAgent(client, apiKey, model, system, tools.specs)
        }
    }

    private fun runLoop() {
        val conv = conversation ?: return
        val workspaceTools = tools ?: return
        activeJob =
                viewModelScope.launch {
                    _isRunning.value = true
                    // Tool calls of the last turn that still need a result. The API rejects a
                    // history with an unanswered call, so a stop must answer them.
                    var unanswered: List<ToolCall> = emptyList()
                    val answered = mutableListOf<Pair<ToolCall, ToolOutcome>>()
                    try {
                        var rounds = 0
                        while (true) {
                            if (rounds++ >= MAX_ROUNDS) {
                                items += Item.Notice("Stopped after $MAX_ROUNDS steps. Send \"continue\" to let it go on.")
                                break
                            }
                            val textItem = Item.Assistant("")
                            items += textItem
                            val streamed = StringBuilder()
                            val turn =
                                    withRetry {
                                        // A failed attempt adds nothing to the history. Clear its
                                        // partial text before the next attempt.
                                        streamed.clear()
                                        replace(textItem.id) { (it as Item.Assistant).copy(text = "") }
                                        conv.runTurn { delta ->
                                            streamed.append(delta)
                                            replace(textItem.id) { (it as Item.Assistant).copy(text = streamed.toString()) }
                                        }
                                    }
                            if (streamed.isBlank()) remove(textItem.id)

                            if (turn.toolCalls.isEmpty()) {
                                if (turn.stop == StopKind.MAX_TOKENS) {
                                    items += Item.Notice("The reply reached the length limit and is cut off.")
                                }
                                break
                            }

                            unanswered = turn.toolCalls
                            answered.clear()
                            for (call in turn.toolCalls) {
                                val outcome =
                                        if (turn.stop == StopKind.MAX_TOKENS) {
                                            // The input may be cut off. Never run it.
                                            ToolOutcome(
                                                    "Your reply reached the output limit, so this tool call is incomplete and did not run. Do the work in smaller steps, for example several edit_file calls instead of one large write_file.",
                                                    isError = true
                                            )
                                        } else {
                                            runTool(call, workspaceTools)
                                        }
                                answered += call to outcome
                            }
                            conv.addToolResults(answered.toList())
                            unanswered = emptyList()
                        }
                    } catch (e: CancellationException) {
                        withContext(NonCancellable) {
                            items += Item.Notice("Stopped.")
                        }
                        throw e
                    } catch (e: AiStreamException) {
                        items += Item.Notice(e.message ?: "The AI request failed.", isError = true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Agent loop failed", e)
                        items += Item.Notice("Request failed: ${e.message ?: e.javaClass.simpleName}", isError = true)
                    } finally {
                        if (unanswered.isNotEmpty()) {
                            val done = answered.map { it.first.id }.toSet()
                            conv.addToolResults(
                                    answered.toList() +
                                            unanswered.filter { it.id !in done }.map {
                                                it to ToolOutcome("The user stopped this before it ran.", isError = true)
                                            }
                            )
                        }
                        items.lastOrNull()?.let { last ->
                            if (last is Item.Assistant && last.text.isBlank()) remove(last.id)
                        }
                        if (activeJob === coroutineContext[Job]) {
                            _isRunning.value = false
                            activeJob = null
                        }
                    }
                }
    }

    /**
     * Runs [block] again after a short wait when it fails with a network error or a retryable
     * service error. Phones often lose the network for a moment.
     */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val retryable = (e as? AiStreamException)?.retryable ?: isNetworkError(e)
                if (!retryable || attempt >= RETRY_DELAYS_MS.size) throw e
                val wait = RETRY_DELAYS_MS[attempt++]
                Log.w(TAG, "Request failed, attempt $attempt. Trying again in $wait ms.", e)
                val notice = Item.Notice("Connection problem. Trying again (${attempt}/${RETRY_DELAYS_MS.size}).")
                items += notice
                delay(wait)
                remove(notice.id)
            }
        }
    }

    private fun isNetworkError(e: Throwable): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is java.net.UnknownHostException ||
                            cause is java.net.ConnectException ||
                            cause is java.net.SocketException ||
                            cause is javax.net.ssl.SSLException ||
                            cause is java.io.EOFException
            ) return true
            cause = cause.cause
        }
        return false
    }

    private suspend fun runTool(call: ToolCall, workspaceTools: WorkspaceTools): ToolOutcome {
        val input = call.input
        fun arg(name: String) = (input?.get(name) as? JsonPrimitive)?.contentOrNull

        return when (call.name) {
            "edit_file", "write_file" -> {
                val outcome = withContext(Dispatchers.IO) { workspaceTools.execute(call) }
                if (outcome.isError) {
                    items += Item.Activity("${if (call.name == "edit_file") "Edit" else "Write"} ${arg("path") ?: "?"}", outcome.content, State.FAILED)
                }
                outcome
            }
            "run_command" -> {
                val item = Item.Command(arg("command") ?: call.rawInput, State.RUNNING)
                items += item
                currentCommandId = item.id
                val outcome = workspaceTools.execute(call)
                currentCommandId = null
                replace(item.id) {
                    val current = it as Item.Command
                    if (current.state == State.DENIED) current
                    else current.copy(state = if (outcome.isError) State.FAILED else State.DONE, output = outcome.content)
                }
                outcome
            }
            else -> {
                val label =
                        when (call.name) {
                            "list_files" -> "List ${arg("path") ?: "project"}"
                            "read_file" -> {
                                val start = arg("start_line")?.takeIf { it != "1" }
                                val end = arg("end_line")
                                "Read ${arg("path") ?: "?"}" +
                                        when {
                                            start != null && end != null -> " lines $start-$end"
                                            start != null -> " from line $start"
                                            end != null -> " to line $end"
                                            else -> ""
                                        }
                            }
                            "search_files" -> "Search /${arg("pattern") ?: ""}/"
                            else -> call.name
                        }
                val item = Item.Activity(label, null, State.RUNNING)
                items += item
                val outcome = withContext(Dispatchers.IO) { workspaceTools.execute(call) }
                replace(item.id) {
                    (it as Item.Activity).copy(
                            state = if (outcome.isError) State.FAILED else State.DONE,
                            detail = if (outcome.isError) outcome.content else null
                    )
                }
                outcome
            }
        }
    }

    private var currentCommandId: Long? = null

    private suspend fun askApproval(command: String): Boolean {
        val id = currentCommandId
        if (_allowAllCommands.value) return true
        id?.let { replace(it) { item -> (item as Item.Command).copy(state = State.WAITING_APPROVAL) } }
        val decision = CompletableDeferred<Boolean>()
        approval = decision
        val allowed = try {
            decision.await()
        } finally {
            approval = null
        }
        id?.let {
            replace(it) { item -> (item as Item.Command).copy(state = if (allowed) State.RUNNING else State.DENIED) }
        }
        return allowed
    }

    /** Runs on the IO thread inside a tool. Moves the UI work to the main thread. */
    private fun recordWrite(change: FileChange) {
        if (liveFile?.first == change.path) liveFile = change.path to change.after
        viewModelScope.launch {
            val diff = LineDiff.of(change.before ?: "", change.after)
            items += Item.Edit(change, diff.added, diff.removed, diff.preview)
            _fileWrites.emit(change)
        }
    }

    /** Puts a file back as it was before an edit. A file the agent created is deleted. */
    fun undo(edit: Item.Edit) {
        if (edit.undone) return
        viewModelScope.launch {
            val change = edit.change
            val file = File(change.path)
            val ok =
                    withContext(Dispatchers.IO) {
                        runCatching {
                                    if (change.before == null) file.delete()
                                    else file.writeText(change.before, Charsets.UTF_8)
                                }
                                .isSuccess
                    }
            if (!ok) {
                items += Item.Notice("Could not undo the change to ${change.relativePath}.", isError = true)
                return@launch
            }
            replace(edit.id) { (it as Item.Edit).copy(undone = true) }
            val restored = change.before ?: ""
            if (liveFile?.first == change.path) liveFile = change.path to restored
            _fileWrites.emit(FileChange(change.path, change.relativePath, change.after, restored))
            // Tell the model at its next turn, so it does not build on the removed change.
            pendingNotes += "The user undid your change to ${change.relativePath}."
        }
    }

    private val pendingNotes = mutableListOf<String>()

    /** Notes for the model about things the user did between messages, such as an undo. */
    private fun takeNotes(): String? = pendingNotes.takeIf { it.isNotEmpty() }?.joinToString("\n")?.also { pendingNotes.clear() }

    private fun replace(id: Long, transform: (Item) -> Item) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) items[index] = transform(items[index])
    }

    private fun remove(id: Long) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) items.removeAt(index)
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
        // onCleared runs on the main thread. This also stops the "Beetle Terminal" notification
        // when the agent shell is the last terminal.
        commandRunner.release()
    }

    companion object {
        private const val TAG = "AiChatViewModel"
        private const val MAX_ROUNDS = 40
        private val RETRY_DELAYS_MS = longArrayOf(2_000, 5_000)
        private val idCounter = AtomicLong(0)

        private fun nextId() = idCounter.getAndIncrement()

        private fun systemPrompt(root: File) =
                """
                You are the coding agent inside Beetle, a code editor on the user's Android phone.
                You work on the user's project with tools. The project root is ${root.canonicalPath}. Tool paths are relative to that root.

                How to work:
                - Find out before you change. Use list_files, search_files and read_file to learn the code you touch.
                - Change files with edit_file. Use write_file only for new files or full rewrites.
                - After a change, check it when you can: build, run or test it with run_command. Each command needs the user's approval, so run only commands that help the task.
                - run_command runs bash in Ubuntu 24.04 (arm64) through proot, as root. apt works. There is no GUI and no stdin.
                - Do not run destructive commands (rm -rf, git reset --hard, force push) unless the user asks for them.
                - If a request is unclear, ask one short question instead of guessing.

                How to answer:
                - The user reads your reply on a phone screen. Keep it short. Use short paragraphs and lists.
                - Say what you changed and why, in a few lines. The app already shows each file change as a diff, so do not repeat the changed code.
                - Use fenced code blocks only for code the user must see.
                """.trimIndent()
    }
}
