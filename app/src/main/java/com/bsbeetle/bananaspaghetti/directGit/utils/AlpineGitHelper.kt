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
        return terminalManager.executeGitCommand(
                "/home",
                "ssh -T -i /home/.ssh/id_ed25519 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o BatchMode=yes -o ConnectTimeout=10 git@$host",
                showInTerminal = true
        )
    }

    // ============== Branch Management ==============

    /** Create a new branch from a source branch */
    suspend fun createBranchFrom(
            repoPath: String,
            newBranch: String,
            sourceBranch: String
    ): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git checkout -b '$newBranch' '$sourceBranch'",
                showInTerminal = true
        )
    }

    /** Delete a local branch */
    suspend fun deleteBranchLocal(
            repoPath: String,
            branchName: String,
            force: Boolean = false
    ): Result<String> {
        val flag = if (force) "-D" else "-d"
        return terminalManager.executeGitCommand(
                repoPath,
                "git branch $flag '$branchName'",
                showInTerminal = true
        )
    }

    /** Delete a remote branch */
    suspend fun deleteBranchRemote(repoPath: String, branchName: String): Result<String> {
        ensureSshRemote(repoPath)
        return terminalManager.executeGitCommand(
                repoPath,
                "git push origin --delete '$branchName'",
                showInTerminal = true
        )
    }

    /** Rename a branch */
    suspend fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git branch -m '$oldName' '$newName'",
                showInTerminal = true
        )
    }

    /** Merge a branch into current branch */
    suspend fun merge(repoPath: String, branchToMerge: String): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git merge '$branchToMerge'",
                showInTerminal = true
        )
    }

    /** Rebase current branch onto another branch */
    suspend fun rebase(repoPath: String, ontoBranch: String): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git rebase '$ontoBranch'",
                showInTerminal = true
        )
    }

    /** Abort a merge in progress */
    suspend fun mergeAbort(repoPath: String): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git merge --abort",
                showInTerminal = true
        )
    }

    /** Abort a rebase in progress */
    suspend fun rebaseAbort(repoPath: String): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git rebase --abort",
                showInTerminal = true
        )
    }

    // ============== Stash Operations ==============

    /** Stash current changes */
    suspend fun stash(repoPath: String, message: String? = null): Result<String> {
        val cmd = if (message != null) "git stash push -m '$message'" else "git stash"
        return terminalManager.executeGitCommand(repoPath, cmd, showInTerminal = true)
    }

    /** Pop the latest stash */
    suspend fun stashPop(repoPath: String): Result<String> {
        return terminalManager.executeGitCommand(repoPath, "git stash pop", showInTerminal = true)
    }

    /** Apply the latest stash (keep in stash list) */
    suspend fun stashApply(repoPath: String, stashIndex: Int = 0): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git stash apply stash@{$stashIndex}",
                showInTerminal = true
        )
    }

    /** Drop a stash */
    suspend fun stashDrop(repoPath: String, stashIndex: Int = 0): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git stash drop stash@{$stashIndex}",
                showInTerminal = true
        )
    }

    /** List all stashes */
    suspend fun stashList(repoPath: String): Result<String> {
        return terminalManager.executeGitCommand(repoPath, "git stash list", showInTerminal = false)
    }

    /** Get stash count */
    suspend fun getStashCount(repoPath: String): Int {
        val result =
                terminalManager.executeGitCommand(
                        repoPath,
                        "git stash list",
                        showInTerminal = false
                )
        return result.getOrNull()?.lines()?.filter { it.isNotBlank() }?.size ?: 0
    }

    // ============== Other Useful Commands ==============

    /** Discard changes in a file */
    suspend fun discardChanges(repoPath: String, filePath: String): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git checkout -- '$filePath'",
                showInTerminal = true
        )
    }

    /** Reset file to HEAD (unstage) */
    suspend fun unstageFile(repoPath: String, filePath: String): Result<String> {
        return terminalManager.executeGitCommand(
                repoPath,
                "git reset HEAD '$filePath'",
                showInTerminal = true
        )
    }

    /** Get list of remote branches */
    suspend fun getRemoteBranches(repoPath: String): List<String> {
        val result =
                terminalManager.executeGitCommand(repoPath, "git branch -r", showInTerminal = false)
        return result.getOrNull()
                ?.lines()
                ?.filter { it.isNotBlank() && !it.contains("HEAD") }
                ?.map { it.trim().removePrefix("origin/") }
                ?: emptyList()
    }

    /** Clean up the Git session */
    fun cleanup() {
        terminalManager.cleanup()
    }
}
