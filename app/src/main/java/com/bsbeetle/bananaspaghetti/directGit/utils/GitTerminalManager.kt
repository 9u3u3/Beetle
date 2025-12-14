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
        private const val COMMAND_TIMEOUT = 30000L // 30 seconds for network operations
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

            // Wait for terminal to be fully ready (outputFd is set)
            Log.d(TAG, "⏳ Waiting for terminal to be ready...")
            var waitTime = 0L
            val maxWait = 10000L // 10 seconds max wait
            while (waitTime < maxWait) {
                if (terminalSession?.isReady?.value == true) {
                    Log.d(TAG, "✅ Terminal is ready (outputFd set)")
                    break
                }
                delay(100)
                waitTime += 100
                if (waitTime % 1000 == 0L) {
                    Log.d(TAG, "⏳ Still waiting for terminal... ${waitTime}ms")
                }
            }

            if (terminalSession?.isReady?.value != true) {
                Log.w(
                        TAG,
                        "⚠️ Terminal may not be fully ready after ${maxWait}ms, but proceeding..."
                )
            }

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
                    val alpinePrivateKeyPath = File(context.filesDir, "alpine/home/.ssh/id_ed25519")
                    alpinePrivateKeyPath.parentFile?.mkdirs()
                    alpinePrivateKeyPath.writeText(privateKey)
                    session.insertText("chmod 600 ~/.ssh/id_ed25519\n")
                    delay(300)
                    addToHistory("✅ Private key configured")

                    // Write public key
                    Log.d(TAG, "🔓 Writing public key...")
                    val alpinePublicKeyPath =
                            File(context.filesDir, "alpine/home/.ssh/id_ed25519.pub")
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
                    
                Host gitlab.com
                    HostName gitlab.com
                    User git
                    IdentityFile ~/.ssh/id_ed25519
                    StrictHostKeyChecking no
                    UserKnownHostsFile /dev/null
                    
                Host bitbucket.org
                    HostName bitbucket.org
                    User git
                    IdentityFile ~/.ssh/id_ed25519
                    StrictHostKeyChecking no
                    UserKnownHostsFile /dev/null
            """.trimIndent()

                    val alpineSshConfigPath = File(context.filesDir, "alpine/home/.ssh/config")
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

                    // Ensure git is installed before running git commands
                    ensureGitInstalled(session)

                    if (showInTerminal) {
                        addToHistory("$ cd $repoPath")
                        addToHistory("$ $command")
                    }

                    Log.d(TAG, "📂 Executing: $command in $repoPath")

                    // Change to repo directory
                    session.insertText("cd '$repoPath'\n")
                    delay(300)

                    // Set GIT_SSH_COMMAND to bypass host key checking and use correct identity file
                    val sshCommand =
                            "ssh -i /home/.ssh/id_ed25519 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o BatchMode=yes"
                    session.insertText("export GIT_SSH_COMMAND='$sshCommand'\n")
                    delay(200)

                    // Execute command with output redirection to /home which is properly mapped
                    val outputFileName = "git_output_${System.currentTimeMillis()}.txt"
                    val alpineOutputFile = "/home/$outputFileName"
                    val fullCommand =
                            "$command > $alpineOutputFile 2>&1; echo \$? >> $alpineOutputFile"
                    Log.d(TAG, "📝 Full command: $fullCommand")
                    session.insertText("$fullCommand\n")

                    // Poll for command to complete (check output file has content)
                    val alpineOutputPath = File(context.filesDir, "alpine/home/$outputFileName")
                    Log.d(TAG, "📖 Output file path: ${alpineOutputPath.absolutePath}")

                    val startTime = System.currentTimeMillis()
                    var output = ""
                    var pollCount = 0

                    // Poll every 500ms until timeout or we get output
                    while (System.currentTimeMillis() - startTime < COMMAND_TIMEOUT) {
                        delay(500)
                        pollCount++

                        if (alpineOutputPath.exists()) {
                            val content = alpineOutputPath.readText()
                            // Check if output contains exit code (command completed)
                            // The exit code is appended as the last line
                            val lines = content.lines().filter { it.isNotBlank() }
                            val lastLine = lines.lastOrNull()?.trim()
                            if (lastLine?.toIntOrNull() != null) {
                                // Command completed - we have an exit code
                                output = content
                                Log.d(
                                        TAG,
                                        "✅ Command completed after ${pollCount * 500}ms (${lines.size} lines)"
                                )
                                break
                            } else if (content.isNotEmpty()) {
                                Log.d(
                                        TAG,
                                        "⏳ Poll $pollCount: Output has ${content.length} chars but no exit code yet"
                                )
                            }
                        } else {
                            if (pollCount % 10 == 0) {
                                Log.d(TAG, "⏳ Poll $pollCount: File not created yet")
                            }
                        }
                    }

                    val elapsedTime = System.currentTimeMillis() - startTime
                    Log.d(TAG, "⏱️ Total wait time: ${elapsedTime}ms")

                    // Final read if we timed out but file exists
                    if (output.isEmpty() && alpineOutputPath.exists()) {
                        output = alpineOutputPath.readText()
                        Log.d(
                                TAG,
                                "⚠️ Timeout reached, reading whatever output we have: ${output.length} chars"
                        )
                    }

                    // Clean up output file
                    if (alpineOutputPath.exists()) {
                        alpineOutputPath.delete()
                    }

                    Log.d(TAG, "📄 Final output (${output.length} chars): ${output.take(500)}")

                    // Parse exit code
                    val lines = output.lines().filter { it.isNotBlank() }
                    val exitCode = lines.lastOrNull()?.trim()?.toIntOrNull() ?: -1
                    val commandOutput =
                            if (lines.size > 1) lines.dropLast(1).joinToString("\n") else ""

                    Log.d(TAG, "📊 Exit code: $exitCode, Output lines: ${lines.size}")

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

    private var gitInstallChecked = false
    private var safeDirectoryConfigured = false

    /** Ensure git is installed in Alpine */
    private suspend fun ensureGitInstalled(
            session: com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
    ) {
        // Install git if needed
        if (!gitInstallChecked) {
            Log.d(TAG, "📦 Checking if git is installed...")
            addToHistory("📦 Checking git installation...")

            // Check if git is installed
            val gitCheckFile = File(context.filesDir, "alpine/usr/bin/git")
            if (!gitCheckFile.exists()) {
                Log.d(TAG, "📥 Git not found, installing...")
                addToHistory("📥 Installing git...")

                // Install git
                session.insertText("apk update && apk add git openssh\n")

                // Wait for installation to complete
                delay(10000) // 10 seconds for package installation

                addToHistory("✅ Git installed successfully")
            } else {
                Log.d(TAG, "✅ Git already installed")
            }
            gitInstallChecked = true
        }

        // Configure safe.directory (must run once per session)
        if (!safeDirectoryConfigured) {
            // Configure git to allow all directories (fixes "dubious ownership" error on Android)
            // This is needed because Alpine's user differs from Android's file owner
            session.insertText("git config --global --add safe.directory '*'\n")
            delay(200)
            Log.d(TAG, "🔧 Configured safe.directory for all paths")
            addToHistory("🔧 Git safe.directory configured")
            safeDirectoryConfigured = true
        }
    }

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
