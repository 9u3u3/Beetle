package com.bsbeetle.bananaspaghetti.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.utils.ai.AiRequest
import com.bsbeetle.bananaspaghetti.utils.ai.AiRole
import com.bsbeetle.bananaspaghetti.utils.ai.AiStreamException
import com.bsbeetle.bananaspaghetti.utils.ai.AiTurn
import com.bsbeetle.bananaspaghetti.utils.ai.LineDiff
import com.bsbeetle.bananaspaghetti.utils.preferences.AiPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * BYOK chat + apply-to-file engine for the editor. Two modes:
 * - [sendChat]: conversational coding help, streamed into [messages].
 * - [proposeEdit]: asks the model to return the FULL updated file; the result lands in
 *   [pendingEdit] for thumb diff-review (reuse DiffViewerSession) → accept writes it back.
 *
 * The ViewModel owns no managed key for any provider; everything runs on the user's own key via
 * [AiPreferences], so cost is the user's and predictable.
 *
 * Provider-specific wire formats live behind
 * [com.bsbeetle.bananaspaghetti.utils.ai.AiProvider]; this class only decides *what* to ask and
 * where the answer goes. Adding a provider should not require touching this file.
 */
class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AiPreferences(application)

    private val client =
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            }
                    )
                }
                install(HttpTimeout) {
                    // Providers override these per request; these are fallbacks.
                    requestTimeoutMillis = 120_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 120_000
                }
            }

    /**
     * What an edit did, rendered in the transcript instead of a bare "applied".
     *
     * The model rewrites the whole file, so the only way to see the change is to diff it — the
     * response itself gives no hint of which lines moved.
     */
    data class EditSummary(
            val fileName: String,
            val added: Int,
            val removed: Int,
            val preview: List<LineDiff.Line>
    )

    /** A single turn in the chat panel. [id] is stable for LazyColumn keying. */
    data class ChatUiMessage(
            val role: Role,
            val text: String,
            val edit: EditSummary? = null,
            val id: Long = idCounter.getAndIncrement()
    ) {
        enum class Role {
            USER,
            ASSISTANT
        }

        companion object {
            private val idCounter = java.util.concurrent.atomic.AtomicLong(0)
        }
    }

    /** A proposed whole-file rewrite awaiting accept/reject in the diff viewer. */
    data class PendingEdit(
            val filePath: String,
            val originalContent: String,
            val proposedContent: String
    )

    val messages = mutableStateListOf<ChatUiMessage>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pendingEdit = MutableStateFlow<PendingEdit?>(null)
    val pendingEdit: StateFlow<PendingEdit?> = _pendingEdit.asStateFlow()

    /**
     * Whether the assistant is taking over the editor pane.
     *
     * Lives here rather than in the editor screen because the toggle lives in the top bar and the
     * panel renders in the editor slot — two composables far apart in the tree, with no common
     * owner short of the screen root. Keeping it in the shared ViewModel also means the panel
     * survives a rotation with the conversation it belongs to.
     */
    private val _isPanelOpen = MutableStateFlow(false)
    val isPanelOpen: StateFlow<Boolean> = _isPanelOpen.asStateFlow()

    fun togglePanel() {
        _isPanelOpen.value = !_isPanelOpen.value
    }

    fun closePanel() {
        _isPanelOpen.value = false
    }

    /**
     * The edit most recently written into the editor, kept only so it can be undone.
     *
     * Edits land in the file immediately rather than behind a confirm dialog — that is the whole
     * point of asking the assistant to change a file, and a modal diff on a phone screen buries the
     * result behind a second decision. Holding the pre-edit content here is what makes that safe:
     * one tap puts the file back exactly as it was.
     */
    private val _lastApplied = MutableStateFlow<PendingEdit?>(null)
    val lastApplied: StateFlow<PendingEdit?> = _lastApplied.asStateFlow()

    fun noteApplied(edit: PendingEdit) {
        _lastApplied.value = edit
    }

    fun clearApplied() {
        _lastApplied.value = null
    }

    /** The in-flight request, so the user can stop a long generation. */
    private var activeJob: Job? = null

    fun isConfigured(): Boolean = prefs.isConfigured()

    /** Shown in the chat header so it's obvious which provider is answering. */
    fun activeProviderName(): String = prefs.provider.displayName

    fun activeModelName(): String {
        val model = prefs.model
        return prefs.provider.models.firstOrNull { it.id == model }?.displayName ?: model
    }

    fun clearError() {
        _error.value = null
    }

    fun clearConversation() {
        cancelActive()
        messages.clear()
    }

    fun dismissPendingEdit() {
        _pendingEdit.value = null
    }

    /** Stops the current generation. Partial output already streamed is kept. */
    fun cancelActive() {
        activeJob?.cancel()
        activeJob = null
        _isLoading.value = false
    }

    /** Conversational coding help. Optionally grounds the answer in the currently open file. */
    fun sendChat(userText: String, fileName: String? = null, fileContent: String? = null) {
        if (userText.isBlank()) return
        if (!prefs.isConfigured()) {
            _error.value = "Add a ${prefs.provider.displayName} API key in Settings → AI."
            return
        }

        messages.add(ChatUiMessage(ChatUiMessage.Role.USER, userText))

        // Send the full conversation so the model has context. Skip empty turns: a failed or
        // stopped request, or an edit summary, leaves an assistant bubble with no text, and the
        // providers reject a turn with empty content.
        val turns =
                messages.filter { it.text.isNotBlank() }.map {
                    AiTurn(
                            role =
                                    if (it.role == ChatUiMessage.Role.USER) AiRole.USER
                                    else AiRole.ASSISTANT,
                            text = it.text
                    )
                }

        // Placeholder assistant turn; deltas are written into it as they arrive.
        val placeholder = ChatUiMessage(ChatUiMessage.Role.ASSISTANT, "")
        messages.add(placeholder)
        val slot = messages.lastIndex

        run(
                request =
                        AiRequest(
                                model = prefs.model,
                                systemInstructions = CHAT_INSTRUCTIONS,
                                fileName = fileName,
                                fileContent = fileContent,
                                turns = turns,
                                maxTokens = MAX_TOKENS
                        ),
                onDelta = { soFar ->
                    if (slot <= messages.lastIndex && messages[slot].id == placeholder.id) {
                        messages[slot] = placeholder.copy(text = soFar)
                    }
                },
                // The placeholder already holds the full text by the time the stream ends.
                onComplete = {},
                onFailure = { removeIfEmpty(placeholder.id) }
        )
    }

    /**
     * Ask the model to rewrite [fileContent] per [instruction] and return the FULL updated file.
     * The result is staged in [pendingEdit] for diff review — it is NOT written to disk here.
     */
    fun proposeEdit(instruction: String, filePath: String, fileName: String, fileContent: String) {
        if (instruction.isBlank()) return
        if (!prefs.isConfigured()) {
            _error.value = "Add a ${prefs.provider.displayName} API key in Settings → AI."
            return
        }

        val userMsg = buildString {
            append("File: `$fileName`\n\n")
            append(fileContent)
            append("\n\nInstruction: ")
            append(instruction)
        }

        // The transcript shows the instruction and the outcome, never the inlined file — echoing a
        // whole file back into a phone-sized chat log buries everything else in it.
        messages.add(ChatUiMessage(ChatUiMessage.Role.USER, instruction))
        val placeholder = ChatUiMessage(ChatUiMessage.Role.ASSISTANT, "Editing $fileName…")
        messages.add(placeholder)
        val slot = messages.lastIndex

        run(
                request =
                        AiRequest(
                                model = prefs.model,
                                systemInstructions = EDIT_INSTRUCTIONS,
                                // The file is inlined in the user turn here rather than attached as
                                // context, because the model is being asked to reproduce it rather
                                // than reason about it.
                                turns = listOf(AiTurn(AiRole.USER, userMsg)),
                                maxTokens = MAX_TOKENS
                        ),
                onDelta = null,
                onComplete = { raw ->
                    val updated = extractFileContent(raw)
                    val diff = LineDiff.of(fileContent, updated)
                    if (slot <= messages.lastIndex && messages[slot].id == placeholder.id) {
                        messages[slot] =
                                placeholder.copy(
                                        text =
                                                if (diff.added == 0 && diff.removed == 0)
                                                        "No changes needed in $fileName"
                                                else "",
                                        edit =
                                                if (diff.added == 0 && diff.removed == 0) null
                                                else
                                                        EditSummary(
                                                                fileName = fileName,
                                                                added = diff.added,
                                                                removed = diff.removed,
                                                                preview = diff.preview
                                                        )
                                )
                    }
                    // Nothing changed: don't overwrite the file (and don't arm an undo that would
                    // restore the same bytes) just to prove the model was asked.
                    if (diff.added == 0 && diff.removed == 0) return@run

                    _pendingEdit.value =
                            PendingEdit(
                                    filePath = filePath,
                                    originalContent = fileContent,
                                    proposedContent = updated
                            )
                },
                onFailure = {
                    val index = messages.indexOfFirst { it.id == placeholder.id }
                    if (index >= 0) {
                        messages[index] = placeholder.copy(text = "Edit of $fileName did not finish.")
                    }
                }
        )
    }

    /** Removes an assistant bubble that never received text, so it does not show as a blank box. */
    private fun removeIfEmpty(id: Long) {
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0 && messages[index].text.isBlank() && messages[index].edit == null) {
            messages.removeAt(index)
        }
    }

    /**
     * Runs a streaming request against the active provider. [onDelta] receives the accumulated text
     * as it grows (null for modes that only care about the final result); [onComplete] receives the
     * full text once.
     *
     * Collection happens on the main dispatcher while each provider's network read and parsing run
     * on IO, so the Compose state writes in the callbacks stay on main.
     */
    private fun run(
            request: AiRequest,
            onDelta: ((String) -> Unit)?,
            onComplete: (String) -> Unit,
            onFailure: () -> Unit
    ) {
        cancelActive()

        val provider = prefs.provider
        val apiKey = prefs.apiKey

        activeJob =
                viewModelScope.launch {
                    _isLoading.value = true
                    _error.value = null

                    val accumulated = StringBuilder()
                    try {
                        provider.streamChat(client, apiKey, request).collect { chunk ->
                            accumulated.append(chunk)
                            onDelta?.invoke(accumulated.toString())
                        }

                        val text = accumulated.toString()
                        if (text.isBlank()) {
                            _error.value = "${provider.displayName} returned an empty response."
                            onFailure()
                        } else {
                            onComplete(text)
                        }
                    } catch (e: CancellationException) {
                        // Stop keeps partial chat output. An edit with no full file is not usable.
                        if (onDelta == null || accumulated.isBlank()) onFailure()
                        throw e
                    } catch (e: AiStreamException) {
                        _error.value = e.message
                        if (onDelta == null || accumulated.isBlank()) onFailure()
                    } catch (e: Exception) {
                        Log.e(TAG, "AI request failed (${provider.id})", e)
                        _error.value = "Request failed: ${e.message}"
                        if (onDelta == null || accumulated.isBlank()) onFailure()
                    } finally {
                        // A newer request may have replaced this one. Only the current job may
                        // clear the shared state, or the newer request loses its Stop button.
                        if (activeJob === coroutineContext[Job]) {
                            _isLoading.value = false
                            activeJob = null
                        }
                    }
                }
    }

    /**
     * Pull the file body out of a model response.
     *
     * Models add a lead-in ("Here's the updated file:") and fence the code no matter how firmly
     * they are told not to. The old version only stripped fences when the response *started* with
     * one, so any prose before the fence was written into the source file verbatim — which is how
     * shell commands and English sentences ended up in a .c file.
     *
     * So: if the response contains fenced blocks, the file is the largest of them. Only a response
     * with no fences at all is taken literally.
     */
    internal fun extractFileContent(raw: String): String {
        val blocks = mutableListOf<String>()
        val buffer = StringBuilder()
        var inFence = false

        raw.lines().forEach { line ->
            if (line.trimStart().startsWith("```")) {
                if (inFence) {
                    blocks += buffer.toString()
                    buffer.setLength(0)
                }
                inFence = !inFence
            } else if (inFence) {
                buffer.append(line).append('\n')
            }
        }
        // An unterminated fence still holds the file — the model just ran out of tokens closing it.
        if (inFence && buffer.isNotBlank()) blocks += buffer.toString()

        val best = blocks.maxByOrNull { it.length }
        return (best ?: raw).trim('\n')
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }

    companion object {
        private const val TAG = "AiChatViewModel"

        private const val CHAT_INSTRUCTIONS =
                "You are a coding assistant inside Beetle, a mobile IDE running a real Linux " +
                        "environment on the user's Android phone. Be concise — the user is on a " +
                        "small screen. Prefer short, copy-pasteable code."

        private const val EDIT_INSTRUCTIONS =
                "You are a code-editing assistant. The user will give you a file and an " +
                        "instruction. Respond with ONLY the complete, updated contents of the file — " +
                        "no explanations, no commentary, and no markdown code fences. Preserve the " +
                        "file's existing style and indentation. Your output is written straight " +
                        "into the open editor buffer, so it must be the file itself: never a shell " +
                        "command that writes the file (no `cat > file <<EOF`), never a build or run " +
                        "command, and never a diff or patch."

        // Streaming lifts the timeout ceiling that capped this before, so there is room for a whole
        // rewritten file. Kept well below provider maximums to bound worst-case spend on the user's key.
        private const val MAX_TOKENS = 32_000
    }
}
