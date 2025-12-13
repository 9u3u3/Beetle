package com.bsbeetle.bananaspaghetti.directGit.utils

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * GitTerminalManager - Manages a single persistent Tenebrio terminal session for all Git
 * operations.
 *
 * This provides:
 * - ONE persistent TerminalSession for all Git commands
 * - Visual feedback - commands shown in terminal UI
 * - SSH key auto-configuration in Alpine
 * - Command output capture
 * - Real-time progress display
 */
class GitTerminalManager(private val context: Context) {

    companion object {
        private const val TAG = "GitTerminalManager"
        private const val COMMAND_TIMEOUT = 5000L // 5 seconds
    }

    private var terminalSession: TerminalSession? = null
    private val commandHistory = mutableListOf<String>()

    /**
     * Initialize the terminal session (must be called before any operations) NOTE: TerminalSession
     * uses WebView which MUST be created on the Main thread
     */
    suspend fun initialize(): Result<Unit> {
        try {
            if (terminalSession != null) {
                Log.d(TAG, "♻️ Terminal session already initialized")
                return Result.success(Unit)
            }

            Log.d(TAG, "🔧 Initializing Tenebrio terminal session...")

            // CRITICAL: TerminalSession must be created on Main thread (WebView requirement)
            withContext(Dispatchers.Main) { terminalSession = TerminalSession(context) }

            Log.d(TAG, "✅ Terminal session initialized successfully")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize terminal", e)
            return Result.failure(e)
        }
    }

    /** Setup SSH keys in Alpine's ~/.ssh/ directory */
    suspend fun setupSshKeys(privateKey: String, publicKey: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    initialize().getOrThrow()

                    val session =
                            terminalSession
                                    ?: return@withContext Result.failure(
                                            Exception("Terminal not initialized")
                                    )

                    addToHistory("🔑 Setting up SSH keys in Alpine...")

                    // Create .ssh directory
                    Log.d(TAG, "📁 Creating ~/.ssh directory...")
                    session.insertText("mkdir -p ~/.ssh\n")
                    delay(300)
                    session.insertText("chmod 700 ~/.ssh\n")
                    delay(300)

                    // Write private key directly to Alpine filesystem
                    Log.d(TAG, "🔐 Writing private key...")
                    val alpinePrivateKeyPath = File(context.filesDir, "alpine/root/.ssh/id_ed25519")
                    alpinePrivateKeyPath.parentFile?.mkdirs()
                    alpinePrivateKeyPath.writeText(privateKey)
                    session.insertText("chmod 600 ~/.ssh/id_ed25519\n")
                    delay(300)
                    addToHistory("✅ Private key configured")

                    // Write public key
                    Log.d(TAG, "🔓 Writing public key...")
                    val alpinePublicKeyPath =
                            File(context.filesDir, "alpine/root/.ssh/id_ed25519.pub")
                    alpinePublicKeyPath.writeText(publicKey)
                    session.insertText("chmod 644 ~/.ssh/id_ed25519.pub\n")
                    delay(300)
                    addToHistory("✅ Public key configured")

                    // Create SSH config
                    Log.d(TAG, "⚙️ Creating SSH config...")
                    val sshConfig =
                            """
                Host github.com
                    HostName github.com
                    User git
                    IdentityFile ~/.ssh/id_ed25519
                    StrictHostKeyChecking no
                    UserKnownHostsFile /dev/null
            """.trimIndent()

                    val alpineSshConfigPath = File(context.filesDir, "alpine/root/.ssh/config")
                    alpineSshConfigPath.writeText(sshConfig)
                    session.insertText("chmod 600 ~/.ssh/config\n")
                    delay(300)
                    addToHistory("✅ SSH config created")

                    Log.d(TAG, "🎉 SSH keys setup complete!")
                    addToHistory("🎉 SSH authentication ready!")

                    Result.success(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to setup SSH keys", e)
                    addToHistory("❌ SSH setup failed: ${e.message}")
                    Result.failure(e)
                }
            }

    /**
     * Execute a Git command in the Alpine environment
     *
     * @param repoPath Absolute path to the Git repository
     * @param command Git command to execute (e.g., "git push origin main")
     * @param showInTerminal Whether to show command in terminal UI
     */
    suspend fun executeGitCommand(
            repoPath: String,
            command: String,
            showInTerminal: Boolean = true
    ): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    initialize().getOrThrow()

                    val session =
                            terminalSession
                                    ?: return@withContext Result.failure(
                                            Exception("Terminal not initialized")
                                    )

                    if (showInTerminal) {
                        addToHistory("$ cd $repoPath")
                        addToHistory("$ $command")
                    }

                    Log.d(TAG, "📂 Executing: $command in $repoPath")

                    // Change to repo directory
                    session.insertText("cd '$repoPath'\n")
                    delay(200)

                    // Execute command with output redirection
                    val outputFile = "/tmp/git_output_${System.currentTimeMillis()}.txt"
                    val fullCommand = "$command > $outputFile 2>&1; echo $? >> $outputFile"
                    session.insertText("$fullCommand\n")

                    // Wait for command to complete
                    delay(COMMAND_TIMEOUT)

                    // Read output from Alpine filesystem
                    val alpineOutputPath = File(context.filesDir, "alpine$outputFile")
                    val output =
                            if (alpineOutputPath.exists()) {
                                val content = alpineOutputPath.readText()
                                alpineOutputPath.delete()
                                content
                            } else {
                                Log.w(
                                        TAG,
                                        "⚠️ Output file not found: ${alpineOutputPath.absolutePath}"
                                )
                                ""
                            }

                    // Parse exit code
                    val lines = output.lines()
                    val exitCode = lines.lastOrNull()?.toIntOrNull() ?: -1
                    val commandOutput = lines.dropLast(1).joinToString("\n")

                    if (showInTerminal) {
                        if (commandOutput.isNotBlank()) {
                            commandOutput.lines().forEach { line ->
                                if (line.isNotBlank()) addToHistory(line)
                            }
                        }

                        if (exitCode == 0) {
                            addToHistory("✅ Command completed successfully")
                        } else {
                            addToHistory("❌ Command failed with exit code: $exitCode")
                        }
                    }

                    Log.d(
                            TAG,
                            if (exitCode == 0) "✅ Command succeeded"
                            else "❌ Command failed: $exitCode"
                    )

                    if (exitCode == 0) {
                        Result.success(commandOutput)
                    } else {
                        Result.failure(
                                Exception("Command failed with exit code $exitCode: $commandOutput")
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Command execution failed", e)
                    if (showInTerminal) {
                        addToHistory("❌ Error: ${e.message}")
                    }
                    Result.failure(e)
                }
            }

    /** Get the terminal session for UI display */
    fun getTerminalSession(): TerminalSession? = terminalSession

    /** Get command history for display */
    fun getCommandHistory(): List<String> = commandHistory.toList()

    /** Add entry to command history */
    private fun addToHistory(entry: String) {
        commandHistory.add(entry)
        Log.d(TAG, "📝 $entry")
    }

    /** Clear command history */
    fun clearHistory() {
        commandHistory.clear()
        Log.d(TAG, "🗑️ Command history cleared")
    }

    /** Clean up terminal session */
    fun cleanup() {
        terminalSession?.destroy()
        terminalSession = null
        Log.d(TAG, "🧹 Terminal session cleaned up")
    }
}
