package com.bsbeetle.bananaspaghetti.directGit.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.directGit.utils.AlpineGitHelper
import com.bsbeetle.bananaspaghetti.directGit.utils.AlpineSshKeyManager
import com.bsbeetle.bananaspaghetti.directGit.utils.GitSshKeyManager
import com.bsbeetle.bananaspaghetti.directGit.utils.GitSshSessionFactory
import com.bsbeetle.bananaspaghetti.directGit.utils.GitUrlConverter
import com.bsbeetle.bananaspaghetti.utils.git.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.transport.SshSessionFactory

data class GitUiState(
        val isInitialized: Boolean = false,
        val modifiedFiles: List<String> = emptyList(),
        val stagedFiles: List<String> = emptyList(),
        val untrackedFiles: List<String> = emptyList(),
        val conflictedFiles: List<String> = emptyList(), // Files with merge conflicts
        val isLoading: Boolean = false,
        val userName: String = "",
        val userEmail: String = "",
        val remoteUrl: String = "",
        val lastError: String? = null,
        val lastSuccess: String? = null,
        val currentDiff: String? = null,
        // Branch
        val currentBranch: String = "",
        val branches: List<BranchInfo> = emptyList(),
        // History
        val commits: List<CommitInfo> = emptyList(),
        // Stash
        val stashes: List<StashInfo> = emptyList(),
        // Sync status
        val aheadCount: Int = 0,
        val behindCount: Int = 0,
        // Operation status
        val operationInProgress: String? = null,
        // Logs
        val logs: List<LogEntry> = emptyList(),
        // SSH status
        val sshKeysConfigured: Boolean = false,
        // Terminal history
        val terminalHistory: List<String> = emptyList(),
        // Merge editor state
        val showMergeEditor: Boolean = false,
        val mergeEditorFile: String? = null,
        val mergeEditorContent: String? = null
)

class GitViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()

    private var repository: GitRepository? = null
    private var currentRoot: File? = null

    // SSH key manager for JGit (RSA 4096 - JSch limitation)
    private val sshKeyManager = GitSshKeyManager(application)
    private val sshSessionFactory = GitSshSessionFactory(sshKeyManager)

    // Alpine SSH key manager for ED25519 keys (used for push/pull via terminal)
    private val alpineSshKeyManager = AlpineSshKeyManager(application)

    // Alpine Git Helper for terminal-based Git operations
    private val alpineGitHelper = AlpineGitHelper(application)

    private var isGitInitialized = false

    init {
        // Empty init - initialization happens lazily via initializeGit()
    }

    fun initializeGit() {
        if (isGitInitialized) return
        isGitInitialized = true

        viewModelScope.launch(Dispatchers.IO) {
            Log.d("GitViewModel", "🚀 Initializing GitViewModel lazily...")

            // Configure SSH for JGit operations (local commits, etc.)
            SshSessionFactory.setInstance(sshSessionFactory)

            // Check if Alpine SSH keys are configured (ED25519)
            val hasAlpineKeys = alpineSshKeyManager.hasKeys()
            Log.d("GitViewModel", "Alpine ED25519 keys exist: $hasAlpineKeys")
            _uiState.value = _uiState.value.copy(sshKeysConfigured = hasAlpineKeys)

            // Auto-setup Alpine SSH keys if they exist
            if (hasAlpineKeys) {
                Log.d("GitViewModel", "🔑 Auto-setting up ED25519 SSH keys in Alpine...")
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val privateKey = alpineSshKeyManager.getPrivateKeyPEM()
                        val publicKey = alpineSshKeyManager.getPublicKey()

                        Log.d("GitViewModel", "Private key null: ${privateKey == null}")
                        Log.d("GitViewModel", "Public key null: ${publicKey == null}")

                        if (privateKey != null && publicKey != null) {
                            Log.d("GitViewModel", "📤 Calling alpineGitHelper.setupSshKeys()...")
                            alpineGitHelper.setupSshKeys(privateKey, publicKey)
                            Log.d("GitViewModel", "✅ ED25519 SSH keys auto-configured in Alpine")
                        } else {
                            Log.w(
                                    "GitViewModel",
                                    "⚠️ Keys exist but are null - this shouldn't happen"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("GitViewModel", "❌ Failed to auto-setup SSH keys", e)
                    }
                }
            } else {
                Log.d("GitViewModel", "ℹ️ No ED25519 SSH keys found - user needs to set up")
            }

            // Observe logs
            viewModelScope.launch {
                GitLogger.logs.collect { logs ->
                    _uiState.value =
                            _uiState.value.copy(logs = logs.reversed()) // Show newest first
                }
            }

            // Periodically update terminal history
            viewModelScope.launch {
                while (true) {
                    delay(500) // Update every 500ms
                    val history = alpineGitHelper.getCommandHistory()
                    if (history != _uiState.value.terminalHistory) {
                        _uiState.value = _uiState.value.copy(terminalHistory = history)
                    }
                }
            }
        }
    }

    fun setRootDir(path: String) {
        val file = File(path)
        Log.d("GitViewModel", "🔧 setRootDir called with: $path")
        Log.d(
                "GitViewModel",
                "📁 File exists: ${file.exists()}, .git exists: ${File(file, ".git").exists()}"
        )

        if (currentRoot != file) {
            currentRoot = file
            repository?.close()
            Log.d("GitViewModel", "🔄 Creating new GitRepository for: ${file.absolutePath}")
            repository = GitRepository(file)
            Log.d("GitViewModel", "📊 Repository initialized: ${repository?.isInitialized()}")
            refreshStatus()
        } else {
            Log.d("GitViewModel", "♻️ Same root, skipping re-initialization")
        }
    }

    fun refreshStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = repository ?: return@launch
            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Refreshing..."
                    )

            if (!repo.isInitialized()) {
                _uiState.value =
                        _uiState.value.copy(
                                isInitialized = false,
                                isLoading = false,
                                operationInProgress = null
                        )
                return@launch
            }

            val (name, email) = repo.getConfig()
            val remote = repo.getRemote()
            val status = repo.getStatus()
            val currentBranch = repo.getCurrentBranch()
            val branches = repo.getBranches()
            val commits = repo.getCommitLog(30)
            val stashes = repo.listStashes()
            val (ahead, behind) = repo.getAheadBehind()

            if (status != null) {
                val modified = status.modified + status.missing
                val staged = status.added + status.changed + status.removed
                val untracked = status.untracked
                val conflicted = repo.getConflictedFiles()

                Log.d(
                        "GitViewModel",
                        "📊 Status: modified=${modified.size}, staged=${staged.size}, untracked=${untracked.size}, conflicts=${conflicted.size}"
                )

                _uiState.value =
                        _uiState.value.copy(
                                isInitialized = true,
                                modifiedFiles = modified.toList().sorted(),
                                stagedFiles = staged.toList().sorted(),
                                untrackedFiles = untracked.toList().sorted(),
                                conflictedFiles = conflicted,
                                userName = name,
                                userEmail = email,
                                remoteUrl = remote,
                                currentBranch = currentBranch,
                                branches = branches,
                                commits = commits,
                                stashes = stashes,
                                aheadCount = ahead,
                                behindCount = behind,
                                sshKeysConfigured = alpineSshKeyManager.hasKeys(),
                                isLoading = false,
                                operationInProgress = null
                        )
            } else {
                Log.w("GitViewModel", "⚠️ Status is null - repo may have issues")
                // Still show as initialized if we got this far
                _uiState.value =
                        _uiState.value.copy(
                                isInitialized = true,
                                currentBranch = currentBranch,
                                branches = branches,
                                commits = commits,
                                stashes = stashes,
                                userName = name,
                                userEmail = email,
                                remoteUrl = remote,
                                aheadCount = ahead,
                                behindCount = behind,
                                sshKeysConfigured = alpineSshKeyManager.hasKeys(),
                                isLoading = false,
                                operationInProgress = null
                        )
            }
        }
    }

    fun getSshKeyManager(): GitSshKeyManager = sshKeyManager

    fun getAlpineSshKeyManager(): AlpineSshKeyManager = alpineSshKeyManager

    fun getAlpineGitHelper(): AlpineGitHelper = alpineGitHelper

    fun initRepo() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Initializing repository...")
            when (val result = repository?.init()) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository set")
            }
            refreshStatus()
        }
    }

    fun stageFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Staging file...")
            when (val result = repository?.stageFile(path)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> {}
            }
            refreshStatus()
        }
    }

    fun unstageFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Unstaging file...")
            when (val result = repository?.unstageFile(path)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> {}
            }
            refreshStatus()
        }
    }

    fun doCommit(message: String, files: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Committing changes...")

            // 1. Reset Mixed (Unstage all) to ensure we only commit what is selected
            repository?.resetMixed()

            // 2. Stage selected files
            val stageResult = repository?.stageFiles(files)
            if (stageResult is GitResult.Error) {
                showError(stageResult.message, stageResult.details)
                refreshStatus()
                return@launch
            }

            when (val result = repository?.commit(message)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    /**
     * Commits changes and pushes to remote using SSH. No credentials needed - uses SSH keys
     * automatically.
     */
    fun commitAndPush(message: String, files: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if SSH is configured
            if (!alpineSshKeyManager.hasKeys()) {
                showError("SSH not configured", "Please set up SSH keys first")
                return@launch
            }

            _uiState.value = _uiState.value.copy(operationInProgress = "Committing...")

            // Stage and commit
            repository?.resetMixed()
            val stageResult = repository?.stageFiles(files)
            if (stageResult is GitResult.Error) {
                showError(stageResult.message, stageResult.details)
                refreshStatus()
                return@launch
            }

            when (val result = repository?.commit(message)) {
                is GitResult.Success -> {
                    // Then push
                    _uiState.value = _uiState.value.copy(operationInProgress = "Pushing...")
                    push()
                }
                is GitResult.Error -> {
                    showError(result.message, result.details)
                    refreshStatus()
                }
                null -> showError("No repository")
            }
        }
    }

    fun setIdentity(name: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Saving identity...")
            when (val result = repository?.setConfig(name, email)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> {}
            }
            refreshStatus()
        }
    }

    fun setRemote(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Setting remote...")
            when (val result = repository?.setRemote(url)) {
                is GitResult.Success -> {
                    _uiState.value =
                            _uiState.value.copy(
                                    remoteUrl = url,
                                    lastSuccess = result.message,
                                    lastError = null,
                                    operationInProgress = null
                            )
                }
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository initialized")
            }
        }
    }

    // === Network Operations (SSH-Only) ===

    /**
     * Push commits to remote using SSH authentication. No credentials needed - uses SSH keys
     * automatically.
     */
    fun push() {
        Log.d("GitViewModel", "🚀 push() called")
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("GitViewModel", "🔄 Inside push coroutine")

            // Check if SSH is configured
            val hasKeys = alpineSshKeyManager.hasKeys()
            Log.d("GitViewModel", "🔑 SSH keys configured: $hasKeys")
            if (!hasKeys) {
                showError("SSH not configured", "Please set up SSH keys first")
                return@launch
            }

            val repoPath = currentRoot?.absolutePath
            Log.d("GitViewModel", "📁 Repository path: $repoPath")
            if (repoPath == null) {
                showError("No repository", "Open a folder with a Git repository first")
                return@launch
            }

            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Pushing to remote..."
                    )

            // Use Alpine's native git for SSH operations
            val branch = repository?.getCurrentBranch() ?: "main"
            Log.d("GitViewModel", "🌿 Branch: $branch")
            GitLogger.log("git push origin $branch", LogType.COMMAND)

            Log.d("GitViewModel", "⏳ Calling alpineGitHelper.push()...")
            val result = alpineGitHelper.push(repoPath, branch)
            Log.d("GitViewModel", "✅ alpineGitHelper.push() returned: $result")

            result.fold(
                    onSuccess = { output ->
                        Log.d("GitViewModel", "✅ Push success: $output")
                        GitLogger.log("Push completed successfully", LogType.SUCCESS)
                        showSuccess("Push completed")
                    },
                    onFailure = { e ->
                        Log.e("GitViewModel", "❌ Push failed: ${e.message}", e)
                        GitLogger.log("Push failed: ${e.message}", LogType.ERROR)
                        showError("Push failed", e.message)
                    }
            )
            refreshStatus()
        }
    }

    /**
     * Pull changes from remote using SSH authentication. No credentials needed - uses SSH keys
     * automatically.
     */
    fun pull() {
        pullFromBranch(null) // Pull from current branch
    }

    /**
     * Pull changes from a specific remote branch using SSH authentication.
     * @param remoteBranch The remote branch to pull from, or null to use current branch
     */
    fun pullFromBranch(remoteBranch: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if SSH is configured
            if (!alpineSshKeyManager.hasKeys()) {
                showError("SSH not configured", "Please set up SSH keys first")
                return@launch
            }

            val repoPath = currentRoot?.absolutePath
            if (repoPath == null) {
                showError("No repository", "Open a folder with a Git repository first")
                return@launch
            }

            val branch = remoteBranch ?: repository?.getCurrentBranch() ?: "main"

            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Pulling from origin/$branch..."
                    )

            // Use Alpine's native git for SSH operations
            GitLogger.log("git pull origin $branch", LogType.COMMAND)
            val result = alpineGitHelper.pull(repoPath, branch)
            result.fold(
                    onSuccess = { output ->
                        showSuccess("Pull from '$branch' completed")
                        Log.d("GitViewModel", "Pull output: $output")
                    },
                    onFailure = { e -> showError("Pull failed", e.message) }
            )
            refreshStatus()
        }
    }

    /**
     * Fetch changes from remote using SSH authentication. No credentials needed - uses SSH keys
     * automatically.
     */
    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if SSH is configured
            if (!alpineSshKeyManager.hasKeys()) {
                showError("SSH not configured", "Please set up SSH keys first")
                return@launch
            }

            val repoPath = currentRoot?.absolutePath
            if (repoPath == null) {
                showError("No repository", "Open a folder with a Git repository first")
                return@launch
            }

            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Fetching from remote..."
                    )

            // Use Alpine's native git for SSH operations
            GitLogger.log("git fetch origin", LogType.COMMAND)
            val result = alpineGitHelper.fetch(repoPath)
            result.fold(
                    onSuccess = { output ->
                        showSuccess("Fetch completed")
                        Log.d("GitViewModel", "Fetch output: $output")
                    },
                    onFailure = { e -> showError("Fetch failed", e.message) }
            )
            refreshStatus()
        }
    }

    fun loadDiff(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Loading diff...")
            val diff = repository?.getDiff(filePath) ?: "No diff available"
            _uiState.value = _uiState.value.copy(currentDiff = diff, operationInProgress = null)
        }
    }

    fun clearDiff() {
        _uiState.value = _uiState.value.copy(currentDiff = null)
    }

    // === Merge Conflict Resolution ===

    /**
     * Open the merge editor for a conflicted file.
     * @param relativePath Path relative to repository root
     */
    fun openMergeEditor(relativePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("GitViewModel", "📂 openMergeEditor called for: $relativePath")
            val content = repository?.getFileContent(relativePath)
            Log.d(
                    "GitViewModel",
                    "📄 Content retrieved: ${content?.length ?: 0} chars, hasConflictMarkers=${content?.contains("<<<<<<<") == true}"
            )

            if (content != null) {
                Log.d(
                        "GitViewModel",
                        "📝 Setting merge editor state: path=$relativePath, contentPreview=${content.take(200)}"
                )
                _uiState.value =
                        _uiState.value.copy(
                                showMergeEditor = true,
                                mergeEditorFile = relativePath,
                                mergeEditorContent = content
                        )
                Log.d("GitViewModel", "✅ Merge editor state updated")
            } else {
                showError("Cannot open file", "Failed to read file content")
            }
        }
    }

    /**
     * Save the merged content and mark the conflict as resolved.
     * @param relativePath Path relative to repository root
     * @param content The resolved content
     */
    fun saveMergedContent(relativePath: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Saving merged content...")

            when (val writeResult = repository?.writeFileContent(relativePath, content)) {
                is GitResult.Success -> {
                    // Stage the file to mark conflict as resolved
                    when (val stageResult = repository?.markConflictResolved(relativePath)) {
                        is GitResult.Success -> {
                            showSuccess("Conflict resolved: $relativePath")
                            closeMergeEditor()
                        }
                        is GitResult.Error -> showError(stageResult.message, stageResult.details)
                        null -> {}
                    }
                }
                is GitResult.Error -> showError(writeResult.message, writeResult.details)
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    /** Close the merge editor without saving. */
    fun closeMergeEditor() {
        _uiState.value =
                _uiState.value.copy(
                        showMergeEditor = false,
                        mergeEditorFile = null,
                        mergeEditorContent = null
                )
    }

    fun clearMessages() {
        _uiState.value =
                _uiState.value.copy(
                        lastError = null,
                        lastSuccess = null,
                        logs = emptyList(),
                        terminalHistory = emptyList()
                )
        alpineGitHelper.clearHistory()
    }

    // === Branch Operations ===

    fun createBranch(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Creating branch '$name'...")
            when (val result = repository?.createBranch(name)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    fun checkoutBranch(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            operationInProgress = "Switching to '$name'..."
                    )
            when (val result = repository?.checkoutBranch(name)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    fun deleteBranch(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Deleting branch '$name'...")
            when (val result = repository?.deleteBranch(name)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    // === Stash ===

    fun stash() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Stashing changes...")
            when (val result = repository?.stash()) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    fun stashPop() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Applying stash...")
            when (val result = repository?.stashPop()) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    // === Advanced Branch Operations (via Alpine) ===

    /** Create a new branch from a specific source branch */
    fun createBranchFrom(newName: String, sourceBranch: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val repoPath = currentRoot?.absolutePath ?: return@launch showError("No repository")
            _uiState.value =
                    _uiState.value.copy(
                            operationInProgress =
                                    "Creating branch '$newName' from '$sourceBranch'..."
                    )
            GitLogger.log("git checkout -b $newName $sourceBranch", LogType.COMMAND)

            val result = alpineGitHelper.createBranchFrom(repoPath, newName, sourceBranch)
            result.fold(
                    onSuccess = { showSuccess("Created branch '$newName' from '$sourceBranch'") },
                    onFailure = { e -> showError("Failed to create branch", e.message) }
            )
            refreshStatus()
        }
    }

    /** Delete a branch (optionally from remote) */
    fun deleteBranchWithRemote(branchName: String, deleteRemote: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val repoPath = currentRoot?.absolutePath ?: return@launch showError("No repository")

            // Delete local first
            _uiState.value =
                    _uiState.value.copy(
                            operationInProgress = "Deleting local branch '$branchName'..."
                    )
            GitLogger.log("git branch -d $branchName", LogType.COMMAND)

            val localResult = alpineGitHelper.deleteBranchLocal(repoPath, branchName)
            localResult.fold(
                    onSuccess = {
                        if (deleteRemote) {
                            _uiState.value =
                                    _uiState.value.copy(
                                            operationInProgress =
                                                    "Deleting remote branch '$branchName'..."
                                    )
                            GitLogger.log("git push origin --delete $branchName", LogType.COMMAND)
                            val remoteResult =
                                    alpineGitHelper.deleteBranchRemote(repoPath, branchName)
                            remoteResult.fold(
                                    onSuccess = {
                                        showSuccess("Deleted branch '$branchName' (local + remote)")
                                    },
                                    onFailure = { e ->
                                        showError(
                                                "Deleted local but failed to delete remote",
                                                e.message
                                        )
                                    }
                            )
                        } else {
                            showSuccess("Deleted local branch '$branchName'")
                        }
                    },
                    onFailure = { e -> showError("Failed to delete branch", e.message) }
            )
            refreshStatus()
        }
    }

    /** Rename a branch */
    fun renameBranch(oldName: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val repoPath = currentRoot?.absolutePath ?: return@launch showError("No repository")
            _uiState.value =
                    _uiState.value.copy(
                            operationInProgress = "Renaming branch '$oldName' to '$newName'..."
                    )
            GitLogger.log("git branch -m $oldName $newName", LogType.COMMAND)

            val result = alpineGitHelper.renameBranch(repoPath, oldName, newName)
            result.fold(
                    onSuccess = { showSuccess("Renamed branch to '$newName'") },
                    onFailure = { e -> showError("Failed to rename branch", e.message) }
            )
            refreshStatus()
        }
    }

    /** Merge a branch into current branch */
    fun mergeBranch(branchName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val repoPath = currentRoot?.absolutePath ?: return@launch showError("No repository")
            _uiState.value =
                    _uiState.value.copy(
                            operationInProgress = "Merging '$branchName' into current branch..."
                    )
            GitLogger.log("git merge $branchName", LogType.COMMAND)

            val result = alpineGitHelper.merge(repoPath, branchName)
            result.fold(
                    onSuccess = { output ->
                        if (output.contains("CONFLICT")) {
                            showError("Merge has conflicts", "Resolve conflicts manually")
                        } else {
                            showSuccess("Merged '$branchName' successfully")
                        }
                    },
                    onFailure = { e -> showError("Merge failed", e.message) }
            )
            refreshStatus()
        }
    }

    /** Rebase current branch onto another branch */
    fun rebaseBranch(ontoBranch: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val repoPath = currentRoot?.absolutePath ?: return@launch showError("No repository")
            _uiState.value =
                    _uiState.value.copy(operationInProgress = "Rebasing onto '$ontoBranch'...")
            GitLogger.log("git rebase $ontoBranch", LogType.COMMAND)

            val result = alpineGitHelper.rebase(repoPath, ontoBranch)
            result.fold(
                    onSuccess = { output ->
                        if (output.contains("CONFLICT")) {
                            showError(
                                    "Rebase has conflicts",
                                    "Resolve conflicts and run 'git rebase --continue'"
                            )
                        } else {
                            showSuccess("Rebased onto '$ontoBranch' successfully")
                        }
                    },
                    onFailure = { e -> showError("Rebase failed", e.message) }
            )
            refreshStatus()
        }
    }

    /** Abort a merge in progress */
    fun abortMerge() {
        viewModelScope.launch(Dispatchers.IO) {
            val repoPath = currentRoot?.absolutePath ?: return@launch showError("No repository")
            _uiState.value = _uiState.value.copy(operationInProgress = "Aborting merge...")
            GitLogger.log("git merge --abort", LogType.COMMAND)

            val result = alpineGitHelper.mergeAbort(repoPath)
            result.fold(
                    onSuccess = { showSuccess("Merge aborted") },
                    onFailure = { e -> showError("Failed to abort merge", e.message) }
            )
            refreshStatus()
        }
    }

    /** Abort a rebase in progress */
    fun abortRebase() {
        viewModelScope.launch(Dispatchers.IO) {
            val repoPath = currentRoot?.absolutePath ?: return@launch showError("No repository")
            _uiState.value = _uiState.value.copy(operationInProgress = "Aborting rebase...")
            GitLogger.log("git rebase --abort", LogType.COMMAND)

            val result = alpineGitHelper.rebaseAbort(repoPath)
            result.fold(
                    onSuccess = { showSuccess("Rebase aborted") },
                    onFailure = { e -> showError("Failed to abort rebase", e.message) }
            )
            refreshStatus()
        }
    }

    // === Helpers ===

    fun showSuccess(message: String) {
        _uiState.value =
                _uiState.value.copy(
                        lastSuccess = message,
                        lastError = null,
                        operationInProgress = null
                )
    }

    fun showError(message: String, details: String? = null) {
        val fullMessage = if (details != null) "$message: $details" else message
        _uiState.value =
                _uiState.value.copy(
                        lastError = fullMessage,
                        lastSuccess = null,
                        operationInProgress = null
                )
    }

    /**
     * Clone a repository using SSH authentication. Automatically converts HTTPS URLs to SSH format.
     * No credentials needed - uses SSH keys automatically.
     */
    fun cloneRepository(url: String, targetPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if SSH is configured
            if (!alpineSshKeyManager.hasKeys()) {
                showError("SSH not configured", "Please set up SSH keys first")
                return@launch
            }

            Log.d("GitViewModel", "🚀 cloneRepository called for: $url -> $targetPath")
            _uiState.value =
                    _uiState.value.copy(
                            operationInProgress = "Cloning repository...",
                            isLoading = true
                    )

            // Convert HTTPS URL to SSH if needed
            val sshUrl = GitUrlConverter.ensureSsh(url)
            Log.d("GitViewModel", "🔄 Converted URL: $url -> $sshUrl")

            // Use the full path provided by user
            val targetDir = File(targetPath)

            // Check if parent directory exists
            val parentDir = targetDir.parentFile
            if (parentDir == null || !parentDir.exists()) {
                Log.e("GitViewModel", "❌ Parent directory missing: ${parentDir?.absolutePath}")
                showError(
                        "Invalid path",
                        "Parent directory does not exist: ${parentDir?.absolutePath}"
                )
                return@launch
            }

            if (targetDir.exists()) {
                Log.e("GitViewModel", "❌ Target directory exists: ${targetDir.absolutePath}")
                showError(
                        "Directory already exists",
                        "Choose a different path or delete the existing directory"
                )
                return@launch
            }

            // Use Alpine's native git for SSH clone
            GitLogger.log("git clone $sshUrl", LogType.COMMAND)
            Log.d("GitViewModel", "⏳ Calling alpineGitHelper.clone()...")
            val result = alpineGitHelper.clone(sshUrl, targetPath)
            Log.d("GitViewModel", "✅ alpineGitHelper.clone() returned: $result")

            result.fold(
                    onSuccess = { output ->
                        Log.d("GitViewModel", "✅ Clone success: $output")
                        // Set this as the current repository
                        repository?.close()
                        repository = GitRepository(targetDir)
                        currentRoot = targetDir

                        showSuccess("Cloned successfully to ${targetDir.absolutePath}")
                        Log.d("GitViewModel", "Clone output: $output")
                        refreshStatus()
                    },
                    onFailure = { e ->
                        Log.e("GitViewModel", "❌ Clone failed: ${e.message}", e)
                        showError("Clone failed", e.message)
                    }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository?.close()
    }
}
