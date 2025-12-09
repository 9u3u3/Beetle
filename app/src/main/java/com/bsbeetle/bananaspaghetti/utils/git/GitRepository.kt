package com.bsbeetle.bananaspaghetti.utils.git

import android.util.Log
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.errors.*
import org.eclipse.jgit.errors.RepositoryNotFoundException
import java.io.File

// Result wrapper for Git operations
sealed class GitResult<out T> {
    data class Success<T>(val data: T, val message: String) : GitResult<T>()
    data class Error(val message: String, val details: String? = null) : GitResult<Nothing>()
}

class GitRepository(private val rootDir: File) {

    private var git: Git? = null

    init {
        try {
            val gitDir = File(rootDir, ".git")
            if (gitDir.exists()) {
                git = Git.open(rootDir)
                GitLogger.log("Opened repository at ${rootDir.absolutePath}", LogType.INFO)
            } else {
                GitLogger.log("No git repository found at ${rootDir.absolutePath}", LogType.INFO)
            }
        } catch (e: RepositoryNotFoundException) {
            Log.w("GitRepository", "No repository found at ${rootDir.absolutePath}")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to open git repo", e)
            GitLogger.log("Failed to open repository: ${e.message}", LogType.ERROR)
        }
    }

    fun isInitialized(): Boolean = git != null

    fun init(): GitResult<Unit> {
        GitLogger.log("git init ${rootDir.absolutePath}", LogType.COMMAND)
        return try {
            git = Git.init().setDirectory(rootDir).call()
            GitLogger.log("Repository initialized", LogType.SUCCESS)
            GitResult.Success(Unit, "Repository initialized at ${rootDir.name}")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to init repo", e)
            GitLogger.log("Init failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to initialize repository", e.message)
        }
    }

    fun getStatus(): Status? {
        return try {
            git?.status()?.call()
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to get status", e)
            null
        }
    }

    fun resetMixed(): GitResult<Unit> {
        GitLogger.log("git reset --mixed", LogType.COMMAND)
        return try {
            git?.reset()?.setMode(org.eclipse.jgit.api.ResetCommand.ResetType.MIXED)?.call()
            GitLogger.log("Reset mixed (unstaged all)", LogType.SUCCESS)
            GitResult.Success(Unit, "Unstaged all files")
        } catch (e: Exception) {
            GitLogger.log("Reset failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to reset", e.message)
        }
    }

    fun stageFile(filePath: String): GitResult<Unit> {
        val relativePath = toRelativePath(filePath)
        GitLogger.log("git add $relativePath", LogType.COMMAND)
        return try {
            git?.add()?.addFilepattern(relativePath)?.call()
            GitLogger.log("Staged $relativePath", LogType.SUCCESS)
            GitResult.Success(Unit, "Staged: $relativePath")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to stage file: $filePath", e)
            GitLogger.log("Stage failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to stage file", e.message)
        }
    }

    fun stageFiles(paths: List<String>): GitResult<Unit> {
        return try {
            GitLogger.log("git add ${paths.joinToString(", ")}", LogType.COMMAND)
            val cmd = git?.add()
            paths.forEach { cmd?.addFilepattern(toRelativePath(it)) }
            cmd?.call()
            GitLogger.log("Staged ${paths.size} files", LogType.SUCCESS)
            GitResult.Success(Unit, "Staged ${paths.size} files")
        } catch (e: Exception) {
            GitLogger.log("Failed to stage files: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to stage files", e.message)
        }
    }

    fun stageAll(): GitResult<Unit> {
        GitLogger.log("git add .", LogType.COMMAND)
        return try {
            git?.add()?.addFilepattern(".")?.call()
            GitLogger.log("Staged all changes", LogType.SUCCESS)
            GitResult.Success(Unit, "Staged all changes")
        } catch (e: Exception) {
            GitLogger.log("Stage all failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to stage all files", e.message)
        }
    }

    fun unstageFile(filePath: String): GitResult<Unit> {
        val relativePath = toRelativePath(filePath)
        GitLogger.log("git reset $relativePath", LogType.COMMAND)
        return try {
            git?.reset()?.addPath(relativePath)?.call()
            GitLogger.log("Unstaged $relativePath", LogType.SUCCESS)
            GitResult.Success(Unit, "Unstaged: $relativePath")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to unstage file: $filePath", e)
            GitLogger.log("Unstage failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to unstage file", e.message)
        }
    }

