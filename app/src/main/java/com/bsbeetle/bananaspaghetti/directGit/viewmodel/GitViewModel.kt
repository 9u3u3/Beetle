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
        val terminalHistory: List<String> = emptyList()
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

    init {
        Log.d("GitViewModel", "🚀 Initializing GitViewModel...")

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
                        Log.w("GitViewModel", "⚠️ Keys exist but are null - this shouldn't happen")
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
                _uiState.value = _uiState.value.copy(logs = logs.reversed()) // Show newest first
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
                                sshKeysConfigured = alpineSshKeyManager.hasKeys(),
                                isLoading = false,
                                operationInProgress = null
                        )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, operationInProgress = null)
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
        viewModelScope.launch(Dispatchers.IO) {
            // Check if SSH is configured
            if (!alpineSshKeyManager.hasKeys()) {
                showError("SSH not configured", "Please set up SSH keys first")
                return@launch
            }

            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Pushing to remote..."
                    )

            when (val result = repository?.push()) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
            refreshStatus()
        }
    }

    /**
     * Pull changes from remote using SSH authentication. No credentials needed - uses SSH keys
     * automatically.
     */
    fun pull() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if SSH is configured
            if (!alpineSshKeyManager.hasKeys()) {
                showError("SSH not configured", "Please set up SSH keys first")
                return@launch
            }

            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Pulling from remote..."
                    )

            when (val result = repository?.pull()) {
                is GitResult.Success -> showSuccess(result.message)
                is GitResult.Error -> showError(result.message, result.details)
                null -> showError("No repository")
            }
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

            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            lastError = null,
                            operationInProgress = "Fetching from remote..."
                    )

            when (val result = repository?.fetch()) {
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

            _uiState.value =
                    _uiState.value.copy(
                            operationInProgress = "Cloning repository...",
                            isLoading = true
                    )

            // Convert HTTPS URL to SSH if needed
            val sshUrl = GitUrlConverter.ensureSsh(url)

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

            when (val result = GitRepository.clone(sshUrl, targetDir)) {
                is GitResult.Success -> {
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
