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

    /** Push changes to remote */
    suspend fun push(repoPath: String, branch: String = "main"): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git push origin $branch",
                showInTerminal = true
        )
    }

    /** Pull changes from remote */
    suspend fun pull(repoPath: String, branch: String = "main"): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git pull origin $branch",
                showInTerminal = true
        )
    }

    /** Fetch from remote */
    suspend fun fetch(repoPath: String): Result<String> {
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

    /** Clean up the Git session */
    fun cleanup() {
        terminalManager.cleanup()
    }
}
