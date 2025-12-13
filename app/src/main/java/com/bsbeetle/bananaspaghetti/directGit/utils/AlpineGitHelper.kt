package com.bsbeetle.bananaspaghetti.directGit.utils

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Alpine Git Helper - Executes Git commands using Alpine Linux's native git binary.
 *
 * This bypasses all Java SSH library limitations by using the real git/ssh binaries in the Alpine
 * environment.
 */
class AlpineGitHelper(private val context: Context) {

    companion object {
        private const val TAG = "AlpineGitHelper"
        private const val COMMAND_TIMEOUT = 30000L // 30 seconds
    }

    private var gitSession: TerminalSession? = null

    /** Initialize the Git session (reuse if already exists) */
    fun initializeGitSession() {
        Log.d(TAG, "🔧 Initializing Git session...")
        try {
            if (gitSession == null) {
                Log.d(TAG, "Creating new TerminalSession...")
                gitSession = TerminalSession(context)
                Log.d(TAG, "✅ TerminalSession created successfully")
            } else {
                Log.d(TAG, "♻️ Reusing existing TerminalSession")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Git session", e)
            throw e
        }
    }

    /** Setup SSH keys in Alpine's ~/.ssh/ directory */
    suspend fun setupSshKeys(privateKey: String, publicKey: String): Result<Unit> {
        Log.d(TAG, "🔑 Setting up SSH keys in Alpine...")
        Log.d(TAG, "Private key length: ${privateKey.length}")
        Log.d(TAG, "Public key length: ${publicKey.length}")

        return withContext(Dispatchers.IO) {
            try {
                initializeGitSession()

                val session =
                        gitSession
                                ?: run {
                                    Log.e(TAG, "❌ Git session is null after initialization")
                                    return@withContext Result.failure(
                                            Exception("Failed to initialize session")
                                    )
                                }

                // Create .ssh directory
                Log.d(TAG, "📁 Creating ~/.ssh directory...")
                session.insertText("mkdir -p ~/.ssh\n")
                Thread.sleep(500)

                // Write private key
                Log.d(TAG, "🔐 Writing private key to ~/.ssh/id_ed25519...")
                val alpinePrivateKeyPath = File(context.filesDir, "alpine/root/.ssh/id_ed25519")
                alpinePrivateKeyPath.parentFile?.mkdirs()
                alpinePrivateKeyPath.writeText(privateKey)
                session.insertText("chmod 600 ~/.ssh/id_ed25519\n")
                Thread.sleep(300)
                Log.d(TAG, "✅ Private key written and permissions set")

                // Write public key
                Log.d(TAG, "🔓 Writing public key to ~/.ssh/id_ed25519.pub...")
                val alpinePublicKeyPath = File(context.filesDir, "alpine/root/.ssh/id_ed25519.pub")
                alpinePublicKeyPath.writeText(publicKey)
                session.insertText("chmod 644 ~/.ssh/id_ed25519.pub\n")
                Thread.sleep(300)
                Log.d(TAG, "✅ Public key written and permissions set")

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
                Thread.sleep(300)
                Log.d(TAG, "✅ SSH config created")

                Log.d(TAG, "🎉 SSH keys setup complete!")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to setup SSH keys", e)
                Result.failure(e)
            }
        }
    }

    /** Execute a Git command in the Alpine environment */
    suspend fun executeGitCommand(repoPath: String, command: String): Result<String> {
        return try {
            initializeGitSession()

            // Change to repo directory and execute command
            val fullCommand = "cd '$repoPath' && $command"
            val output = executeCommand(fullCommand)

            Log.d(TAG, "Git command output: $output")
            Result.success(output)
        } catch (e: Exception) {
            Log.e(TAG, "Git command failed: $command", e)
            Result.failure(e)
        }
    }
    /** Execute a shell command and capture output via file redirection */
    private suspend fun executeCommand(command: String): String {
        val session = gitSession ?: throw IllegalStateException("Git session not initialized")

        // Use file redirection to capture output
        val outputFile = "/tmp/git_output_${System.currentTimeMillis()}.txt"
        val fullCommand = "$command > $outputFile 2>&1; echo \$? >> $outputFile"

        session.insertText("$fullCommand\n")

        // Wait for command to complete
        Thread.sleep(2000)

        // Read the output file from Alpine's filesystem
        val alpineOutputPath = File(context.filesDir, "alpine$outputFile")
        return if (alpineOutputPath.exists()) {
            val output = alpineOutputPath.readText()
            alpineOutputPath.delete()
            output
        } else {
            throw Exception("Command output file not found")
        }
    }

    /** Push changes to remote */
    suspend fun push(repoPath: String, branch: String = "main"): Result<String> {
        return executeGitCommand(repoPath, "git push origin $branch")
    }

    /** Pull changes from remote */
    suspend fun pull(repoPath: String, branch: String = "main"): Result<String> {
        return executeGitCommand(repoPath, "git pull origin $branch")
    }

    /** Fetch from remote */
    suspend fun fetch(repoPath: String): Result<String> {
        return executeGitCommand(repoPath, "git fetch origin")
    }

    /** Clone a repository */
    suspend fun clone(repoUrl: String, targetPath: String): Result<String> {
        initializeGitSession()
        return executeGitCommand(
                File(targetPath).parent ?: "/",
                "git clone $repoUrl ${File(targetPath).name}"
        )
    }

    /** Clean up the Git session */
    fun cleanup() {
        gitSession?.destroy()
        gitSession = null
    }
}