    fun commit(message: String): GitResult<String> {
        GitLogger.log("git commit -m \"$message\"", LogType.COMMAND)
        if (message.isBlank()) {
            return GitResult.Error("Commit message cannot be empty")
        }
        return try {
            val result = git?.commit()?.setMessage(message)?.call()
            val shortHash = result?.abbreviate(7)?.name() ?: "unknown"
            GitLogger.log("Committed $shortHash", LogType.SUCCESS)
            GitResult.Success(shortHash, "Committed: $shortHash - ${message.take(30)}...")
        } catch (e: EmptyCommitException) {
            GitLogger.log("Commit failed: Nothing to commit", LogType.ERROR)
            GitResult.Error("Nothing to commit", "No changes are staged")
        } catch (e: WrongRepositoryStateException) {
            GitLogger.log("Commit failed: Wrong state", LogType.ERROR)
            GitResult.Error("Repository in wrong state", "Resolve conflicts or complete previous operation first")
        } catch (e: ConcurrentRefUpdateException) {
            GitLogger.log("Commit failed: Concurrent update", LogType.ERROR)
            GitResult.Error("Concurrent update detected", "Try again")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to commit", e)
            GitLogger.log("Commit failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Commit failed", e.message)
        }
    }

    fun close() {
        git?.close()
    }

    // === Configuration ===

    fun getConfig(): Pair<String, String> {
        val config = git?.repository?.config
        val name = config?.getString("user", null, "name") ?: ""
        val email = config?.getString("user", null, "email") ?: ""
        return Pair(name, email)
    }

    fun setConfig(name: String, email: String): GitResult<Unit> {
        GitLogger.log("git config user.name \"$name\"", LogType.COMMAND)
        GitLogger.log("git config user.email \"$email\"", LogType.COMMAND)
        return try {
            val config = git?.repository?.config
            config?.setString("user", null, "name", name)
            config?.setString("user", null, "email", email)
            config?.save()
            GitLogger.log("Identity saved", LogType.SUCCESS)
            GitResult.Success(Unit, "Identity saved: $name <$email>")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to set config", e)
            GitLogger.log("Failed to save identity: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to save identity", e.message)
        }
    }

    // === Remote ===

    fun setRemote(url: String): GitResult<Unit> {
        GitLogger.log("git remote set-url origin $url", LogType.COMMAND)
        return try {
            val config = git?.repository?.config
            config?.setString("remote", "origin", "url", url)
            config?.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
            config?.save()
            GitLogger.log("Remote set to $url", LogType.SUCCESS)
            GitResult.Success(Unit, "Remote set: origin → $url")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to set remote", e)
            GitLogger.log("Failed to set remote: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to set remote URL", e.message)
        }
    }

    fun getRemote(): String {
        return git?.repository?.config?.getString("remote", "origin", "url") ?: ""
    }

    // === Network Operations ===

    fun push(username: String, token: String): GitResult<Unit> {
        val remote = getRemote()
        if (remote.isEmpty()) {
            return GitResult.Error("No remote configured", "Set a remote URL first in settings")
        }
        GitLogger.log("git push origin", LogType.COMMAND)
        return try {
            val credentialsProvider = org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(username, token)
            val result = git?.push()?.setCredentialsProvider(credentialsProvider)?.call()
            val updates = result?.flatMap { it.remoteUpdates } ?: emptyList()
            if (updates.isEmpty()) {
                GitLogger.log("Everything up-to-date", LogType.SUCCESS)
                GitResult.Success(Unit, "Everything up-to-date")
            } else {
                GitLogger.log("Pushed ${updates.size} refs", LogType.SUCCESS)
                GitResult.Success(Unit, "Pushed ${updates.size} ref(s) to origin")
            }
        } catch (e: TransportException) {
            Log.e("GitRepository", "Push transport error", e)
            GitLogger.log("Push failed: ${e.message}", LogType.ERROR)
            when {
                e.message?.contains("not authorized") == true || e.message?.contains("Authentication") == true ->
                    GitResult.Error("Authentication failed", "Check your username and token")
                e.message?.contains("not found") == true ->
                    GitResult.Error("Repository not found", "Check your remote URL")
                e.message?.contains("rejected") == true ->
                    GitResult.Error("Push rejected", "Pull changes first or force push")
                else -> GitResult.Error("Push failed", e.message)
            }
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to push", e)
            GitLogger.log("Push failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Push failed", e.message)
        }
    }

