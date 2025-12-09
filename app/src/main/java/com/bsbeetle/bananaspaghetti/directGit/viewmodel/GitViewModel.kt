package com.bsbeetle.bananaspaghetti.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.directGit.utils.GitCredentialManager
import com.bsbeetle.bananaspaghetti.utils.git.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GitUiState(
        val isInitialized: Boolean = false,
        val modifiedFiles: List<String> = emptyList(),
        val stagedFiles: List<String> = emptyList(),
        val untrackedFiles: List<String> = emptyList(),
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
        val logs: List<LogEntry> = emptyList()
)

class GitViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()

    private var repository: GitRepository? = null
    private var currentRoot: File? = null
    private val credentialManager = GitCredentialManager(application)

    init {
        // Observe logs
        viewModelScope.launch {
            GitLogger.logs.collect { logs ->
                _uiState.value = _uiState.value.copy(logs = logs.reversed()) // Show newest first
            }
        }
    }

    fun setRootDir(path: String) {
        val file = File(path)
        if (currentRoot != file) {
            currentRoot = file
            repository?.close()
            repository = GitRepository(file)
            refreshStatus()
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

                _uiState.value =
                        _uiState.value.copy(
                                isInitialized = true,
                                modifiedFiles = modified.toList().sorted(),
                                stagedFiles = staged.toList().sorted(),
                                untrackedFiles = untracked.toList().sorted(),
                                userName = name,
                                userEmail = email,
                                remoteUrl = remote,
                                currentBranch = currentBranch,
                                branches = branches,
                                commits = commits,
                                stashes = stashes,
                                aheadCount = ahead,
                                behindCount = behind,
                                isLoading = false,
                                operationInProgress = null
                        )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, operationInProgress = null)
            }
        }
    }

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

    fun commit(message: String, filesToCommit: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Committing changes...")

            // 1. Reset Mixed (Unstage all) to ensure we only commit what is selected
            repository?.resetMixed()

            // 2. Stage selected files
            val stageResult = repository?.stageFiles(filesToCommit)
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

    fun commitAndPush(
            message: String,
            filesToCommit: List<String>,
            username: String? = null,
            token: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(operationInProgress = "Committing...")

            // 1. Reset Mixed
            repository?.resetMixed()

            // 2. Stage & Commit
            val stageResult = repository?.stageFiles(filesToCommit)
            if (stageResult is GitResult.Error) {
                showError(stageResult.message, stageResult.details)
                refreshStatus()
                return@launch
            }

            val commitResult = repository?.commit(message)
            if (commitResult !is GitResult.Success) {
                if (commitResult is GitResult.Error)
                        showError(commitResult.message, commitResult.details)
                refreshStatus()
                return@launch
            }

            // 2. Push
            _uiState.value = _uiState.value.copy(operationInProgress = "Pushing...")

            val remote = repository?.getRemote() ?: ""
            var finalUser = username
            var finalToken = token

            if (finalUser == null || finalToken == null) {
                val stored = credentialManager.getCredentials(remote)
                if (stored != null) {
                    finalUser = stored.first
                    finalToken = stored.second
                }
            }

            if (finalUser == null || finalToken == null) {
                showError("Authentication required", "Please enter credentials")
                return@launch
            }

            // Save if new credentials provided (logic handled in UI usually, but good to have here)
            if (username != null && token != null && remote.isNotEmpty()) {
                credentialManager.saveCredentials(remote, username, token)
            }

            when (val result = repository?.push(finalUser, finalToken)) {
                is GitResult.Success -> showSuccess("Committed & Pushed: ${commitResult.data}")
                is GitResult.Error -> {
                    if (result.message.contains("Authentication failed")) {
                        credentialManager.clearCredentials(remote)
                    }
                    showError(result.message, result.details)
                }
                null -> showError("No repository")
            }
            refreshStatus()
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

    // === Network Operations with Credentials ===

    fun getStoredCredentials(): Pair<String, String>? {
        val remote = repository?.getRemote() ?: return null
        return credentialManager.getCredentials(remote)
    }

    fun push(username: String? = null, token: String? = null, save: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Pushing to remote..."
                    )

            val remote = repository?.getRemote() ?: ""
            var finalUser = username
            var finalToken = token

            // Try stored credentials if not provided
            if (finalUser == null || finalToken == null) {
                val stored = credentialManager.getCredentials(remote)
                if (stored != null) {
                    finalUser = stored.first
                    finalToken = stored.second
                }
            }

            if (finalUser == null || finalToken == null) {
                showError("Authentication required", "Please enter credentials")
                return@launch
            }

            if (save && remote.isNotEmpty()) {
                credentialManager.saveCredentials(remote, finalUser, finalToken)
            }

            when (val result = repository?.push(finalUser, finalToken)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> {
                    // If auth failed, maybe clear credentials?
                    if (result.message.contains("Authentication failed")) {
                        credentialManager.clearCredentials(remote)
                    }
                    showError(result.message, result.details)
                }
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    fun pull(username: String? = null, token: String? = null, save: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Pulling from remote..."
                    )

            val remote = repository?.getRemote() ?: ""
            var finalUser = username
            var finalToken = token

            if (finalUser == null || finalToken == null) {
                val stored = credentialManager.getCredentials(remote)
                if (stored != null) {
                    finalUser = stored.first
                    finalToken = stored.second
                }
            }

            if (finalUser == null || finalToken == null) {
                showError("Authentication required", "Please enter credentials")
                return@launch
            }

            if (save && remote.isNotEmpty()) {
                credentialManager.saveCredentials(remote, finalUser, finalToken)
            }

            when (val result = repository?.pull(finalUser, finalToken)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> {
                    if (result.message.contains("Authentication failed")) {
                        credentialManager.clearCredentials(remote)
                    }
                    showError(result.message, result.details)
                }
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    fun fetch(username: String? = null, token: String? = null, save: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Fetching from remote..."
                    )

            val remote = repository?.getRemote() ?: ""
            var finalUser = username
            var finalToken = token

            if (finalUser == null || finalToken == null) {
                val stored = credentialManager.getCredentials(remote)
                if (stored != null) {
                    finalUser = stored.first
                    finalToken = stored.second
                }
            }

            // Fetch might work without auth for public repos, but usually needs it for private
            // If no creds, try without (pass empty strings or handle differently)
            // For now, assume we need them if it's a private repo, but let's try with what we have

            val userToUse = finalUser ?: ""
            val tokenToUse = finalToken ?: ""

            when (val result = repository?.fetch(userToUse, tokenToUse)) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
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

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(lastError = null, lastSuccess = null)
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

    // === Helpers ===

    private fun showSuccess(message: String) {
        _uiState.value =
                _uiState.value.copy(
                        lastSuccess = message,
                        lastError = null,
                        operationInProgress = null
                )
    }

    private fun showError(message: String, details: String? = null) {
        val fullMessage = if (details != null) "$message: $details" else message
        _uiState.value =
                _uiState.value.copy(
                        lastError = fullMessage,
                        lastSuccess = null,
                        operationInProgress = null
                )
    }

    fun cloneRepository(url: String, targetPath: String, username: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                    _uiState.value.copy(
                            operationInProgress = "Cloning repository...",
                            isLoading = true
                    )

            // Use the full path provided by user
            val targetDir = File(targetPath)

            // Check if parent directory exists
            val parentDir = targetDir.parentFile
            if (parentDir == null || !parentDir.exists()) {
                showError(
                        "Invalid path",
                        "Parent directory does not exist: ${parentDir?.absolutePath}"
                )
                return@launch
            }

            if (targetDir.exists()) {
                showError(
                        "Directory already exists",
                        "Choose a different path or delete the existing directory"
                )
                return@launch
            }

            when (val result = GitRepository.clone(url, targetDir, username, token)) {
                is GitResult.Success -> {
                    // Save credentials for this repo
                    credentialManager.saveCredentials(url, username, token)

                    // Set this as the current repository
                    repository?.close()
                    repository = result.data
                    currentRoot = targetDir

                    showSuccess("Cloned successfully to ${targetDir.absolutePath}")
                    refreshStatus()
                }
                is GitResult.Error -> showError(result.message, result.details)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository?.close()
    }
}
