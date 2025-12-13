package com.bsbeetle.bananaspaghetti.directGit.utils

import android.content.Context

/**
 * Alpine Git Helper - Executes Git commands using Alpine Linux's native git binary.
 *
 * This now delegates to GitTerminalManager for all operations, providing a unified interface while
 * the terminal manager handles the actual command execution.
 */
class AlpineGitHelper(private val context: Context) {

    private val terminalManager = GitTerminalManager(context)

    /** Initialize the Git session (reuse if already exists) */
    suspend fun initialize(): Result<Unit> {
        return terminalManager.initialize()
    }

    /** Setup SSH keys in Alpine's ~/.ssh/ directory */
    suspend fun setupSshKeys(privateKey: String, publicKey: String): Result<Unit> {
        return terminalManager.setupSshKeys(privateKey, publicKey)
    }

    /** Execute a Git command in the Alpine environment */
    suspend fun executeGitCommand(repoPath: String, command: String): Result<String> {
        return terminalManager.executeGitCommand(repoPath, command, showInTerminal = true)
    }

    /**
     * Convert HTTPS remote URL to SSH format. HTTPS: https://github.com/username/repo.git SSH:
     * git@github.com:username/repo.git
     */
    private suspend fun ensureSshRemote(repoPath: String): Result<Unit> {
        // Get current remote URL
        val remoteResult =
                terminalManager.executeGitCommand(
                        repoPath,
                        "git remote get-url origin",
                        showInTerminal = false
                )
        val remoteUrl = remoteResult.getOrNull()?.trim() ?: return Result.success(Unit)

        // Check if it's HTTPS and convert to SSH
        val httpsPatterns =
                listOf(
                        Regex("""https://github\.com/(.+)/(.+?)(?:\.git)?/?$""") to
                                "git@github.com:",
                        Regex("""https://gitlab\.com/(.+)/(.+?)(?:\.git)?/?$""") to
                                "git@gitlab.com:",
                        Regex("""https://bitbucket\.org/(.+)/(.+?)(?:\.git)?/?$""") to
                                "git@bitbucket.org:"
                )

        for ((pattern, sshPrefix) in httpsPatterns) {
            val match = pattern.find(remoteUrl)
            if (match != null) {
                val username = match.groupValues[1]
                val repo = match.groupValues[2].removeSuffix(".git")
                val sshUrl = "$sshPrefix$username/$repo.git"

                android.util.Log.d(
                        "AlpineGitHelper",
                        "🔄 Converting HTTPS to SSH: $remoteUrl -> $sshUrl"
                )
                terminalManager.executeGitCommand(
                        repoPath,
                        "git remote set-url origin $sshUrl",
                        showInTerminal = true
                )
                return Result.success(Unit)
            }
        }

        return Result.success(Unit)
    }

    /** Push changes to remote */
    suspend fun push(repoPath: String, branch: String = "main"): Result<String> {
        // Ensure remote uses SSH URL
        ensureSshRemote(repoPath)

        return terminalManager.executeGitCommand(
                repoPath,
                "git push origin $branch",
                showInTerminal = true
        )
    }

    /** Pull changes from remote */
    suspend fun pull(repoPath: String, branch: String = "main"): Result<String> {
        // Ensure remote uses SSH URL
        ensureSshRemote(repoPath)

        return terminalManager.executeGitCommand(
                repoPath,
                "git pull origin $branch",
                showInTerminal = true
        )
    }

    /** Fetch from remote */
    suspend fun fetch(repoPath: String): Result<String> {
        // Ensure remote uses SSH URL
        ensureSshRemote(repoPath)

        return terminalManager.executeGitCommand(
                repoPath,
                "git fetch origin",
                showInTerminal = true
        )
    }

    /** Clone a repository */
    suspend fun clone(repoUrl: String, targetPath: String): Result<String> {
        val parentDir = java.io.File(targetPath).parent ?: "/"
        val dirName = java.io.File(targetPath).name
        return terminalManager.executeGitCommand(
                parentDir,
                "git clone $repoUrl $dirName",
                showInTerminal = true
        )
    }

    /** Get terminal session for UI display */
    fun getTerminalSession() = terminalManager.getTerminalSession()

    /** Get command history for UI display */
    fun getCommandHistory() = terminalManager.getCommandHistory()

    /** Clear command history */
    fun clearHistory() = terminalManager.clearHistory()

    /** Test SSH connection to a host (useful for debugging) */
    suspend fun testSshConnection(host: String = "github.com"): Result<String> {
        // Run a simple SSH test that should fail fast if there's an issue
        return terminalManager.executeGitCommand(
                "/home",
                "ssh -T -i /home/.ssh/id_ed25519 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o BatchMode=yes -o ConnectTimeout=10 git@$host",
                showInTerminal = true
        )
    }

    /** Clean up the Git session */
    fun cleanup() {
        terminalManager.cleanup()
    }
}