    fun pull(username: String, token: String): GitResult<Unit> {
        val remote = getRemote()
        if (remote.isEmpty()) {
            return GitResult.Error("No remote configured", "Set a remote URL first in settings")
        }
        GitLogger.log("git pull origin", LogType.COMMAND)
        return try {
            val credentialsProvider = org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(username, token)
            val result = git?.pull()?.setCredentialsProvider(credentialsProvider)?.call()
            when {
                result?.mergeResult?.mergeStatus?.isSuccessful == true -> {
                    GitLogger.log("Pull successful", LogType.SUCCESS)
                    GitResult.Success(Unit, "Pull complete: ${result.fetchResult?.messages ?: "Updated"}")
                }
                result?.isSuccessful == true -> {
                    GitLogger.log("Already up-to-date", LogType.SUCCESS)
                    GitResult.Success(Unit, "Already up-to-date")
                }
                else -> {
                    GitLogger.log("Pull failed: ${result?.mergeResult?.mergeStatus}", LogType.ERROR)
                    GitResult.Error("Pull failed", result?.mergeResult?.mergeStatus?.toString())
                }
            }
        } catch (e: TransportException) {
            Log.e("GitRepository", "Pull transport error", e)
            GitLogger.log("Pull failed: ${e.message}", LogType.ERROR)
            when {
                e.message?.contains("not authorized") == true || e.message?.contains("Authentication") == true ->
                    GitResult.Error("Authentication failed", "Check your username and token")
                e.message?.contains("not found") == true ->
                    GitResult.Error("Repository not found", "Check your remote URL")
                else -> GitResult.Error("Pull failed", e.message)
            }
        } catch (e: CheckoutConflictException) {
            GitLogger.log("Pull conflict", LogType.ERROR)
            GitResult.Error("Checkout conflict", "Commit or stash your changes before pulling")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to pull", e)
            GitLogger.log("Pull failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Pull failed", e.message)
        }
    }

    fun fetch(username: String, token: String): GitResult<Unit> {
        val remote = getRemote()
        if (remote.isEmpty()) {
            return GitResult.Error("No remote configured", "Set a remote URL first in settings")
        }
        GitLogger.log("git fetch origin", LogType.COMMAND)
        return try {
            val credentialsProvider = org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(username, token)
            val result = git?.fetch()?.setCredentialsProvider(credentialsProvider)?.call()
            val updates = result?.trackingRefUpdates?.size ?: 0
            if (updates > 0) {
                GitLogger.log("Fetched $updates updates", LogType.SUCCESS)
                GitResult.Success(Unit, "Fetched $updates update(s)")
            } else {
                GitLogger.log("Already up-to-date", LogType.SUCCESS)
                GitResult.Success(Unit, "Already up-to-date")
            }
        } catch (e: TransportException) {
            GitLogger.log("Fetch failed: ${e.message}", LogType.ERROR)
            when {
                e.message?.contains("not authorized") == true || e.message?.contains("Authentication") == true ->
                    GitResult.Error("Authentication failed", "Check your username and token")
                else -> GitResult.Error("Fetch failed", e.message)
            }
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to fetch", e)
            GitLogger.log("Fetch failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Fetch failed", e.message)
        }
    }

    // === Diff ===

