package com.bsbeetle.bananaspaghetti.utils.ai.agent

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Runs the agent's shell commands in the Ubuntu environment, in a terminal the user does not see.
 *
 * The command output goes to a file, not through the terminal screen. That keeps prompts, echo and
 * colour codes out of the result, and gives a real exit code. stdin is `/dev/null`, so a command
 * that waits for input ends instead of hanging.
 */
class CommandRunner(context: Context) {

    private val appContext = context.applicationContext
    private val workDir = File(appContext.filesDir, "tmp/ai-agent")
    private val mutex = Mutex()
    private var session: TerminalSession? = null
    private var shellReady = false
    private var counter = 0

    val isAvailable: Boolean
        get() = File(appContext.filesDir, "ubuntu/bin/bash").exists()

    suspend fun run(command: String, cwd: File, timeoutSeconds: Int): ToolOutcome =
            mutex.withLock {
                if (!isAvailable) {
                    return ToolOutcome(
                            "The Ubuntu environment is not installed, so commands cannot run. Ask " +
                                    "the user to open a terminal tab once to install it.",
                            isError = true
                    )
                }
                val terminal = obtainSession()
                workDir.mkdirs()
                val id = "${System.currentTimeMillis()}-${counter++}"
                val script = File(workDir, "cmd-$id.sh")
                val out = File(workDir, "out-$id.txt")
                val exit = File(workDir, "exit-$id")
                script.writeText(command + "\n")

                val line =
                        "(cd ${quote(cwd.path)} && exec bash ${quote(script.path)}) </dev/null " +
                                ">${quote(out.path)} 2>&1; echo \$? >${quote(exit.path)}.tmp; " +
                                "mv ${quote(exit.path)}.tmp ${quote(exit.path)}\n"

                val started = System.currentTimeMillis()
                try {
                    terminal.insertText(line)
                    val deadline = started + timeoutSeconds * 1000L
                    while (!exit.exists() && System.currentTimeMillis() < deadline) delay(200)

                    val timedOut = !exit.exists()
                    if (timedOut) interrupt(terminal, exit)
                    val code = runCatching { exit.readText().trim().toInt() }.getOrNull()
                    // bash names the script file in its errors. Show "bash" instead of the temporary path.
                    val output =
                            withContext(Dispatchers.IO) {
                                clean(if (out.exists()) out.readText() else "").replace(script.path, "bash")
                            }
                    val seconds = (System.currentTimeMillis() - started) / 1000.0

                    val summary =
                            when {
                                timedOut -> "Timed out after $timeoutSeconds s. The command was stopped."
                                else -> "Exit code $code (${"%.1f".format(seconds)} s)."
                            }
                    ToolOutcome(
                            "$summary\n${output.ifBlank { "(no output)" }}",
                            isError = timedOut || code != 0
                    )
                } catch (e: CancellationException) {
                    withContext(NonCancellable) { interrupt(terminal, exit) }
                    throw e
                } finally {
                    script.delete()
                    out.delete()
                    exit.delete()
                }
            }

    /** Sends Ctrl-C, and replaces the shell if the command does not stop. */
    private suspend fun interrupt(terminal: TerminalSession, exit: File) {
        terminal.insertText("\u0003")
        repeat(15) {
            if (exit.exists()) return
            delay(200)
        }
        Log.w(TAG, "Command ignored Ctrl-C. Starting a new shell.")
        close()
    }

    private suspend fun obtainSession(): TerminalSession {
        session?.let { if (shellReady) return it }

        val created =
                withContext(Dispatchers.Main) {
                    val active = SessionList.activeSessionIndex
                    val terminal = TerminalSession(appContext, "AI-Agent")
                    // The constructor adds the session to the tab list. This terminal has no tab.
                    SessionList.sessions.remove(terminal)
                    SessionList.activeSessionIndex = active
                    terminal.onProcessExited = { shellReady = false }
                    terminal
                }
        session = created

        // Wait for the first prompt. Input sent earlier stays in the terminal buffer, so this is
        // only to let the init script finish first.
        var prompted = false
        created.onOutput = { text -> if (text.contains("$ ") || text.contains("# ")) prompted = true }
        val deadline = System.currentTimeMillis() + 20_000
        while (!prompted && System.currentTimeMillis() < deadline) delay(100)
        created.onOutput = null
        shellReady = true
        return created
    }

    /** Stops the shell. The next command starts a new one. */
    suspend fun close() = withContext(Dispatchers.Main.immediate) { release() }

    /**
     * Stops the shell now. Call it on the main thread.
     *
     * Do not wrap close() in runBlocking on the main thread instead. close() then waits for the
     * main thread, the main thread waits for close(), and the app shows a white screen.
     */
    fun release() {
        val terminal = session ?: return
        session = null
        shellReady = false
        val active = SessionList.activeSessionIndex
        terminal.destroy()
        // destroy() moves the active tab to the last one. Put it back.
        SessionList.activeSessionIndex =
                active?.takeIf { it < SessionList.sessions.size } ?: SessionList.activeSessionIndex
    }

    companion object {
        private const val TAG = "CommandRunner"
        private const val MAX_CHARS = 30_000
        private val ANSI = Regex("\u001B\\[[0-?]*[ -/]*[@-~]|\u001B\\][^\u0007]*\u0007|\r")

        fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"

        /** Removes colour codes and keeps the start and the end of a long output. */
        fun clean(raw: String): String {
            val text = ANSI.replace(raw, "")
            if (text.length <= MAX_CHARS) return text
            val head = text.take(MAX_CHARS / 3)
            val tail = text.takeLast(MAX_CHARS * 2 / 3)
            return "$head\n… (${text.length - MAX_CHARS} characters left out) …\n$tail"
        }
    }
}