    fun getDiff(filePath: String): String {
        return try {
            val out = java.io.ByteArrayOutputStream()
            val df = org.eclipse.jgit.diff.DiffFormatter(out)
            df.setRepository(git?.repository)
            
            val relativePath = toRelativePath(filePath)
            val diffs = git?.diff()?.setPathFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(relativePath))?.call()
            
            diffs?.forEach { entry ->
                df.format(entry)
            }
            
            val result = out.toString()
            if (result.isBlank()) "No changes detected" else result
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to get diff", e)
            "Error loading diff: ${e.message}"
        }
    }

    // === Branch Operations ===

    fun getCurrentBranch(): String {
        return try {
            git?.repository?.branch ?: "HEAD"
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to get current branch", e)
            "unknown"
        }
    }

    fun getBranches(): List<BranchInfo> {
        val branches = mutableListOf<BranchInfo>()
        try {
            val currentBranch = getCurrentBranch()
            
            git?.branchList()?.call()?.forEach { ref ->
                val name = ref.name.removePrefix("refs/heads/")
                branches.add(BranchInfo(name, isRemote = false, isCurrent = name == currentBranch))
            }
            
            git?.branchList()?.setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE)?.call()?.forEach { ref ->
                val name = ref.name.removePrefix("refs/remotes/")
                branches.add(BranchInfo(name, isRemote = true, isCurrent = false))
            }
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to get branches", e)
        }
        return branches
    }

    fun createBranch(name: String): GitResult<Unit> {
        GitLogger.log("git branch $name", LogType.COMMAND)
        if (name.isBlank()) {
            return GitResult.Error("Branch name cannot be empty")
        }
        if (name.contains(" ") || name.contains("..")) {
            return GitResult.Error("Invalid branch name", "No spaces or '..' allowed")
        }
        return try {
            git?.branchCreate()?.setName(name)?.call()
            GitLogger.log("Branch created", LogType.SUCCESS)
            GitResult.Success(Unit, "Branch '$name' created")
        } catch (e: RefAlreadyExistsException) {
            GitLogger.log("Branch already exists", LogType.ERROR)
            GitResult.Error("Branch already exists", "Choose a different name")
        } catch (e: InvalidRefNameException) {
            GitLogger.log("Invalid branch name", LogType.ERROR)
            GitResult.Error("Invalid branch name", e.message)
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to create branch: $name", e)
            GitLogger.log("Create branch failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to create branch", e.message)
        }
    }

    fun checkoutBranch(name: String): GitResult<Unit> {
        GitLogger.log("git checkout $name", LogType.COMMAND)
        return try {
            git?.checkout()?.setName(name)?.call()
            GitLogger.log("Checked out $name", LogType.SUCCESS)
            GitResult.Success(Unit, "Switched to '$name'")
        } catch (e: CheckoutConflictException) {
            GitLogger.log("Checkout conflict", LogType.ERROR)
            GitResult.Error("Checkout conflict", "Commit or stash changes before switching: ${e.conflictingPaths.take(3).joinToString()}")
        } catch (e: RefNotFoundException) {
            GitLogger.log("Branch not found", LogType.ERROR)
            GitResult.Error("Branch not found", "Branch '$name' doesn't exist")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to checkout branch: $name", e)
            GitLogger.log("Checkout failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to switch branch", e.message)
        }
    }

    fun deleteBranch(name: String): GitResult<Unit> {
        GitLogger.log("git branch -D $name", LogType.COMMAND)
        if (name == getCurrentBranch()) {
            return GitResult.Error("Cannot delete current branch", "Switch to a different branch first")
        }
        return try {
            git?.branchDelete()?.setBranchNames(name)?.setForce(true)?.call()
            GitLogger.log("Branch deleted", LogType.SUCCESS)
            GitResult.Success(Unit, "Deleted branch '$name'")
        } catch (e: CannotDeleteCurrentBranchException) {
            GitLogger.log("Cannot delete current branch", LogType.ERROR)
            GitResult.Error("Cannot delete current branch", "Switch to a different branch first")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to delete branch: $name", e)
            GitLogger.log("Delete branch failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to delete branch", e.message)
        }
    }

    // === Commit History ===

    fun getCommitLog(limit: Int = 50): List<CommitInfo> {
        val commits = mutableListOf<CommitInfo>()
        try {
            git?.log()?.setMaxCount(limit)?.call()?.forEach { revCommit ->
                commits.add(CommitInfo(
                    hash = revCommit.name,
                    shortHash = revCommit.abbreviate(7).name(),
                    message = revCommit.shortMessage,
                    fullMessage = revCommit.fullMessage,
                    author = revCommit.authorIdent.name,
                    email = revCommit.authorIdent.emailAddress,
                    date = java.util.Date(revCommit.commitTime * 1000L)
                ))
            }
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to get commit log", e)
        }
        return commits
    }

    // === Stash ===

    fun stash(): GitResult<Unit> {
        GitLogger.log("git stash", LogType.COMMAND)
        return try {
            val result = git?.stashCreate()?.call()
            if (result != null) {
                GitLogger.log("Stashed changes", LogType.SUCCESS)
                GitResult.Success(Unit, "Changes stashed")
            } else {
                GitLogger.log("Nothing to stash", LogType.INFO)
                GitResult.Error("Nothing to stash", "No local changes to save")
            }
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to stash", e)
            GitLogger.log("Stash failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to stash", e.message)
        }
    }

    fun stashPop(): GitResult<Unit> {
        GitLogger.log("git stash pop", LogType.COMMAND)
        return try {
            val stashList = git?.stashList()?.call()
            if (stashList.isNullOrEmpty()) {
                return GitResult.Error("No stash entries", "Nothing to pop")
            }
            git?.stashApply()?.call()
            git?.stashDrop()?.call()
            GitLogger.log("Stash popped", LogType.SUCCESS)
            GitResult.Success(Unit, "Stash applied and removed")
        } catch (e: StashApplyFailureException) {
            GitLogger.log("Stash apply failed: Conflict", LogType.ERROR)
            GitResult.Error("Stash apply failed", "Conflicts detected - resolve manually")
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to pop stash", e)
            GitLogger.log("Stash pop failed: ${e.message}", LogType.ERROR)
            GitResult.Error("Failed to pop stash", e.message)
        }
    }

    fun listStashes(): List<StashInfo> {
        val stashes = mutableListOf<StashInfo>()
        try {
            git?.stashList()?.call()?.forEachIndexed { index, revCommit ->
                stashes.add(StashInfo(
                    index = index,
                    message = revCommit.shortMessage,
                    date = java.util.Date(revCommit.commitTime * 1000L)
                ))
            }
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to list stashes", e)
        }
        return stashes
    }

    // === Ahead/Behind ===

    fun getAheadBehind(): Pair<Int, Int> {
        return try {
            val repo = git?.repository ?: return Pair(0, 0)
            val branch = repo.branch ?: return Pair(0, 0)
            val trackingStatus = org.eclipse.jgit.lib.BranchTrackingStatus.of(repo, branch)
            Pair(trackingStatus?.aheadCount ?: 0, trackingStatus?.behindCount ?: 0)
        } catch (e: Exception) {
            Log.e("GitRepository", "Failed to get ahead/behind", e)
            Pair(0, 0)
        }
    }

    // === Helpers ===

    private fun toRelativePath(filePath: String): String {
        return if (filePath.startsWith(rootDir.absolutePath)) {
            filePath.substring(rootDir.absolutePath.length + 1)
        } else {
            filePath
        }
    }

    companion object {
        fun clone(url: String, directory: File, username: String, token: String): GitResult<GitRepository> {
            GitLogger.log("git clone $url ${directory.name}", LogType.COMMAND)
            return try {
                val credentialsProvider = org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(username, token)
                Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(directory)
                    .setCredentialsProvider(credentialsProvider)
                    .call()
                GitLogger.log("Cloned successfully", LogType.SUCCESS)
                GitResult.Success(GitRepository(directory), "Cloned to ${directory.name}")
            } catch (e: TransportException) {
                GitLogger.log("Clone failed: ${e.message}", LogType.ERROR)
                when {
                    e.message?.contains("not authorized") == true ->
                        GitResult.Error("Authentication failed", "Check your username and token")
                    e.message?.contains("not found") == true ->
                        GitResult.Error("Repository not found", "Check the URL")
                    else -> GitResult.Error("Clone failed", e.message)
                }
            } catch (e: Exception) {
                Log.e("GitRepository", "Failed to clone", e)
                GitLogger.log("Clone failed: ${e.message}", LogType.ERROR)
                GitResult.Error("Clone failed", e.message)
            }
        }
    }
}

// Data classes
data class BranchInfo(
    val name: String,
    val isRemote: Boolean,
    val isCurrent: Boolean
)

data class CommitInfo(
    val hash: String,
    val shortHash: String,
    val message: String,
    val fullMessage: String,
    val author: String,
    val email: String,
    val date: java.util.Date
)

data class StashInfo(
    val index: Int,
    val message: String,
    val date: java.util.Date
)
