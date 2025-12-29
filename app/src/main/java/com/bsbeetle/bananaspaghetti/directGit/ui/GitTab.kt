package com.bsbeetle.bananaspaghetti.directGit.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.data.sidebar.FileIconUtils
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel
import com.bsbeetle.bananaspaghetti.utils.git.BranchInfo
import com.bsbeetle.bananaspaghetti.utils.git.CommitInfo
import com.bsbeetle.bananaspaghetti.utils.git.LogEntry
import com.bsbeetle.bananaspaghetti.utils.git.LogType
import com.bsbeetle.bananaspaghetti.utils.resourceHandling.ImageLoaderSingleton
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

// Import extracted dialog components
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.StyledDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.BranchDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.NewBranchDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.AuthDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.RemoteDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.CloneDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.CommitHistoryDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.DiffDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.MergeEditorDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.PullBranchDialog

// Use centralized theme colors from GitTheme object

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitTab(viewModel: GitViewModel = viewModel()) {
    val context = LocalContext.current
    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val uiState by viewModel.uiState.collectAsState()
    var commitMessage by remember { mutableStateOf("") }

    // Check for storage permission
    val hasStoragePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Older Android versions don't need this
        }
    }
    var permissionCheckTrigger by remember { mutableStateOf(0) }
    val currentHasPermission =
            remember(permissionCheckTrigger) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }
            }

    // Dialog States
    var showAuthDialog by remember { mutableStateOf(false) }
    var showRemoteDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showConsole by remember { mutableStateOf(false) }
    var showCloneDialog by remember { mutableStateOf(false) }
    var showSshSetupDialog by remember { mutableStateOf(false) }
    var showPullBranchDialog by remember { mutableStateOf(false) }

    // SSH setup dialog is now only shown when user clicks SSH button in header
    // (removed auto-popup behavior)

    // Action to retry after auth
    var authAction by remember { mutableStateOf<((String, String, Boolean) -> Unit)?>(null) }

    // Unified changes list (including conflicted files)
    val allChanges =
            remember(
                    uiState.stagedFiles,
                    uiState.modifiedFiles,
                    uiState.untrackedFiles,
                    uiState.conflictedFiles
            ) {
                (uiState.stagedFiles +
                                uiState.modifiedFiles +
                                uiState.untrackedFiles +
                                uiState.conflictedFiles)
                        .distinct()
                        .sorted()
            }
    var selectedFiles by remember { mutableStateOf(setOf<String>()) }

    // Initialize selection
    LaunchedEffect(allChanges) {
        if (selectedFiles.isEmpty() && allChanges.isNotEmpty()) {
            selectedFiles = allChanges.toSet()
        } else {
            selectedFiles = selectedFiles.intersect(allChanges.toSet())
        }
    }

    // Initialize with current project root from file path
    LaunchedEffect(fileViewModel.currentFilePath) {
        val currentPath = fileViewModel.currentFilePath
        if (currentPath != null) {
            var dir = File(currentPath).parentFile
            while (dir != null) {
                if (File(dir, ".git").exists()) {
                    viewModel.setRootDir(dir.absolutePath)
                    break
                }
                dir = dir.parentFile
            }
            if (dir == null && currentPath.isNotEmpty()) {
                viewModel.setRootDir(File(currentPath).parentFile?.absolutePath ?: "")
            }
        }
    }

    // Also watch for folder selection (when user opens a folder directly)
    LaunchedEffect(fileViewModel.selectedFolderUri) {
        val folderUri = fileViewModel.selectedFolderUri
        if (folderUri != null) {
            // Extract real file path from DocumentFile URI
            // URI format: content://com.android.externalstorage.documents/tree/primary%3Afolder
            val uriPath = folderUri.toString()
            android.util.Log.d("GitTab", "🔍 Folder URI: $uriPath")

            // Extract the path after "primary:" or "primary%3A"
            val realPath =
                    when {
                        uriPath.contains("primary:") -> {
                            val pathPart =
                                    uriPath.substringAfter("primary:")
                                            .substringBefore("/document/")
                                            .replace("%2F", "/")
                                            .replace("%3A", ":")
                            "/storage/emulated/0/$pathPart"
                        }
                        uriPath.contains("primary%3A") -> {
                            val pathPart =
                                    uriPath.substringAfter("primary%3A")
                                            .substringBefore("/document/")
                                            .replace("%2F", "/")
                                            .replace("%3A", ":")
                            "/storage/emulated/0/$pathPart"
                        }
                        // Handle Alpine internal storage URIs
                        folderUri.authority == "${context.packageName}.documents" -> {
                            DocumentsContract.getDocumentId(folderUri)
                        }
                        // Also handle raw file paths from fileViewModel.currentFilePath
                        else -> null
                    }

            android.util.Log.d("GitTab", "📂 Extracted path: $realPath")

            if (realPath != null) {
                val dir = File(realPath)
                android.util.Log.d(
                        "GitTab",
                        "📁 Dir exists: ${dir.exists()}, .git exists: ${File(dir, ".git").exists()}"
                )

                // ALWAYS check if this folder itself is a Git repo first
                if (dir.exists() && File(dir, ".git").exists()) {
                    // Use THIS folder's .git, not parent's
                    android.util.Log.d("GitTab", "✅ Found .git in: ${dir.absolutePath}")
                    viewModel.setRootDir(dir.absolutePath)
                } else if (dir.exists()) {
                    // Only walk up if current folder doesn't have .git
                    var parentDir = dir.parentFile
                    var foundGit = false
                    while (parentDir != null) {
                        if (File(parentDir, ".git").exists()) {
                            android.util.Log.d(
                                    "GitTab",
                                    "✅ Found .git in parent: ${parentDir.absolutePath}"
                            )
                            viewModel.setRootDir(parentDir.absolutePath)
                            foundGit = true
                            break
                        }
                        parentDir = parentDir.parentFile
                    }
                    // Even if no .git found, set root dir so Git tab can show "init repo" option
                    if (!foundGit) {
                        android.util.Log.d(
                                "GitTab",
                                "⚠️ No .git found, setting root anyway: ${dir.absolutePath}"
                        )
                        viewModel.setRootDir(dir.absolutePath)
                    }
                }
            }
        }
    }

    // === Dialogs ===
    // SSH Setup Dialog
    if (showSshSetupDialog) {
        SshSetupDialog(
                viewModel = viewModel,
                onDismiss = { showSshSetupDialog = false },
                onComplete = { viewModel.refreshStatus() }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
                onDismiss = { showAuthDialog = false },
                onConfirm = { u, t, s -> authAction?.invoke(u, t, s) }
        )
    }

    if (showCloneDialog) {
        CloneDialog(
                onDismiss = { showCloneDialog = false },
                onClone = { url, directory ->
                    viewModel.cloneRepository(url, directory)
                    showCloneDialog = false
                }
        )
    }

    if (showRemoteDialog) {
        RemoteDialog(
                currentUrl = uiState.remoteUrl,
                onDismiss = { showRemoteDialog = false },
                onConfirm = { url ->
                    viewModel.setRemote(url)
                    showRemoteDialog = false
                }
        )
    }

    if (showBranchDialog) {
        BranchDialog(
                branches = uiState.branches,
                onDismiss = { showBranchDialog = false },
                onCheckout = { name ->
                    viewModel.checkoutBranch(name)
                    showBranchDialog = false
                },
                onDelete = { name, deleteRemote ->
                    viewModel.deleteBranchWithRemote(name, deleteRemote)
                },
                onNewBranch = { showNewBranchDialog = true }
        )
    }

    if (showNewBranchDialog) {
        NewBranchDialog(
                branches = uiState.branches,
                currentBranch = uiState.currentBranch,
                onDismiss = { showNewBranchDialog = false },
                onCreate = { name, sourceBranch ->
                    viewModel.createBranchFrom(name, sourceBranch)
                    showNewBranchDialog = false
                }
        )
    }

    if (showHistoryDialog) {
        CommitHistoryDialog(commits = uiState.commits, onDismiss = { showHistoryDialog = false })
    }

    uiState.currentDiff?.let { diff ->
        DiffDialog(diffText = diff, onDismiss = { viewModel.clearDiff() })
    }

    // Merge Editor Dialog (for conflict resolution)
    if (uiState.showMergeEditor &&
                    uiState.mergeEditorFile != null &&
                    uiState.mergeEditorContent != null
    ) {
        MergeEditorDialog(
                filePath = uiState.mergeEditorFile!!,
                content = uiState.mergeEditorContent!!,
                onSave = { path, content -> viewModel.saveMergedContent(path, content) },
                onDismiss = { viewModel.closeMergeEditor() }
        )
    }

    // Check if Git is configured FIRST before showing any Git UI
    val prefs = remember { com.bsbeetle.bananaspaghetti.utils.preferences.EditorPreferences(context) }
    
    // Check if SSH key exists
    val sshKeyExists = remember {
        val ubuntuPath = "${context.filesDir.absolutePath}/ubuntu/home/.ssh/id_ed25519.pub"
        val file = java.io.File(ubuntuPath)
        file.exists()
    }
    
    if (!prefs.isGitConfigured() || !sshKeyExists) {
        // Show only the setup message - no header, no controls
        Box(
            modifier = Modifier.fillMaxSize().background(GitTheme.BgDark),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = GitTheme.TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Setup Git in Settings",
                    color = GitTheme.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (!prefs.isGitConfigured()) 
                        "Add your name and email in Settings → Git"
                    else 
                        "Generate an SSH key in Settings → Git",
                    color = GitTheme.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(GitTheme.BgDark)) {
        // --- Header ---
        // === Git Header ===
        GitHeader(
                branch = uiState.currentBranch.ifEmpty { "master" },
                ahead = uiState.aheadCount,
                behind = uiState.behindCount,
                remoteBranches = uiState.branches.filter { it.isRemote },
                onBranchClick = { showBranchDialog = true },
                onFetch = { viewModel.fetch() },
                onPull = { viewModel.pull() },
                onPullFromBranch = { showPullBranchDialog = true },
                onPush = { viewModel.push() },
                onAddAll = {
                    val allFiles = uiState.modifiedFiles + uiState.untrackedFiles
                    allFiles.forEach { viewModel.stageFile(it) }
                },
                onStash = { viewModel.stash() },
                onStashPop = { viewModel.stashPop() },
                onHistory = { showHistoryDialog = true },
                onRefresh = { viewModel.refreshStatus() },
                onSshSetupClick = { showSshSetupDialog = true },
                sshKeysConfigured = uiState.sshKeysConfigured
        )

        // Pull Branch Dialog
        if (showPullBranchDialog) {
            PullBranchDialog(
                    remoteBranches = uiState.branches.filter { it.isRemote },
                    onDismiss = { showPullBranchDialog = false },
                    onSelect = { branchName ->
                        viewModel.pullFromBranch(branchName)
                        showPullBranchDialog = false
                    }
            )
        }

        // --- Main Content ---
        Box(modifier = Modifier.weight(1f)) {
            if (!currentHasPermission) {
                // Show permission request
                StoragePermissionRequest(
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent =
                                        Intent(
                                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                                                )
                                                .apply {
                                                    data =
                                                            Uri.parse(
                                                                    "package:${context.packageName}"
                                                            )
                                                }
                                context.startActivity(intent)
                            }
                        },
                        onRefresh = {
                            permissionCheckTrigger++
                            viewModel.refreshStatus()
                        }
                )
            } else if (!uiState.isInitialized) {
                EmptyState(onInit = { viewModel.initRepo() }, onClone = { showCloneDialog = true })
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Changes List
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Select All Header
                        item {
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .clickable {
                                                        selectedFiles =
                                                                if (selectedFiles.size ==
                                                                                allChanges.size
                                                                )
                                                                        emptySet()
                                                                else allChanges.toSet()
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompositionLocalProvider(
                                        LocalMinimumInteractiveComponentEnforcement provides false
                                ) {
                                    Checkbox(
                                            checked =
                                                    selectedFiles.size == allChanges.size &&
                                                            allChanges.isNotEmpty(),
                                            onCheckedChange = { checked ->
                                                selectedFiles =
                                                        if (checked) allChanges.toSet()
                                                        else emptySet()
                                            },
                                            colors =
                                                    CheckboxDefaults.colors(
                                                            checkedColor = GitTheme.AccentBlue,
                                                            uncheckedColor = GitTheme.TextTertiary,
                                                            checkmarkColor = Color.White
                                                    ),
                                            modifier = Modifier.scale(0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        "CHANGES (${allChanges.size})",
                                        color = GitTheme.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (allChanges.isEmpty()) {
                            item {
                                Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                            "No changes detected",
                                            color = GitTheme.TextTertiary,
                                            fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        items(allChanges) { path ->
                            val isStaged = uiState.stagedFiles.contains(path)
                            val isConflicted = uiState.conflictedFiles.contains(path)
                            val status =
                                    when {
                                        isConflicted -> "C" // Conflict
                                        uiState.untrackedFiles.contains(path) -> "U"
                                        uiState.modifiedFiles.contains(path) -> "M"
                                        else -> "S"
                                    }
                            FileChangeItem(
                                    path = path,
                                    status = status,
                                    isStaged = isStaged,
                                    isSelected = selectedFiles.contains(path),
                                    onToggle = {
                                        selectedFiles =
                                                if (selectedFiles.contains(path))
                                                        selectedFiles - path
                                                else selectedFiles + path
                                    },
                                    onClick = {
                                        // Open merge editor for conflicted files, diff for others
                                        if (isConflicted) {
                                            viewModel.openMergeEditor(path)
                                        } else {
                                            viewModel.loadDiff(path)
                                        }
                                    }
                            )
                        }
                    }

                    // Commit Section
                    Column(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .background(GitTheme.BgCard)
                                            .border(
                                                    1.dp,
                                                    GitTheme.BorderColor,
                                                    RoundedCornerShape(
                                                            topStart = 12.dp,
                                                            topEnd = 12.dp
                                                    )
                                            )
                                            .padding(12.dp)
                    ) {
                        // Message Input
                        BasicTextField(
                                value = commitMessage,
                                onValueChange = { commitMessage = it },
                                textStyle =
                                        TextStyle(
                                                color = GitTheme.TextPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                        ),
                                cursorBrush = SolidColor(GitTheme.AccentBlue),
                                decorationBox = { innerTextField ->
                                    Box(
                                            modifier =
                                                    Modifier.fillMaxWidth()
                                                            .height(80.dp)
                                                            .background(
                                                                    GitTheme.BgDark,
                                                                    RoundedCornerShape(6.dp)
                                                            )
                                                            .border(
                                                                    1.dp,
                                                                    GitTheme.BorderColor,
                                                                    RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(8.dp)
                                    ) {
                                        if (commitMessage.isEmpty()) {
                                            Text(
                                                    "Commit message...",
                                                    color = GitTheme.TextTertiary,
                                                    fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Commit Button
                            Button(
                                    onClick = {
                                        if (commitMessage.isNotBlank()) {
                                            viewModel.doCommit(
                                                    commitMessage,
                                                    selectedFiles.toList()
                                            )
                                            commitMessage = ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors =
                                            ButtonDefaults.buttonColors(
                                                    containerColor = GitTheme.AccentBlue,
                                                    disabledContainerColor =
                                                            GitTheme.AccentBlue.copy(alpha = 0.5f)
                                            ),
                                    shape = RoundedCornerShape(6.dp),
                                    enabled =
                                            selectedFiles.isNotEmpty() && commitMessage.isNotBlank()
                            ) {
                                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Commit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Commit & Push Button
                            Button(
                                    onClick = {
                                        if (commitMessage.isNotBlank()) {
                                            viewModel.commitAndPush(
                                                    commitMessage,
                                                    selectedFiles.toList()
                                            )
                                            commitMessage = ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors =
                                            ButtonDefaults.buttonColors(
                                                    containerColor = GitTheme.BgHover,
                                                    contentColor = GitTheme.TextPrimary,
                                                    disabledContainerColor =
                                                            GitTheme.BgHover.copy(alpha = 0.5f)
                                            ),
                                    border = BorderStroke(1.dp, GitTheme.BorderColor),
                                    shape = RoundedCornerShape(6.dp),
                                    enabled =
                                            selectedFiles.isNotEmpty() && commitMessage.isNotBlank()
                            ) {
                                Icon(
                                        Icons.Rounded.CloudUpload,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Push", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Components ---

@Composable
fun GitHeader(
        branch: String,
        ahead: Int,
        behind: Int,
        remoteBranches: List<BranchInfo>,
        onBranchClick: () -> Unit,
        onFetch: () -> Unit,
        onPull: () -> Unit,
        onPullFromBranch: () -> Unit,
        onPush: () -> Unit,
        onAddAll: () -> Unit,
        onStash: () -> Unit,
        onStashPop: () -> Unit,
        onHistory: () -> Unit,
        onRefresh: () -> Unit,
        onSshSetupClick: () -> Unit,
        sshKeysConfigured: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth().background(GitTheme.BgCard).border(0.dp, Color.Transparent)) {
        // Top Bar
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Branch Selector (Pill Shape)
            Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GitTheme.BgDark,
                    border = BorderStroke(1.dp, GitTheme.BorderColor),
                    modifier = Modifier.clickable(onClick = onBranchClick)
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                            Icons.Rounded.AccountTree,
                            null,
                            tint = GitTheme.AccentBlue,
                            modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                            modifier =
                                    Modifier.widthIn(max = 100.dp)
                                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                                branch,
                                color = GitTheme.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                            Icons.Rounded.ExpandMore,
                            null,
                            tint = GitTheme.TextTertiary,
                            modifier = Modifier.size(12.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ahead/Behind Indicator
                if (ahead > 0 || behind > 0) {
                    Row(
                            modifier =
                                    Modifier.background(GitTheme.BgDark, RoundedCornerShape(4.dp))
                                            .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (behind > 0) {
                            Icon(
                                    Icons.Rounded.ArrowDownward,
                                    null,
                                    tint = GitTheme.AccentBlue,
                                    modifier = Modifier.size(12.dp)
                            )
                            Text(
                                    "$behind",
                                    color = GitTheme.AccentBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 2.dp, end = 6.dp)
                            )
                        }
                        if (ahead > 0) {
                            Icon(
                                    Icons.Rounded.ArrowUpward,
                                    null,
                                    tint = GitTheme.AccentGreen,
                                    modifier = Modifier.size(12.dp)
                            )
                            Text(
                                    "$ahead",
                                    color = GitTheme.AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // SSH Setup Button
                Box(
                        modifier =
                                Modifier.padding(end = 4.dp)
                                        .background(
                                                if (!sshKeysConfigured)
                                                        GitTheme.AccentOrange.copy(alpha = 0.2f)
                                                else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                        )
                ) {
                    IconButton(onClick = onSshSetupClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                                Icons.Rounded.Key,
                                null,
                                tint = if (!sshKeysConfigured) GitTheme.AccentOrange else GitTheme.TextSecondary,
                                modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(
                            Icons.Rounded.Refresh,
                            null,
                            tint = GitTheme.TextSecondary,
                            modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = GitTheme.BorderColor)

        // Actions Toolbar
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Pull Dropdown
            var showPullMenu by remember { mutableStateOf(false) }
            Box {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                                Modifier.clip(RoundedCornerShape(8.dp))
                                        .clickable { showPullMenu = true }
                                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(
                            Icons.Rounded.ArrowDownward,
                            "Pull",
                            tint = GitTheme.TextSecondary,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                "Pull",
                                color = GitTheme.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                        )
                        Icon(
                                Icons.Rounded.ArrowDropDown,
                                null,
                                tint = GitTheme.TextTertiary,
                                modifier = Modifier.size(10.dp)
                        )
                    }
                }

                DropdownMenu(
                        expanded = showPullMenu,
                        onDismissRequest = { showPullMenu = false },
                        modifier =
                                Modifier.background(GitTheme.BgCard)
                                        .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(4.dp))
                ) {
                    DropdownMenuItem(
                            text = { Text("Pull (current branch)", color = GitTheme.TextPrimary) },
                            onClick = {
                                onPull()
                                showPullMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                        Icons.Rounded.ArrowDownward,
                                        null,
                                        tint = GitTheme.AccentBlue,
                                        modifier = Modifier.size(18.dp)
                                )
                            }
                    )
                    DropdownMenuItem(
                            text = { Text("Pull from branch...", color = GitTheme.TextPrimary) },
                            onClick = {
                                onPullFromBranch()
                                showPullMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                        Icons.Rounded.AccountTree,
                                        null,
                                        tint = GitTheme.AccentGreen,
                                        modifier = Modifier.size(18.dp)
                                )
                            }
                    )
                }
            }

            ActionButton(Icons.Rounded.ArrowUpward, "Push", onPush)
            ActionButton(Icons.Rounded.Sync, "Fetch", onFetch)

            // Add All button (hidden in portrait)
            if (isLandscape) {
                ActionButton(Icons.Rounded.Add, "Add All", onAddAll)
            }

            // Stash Dropdown
            var showStashMenu by remember { mutableStateOf(false) }
            Box {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                                Modifier.clip(RoundedCornerShape(8.dp))
                                        .clickable { showStashMenu = true }
                                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(
                            Icons.Rounded.Archive,
                            "Stash",
                            tint = GitTheme.TextSecondary,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                "Stash",
                                color = GitTheme.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                        )
                        Icon(
                                Icons.Rounded.ArrowDropDown,
                                null,
                                tint = GitTheme.TextTertiary,
                                modifier = Modifier.size(10.dp)
                        )
                    }
                }

                DropdownMenu(
                        expanded = showStashMenu,
                        onDismissRequest = { showStashMenu = false },
                        modifier =
                                Modifier.background(GitTheme.BgCard)
                                        .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(4.dp))
                ) {
                    DropdownMenuItem(
                            text = { Text("Stash Changes", color = GitTheme.TextPrimary) },
                            onClick = {
                                onStash()
                                showStashMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                        Icons.Rounded.Archive,
                                        null,
                                        tint = GitTheme.AccentBlue,
                                        modifier = Modifier.size(18.dp)
                                )
                            }
                    )
                    DropdownMenuItem(
                            text = { Text("Pop Stash", color = GitTheme.TextPrimary) },
                            onClick = {
                                onStashPop()
                                showStashMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                        Icons.Rounded.Unarchive,
                                        null,
                                        tint = GitTheme.AccentGreen,
                                        modifier = Modifier.size(18.dp)
                                )
                            }
                    )
                }
            }
            ActionButton(Icons.Rounded.History, "History", onHistory)
        }
        HorizontalDivider(color = GitTheme.BorderColor)
    }
}

@Composable
fun ActionButton(icon: ImageVector, tooltip: String, onClick: () -> Unit) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClick)
                            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Icon(icon, tooltip, tint = GitTheme.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(tooltip, color = GitTheme.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileChangeItem(
        path: String,
        status: String,
        isStaged: Boolean,
        isSelected: Boolean,
        onToggle: () -> Unit,
        onClick: () -> Unit
) {
    val file = File(path)
    val fileName = file.name
    val parentPath = file.parent ?: ""
    val iconPath = FileIconUtils.getIconPath(fileName)

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .background(if (isSelected) GitTheme.BgHover else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    colors =
                            CheckboxDefaults.colors(
                                    checkedColor = GitTheme.AccentBlue,
                                    uncheckedColor = GitTheme.TextTertiary,
                                    checkmarkColor = Color.White
                            ),
                    modifier = Modifier.scale(0.8f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        AsyncImage(
                model = iconPath,
                imageLoader = ImageLoaderSingleton.get(LocalContext.current),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isStaged) {
                    Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint = GitTheme.AccentBlue,
                            modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                        fileName,
                        color =
                                if (status == "U") GitTheme.AccentGreen
                                else if (status == "M") GitTheme.AccentYellow else GitTheme.TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isStaged) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (parentPath.isNotEmpty()) {
                Text(
                        parentPath,
                        color = GitTheme.TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status Badge
        Surface(
                color =
                        when (status) {
                            "U" -> GitTheme.AccentGreen.copy(alpha = 0.1f)
                            "M" -> GitTheme.AccentYellow.copy(alpha = 0.1f)
                            else -> GitTheme.AccentBlue.copy(alpha = 0.1f)
                        },
                shape = RoundedCornerShape(4.dp),
                border =
                        BorderStroke(
                                1.dp,
                                when (status) {
                                    "U" -> GitTheme.AccentGreen.copy(alpha = 0.3f)
                                    "M" -> GitTheme.AccentYellow.copy(alpha = 0.3f)
                                    else -> GitTheme.AccentBlue.copy(alpha = 0.3f)
                                }
                        )
        ) {
            Text(
                    when (status) {
                        "U" -> "A"
                        "M" -> "M"
                        else -> status
                    },
                    color =
                            when (status) {
                                "U" -> GitTheme.AccentGreen
                                "M" -> GitTheme.AccentYellow
                                else -> GitTheme.AccentBlue
                            },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ConsoleStatusBar(
        lastError: String?,
        lastSuccess: String?,
        operation: String?,
        isLoading: Boolean,
        showConsole: Boolean,
        logs: List<LogEntry>,
        terminalHistory: List<String>,
        onToggleConsole: () -> Unit,
        onClearMessages: () -> Unit,
        onAuthRequest: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Terminal, 1: Git Logs

    Column(modifier = Modifier.fillMaxWidth().background(GitTheme.BgCard)) {
        HorizontalDivider(color = GitTheme.BorderColor)

        // Status Bar
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(32.dp)
                                .background(GitTheme.BgDark)
                                .clickable(onClick = onToggleConsole)
                                .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            val rotation by animateFloatAsState(if (showConsole) 180f else 0f)
            Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    null,
                    tint = GitTheme.TextSecondary,
                    modifier = Modifier.size(16.dp).rotate(rotation)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (isLoading || operation != null) {
                CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.dp,
                        color = GitTheme.AccentBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(operation ?: "Working...", color = GitTheme.TextSecondary, fontSize = 11.sp)
            } else if (lastError != null) {
                Icon(Icons.Rounded.Error, null, tint = GitTheme.AccentRed, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        lastError,
                        color = GitTheme.AccentRed,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                )
                if (lastError.contains("Authentication")) {
                    Text(
                            "Fix",
                            color = GitTheme.AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onAuthRequest() }
                    )
                }
                IconButton(onClick = onClearMessages, modifier = Modifier.size(16.dp)) {
                    Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = GitTheme.TextTertiary,
                            modifier = Modifier.size(12.dp)
                    )
                }
            } else if (lastSuccess != null) {
                Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = GitTheme.AccentGreen,
                        modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        lastSuccess,
                        color = GitTheme.AccentGreen,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearMessages, modifier = Modifier.size(16.dp)) {
                    Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = GitTheme.TextTertiary,
                            modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Text("Console", color = GitTheme.TextTertiary, fontSize = 11.sp)
            }
        }

        // Console Content
        AnimatedVisibility(visible = showConsole) {
            Column(
                    modifier = Modifier.fillMaxWidth().height(300.dp).background(Color(0xFF121212))
            ) {
                // Tabs
                Row(
                        modifier = Modifier.fillMaxWidth().background(GitTheme.BgDark),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    ConsoleTab("Terminal", selectedTab == 0) { selectedTab = 0 }
                    ConsoleTab("Git Logs", selectedTab == 1) { selectedTab = 1 }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onClearMessages, modifier = Modifier.size(32.dp)) {
                        Icon(
                                Icons.Rounded.Delete,
                                null,
                                tint = GitTheme.TextTertiary,
                                modifier = Modifier.size(14.dp)
                        )
                    }
                }
                HorizontalDivider(color = GitTheme.BorderColor)

                SelectionContainer {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        if (selectedTab == 0) {
                            // Terminal View
                            LazyColumn(reverseLayout = true) {
                                items(terminalHistory.reversed()) { line ->
                                    val color =
                                            when {
                                                line.startsWith("$") -> GitTheme.AccentBlue
                                                line.startsWith("✅") -> GitTheme.AccentGreen
                                                line.startsWith("❌") -> GitTheme.AccentRed
                                                line.startsWith("🔑") ||
                                                        line.startsWith("🔐") ||
                                                        line.startsWith("📦") -> GitTheme.AccentYellow
                                                line.startsWith("📁") ||
                                                        line.startsWith("📂") ||
                                                        line.startsWith("📥") -> GitTheme.AccentBlue
                                                line.startsWith("⚠️") -> GitTheme.AccentOrange
                                                else -> GitTheme.TextSecondary
                                            }
                                    Text(
                                            line,
                                            color = color,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        } else {
                            // Git Logs View
                            LazyColumn(reverseLayout = false) {
                                items(logs.reversed()) { log ->
                                    val color =
                                            when (log.type) {
                                                LogType.COMMAND -> GitTheme.AccentBlue
                                                LogType.ERROR -> GitTheme.AccentRed
                                                LogType.SUCCESS -> GitTheme.AccentGreen
                                                LogType.INFO -> GitTheme.TextSecondary
                                            }
                                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                                log.timestamp,
                                                color = GitTheme.TextTertiary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.width(60.dp)
                                        )
                                        Text(
                                                "> ${log.message}",
                                                color = color,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
            modifier =
                    Modifier.clickable(onClick = onClick)
                            .background(if (isSelected) Color(0xFF121212) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
                title,
                color = if (isSelected) GitTheme.AccentBlue else GitTheme.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun StoragePermissionRequest(onRequestPermission: () -> Unit, onRefresh: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                    Icons.Rounded.FolderOff,
                    contentDescription = null,
                    tint = GitTheme.AccentOrange,
                    modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    "Storage Access Required",
                    color = GitTheme.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    "Git operations need access to read and write files. " +
                            "Please grant 'All files access' permission.",
                    color = GitTheme.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = GitTheme.AccentBlue),
                    shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("I've granted permission", color = GitTheme.AccentBlue)
            }
        }
    }
}

@Composable
fun EmptyState(onInit: () -> Unit, onClone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Source, null, tint = GitTheme.TextTertiary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    "No Git Repository",
                    color = GitTheme.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    "Initialize or clone a repository",
                    color = GitTheme.TextTertiary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Stack buttons vertically for sidebar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Initialize button - white bg, black text
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .clickable { onInit() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Initialize", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                // Clone button - white bg, black text
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .clickable { onClone() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CloudDownload, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clone", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// Helper for Modifier.scale
private fun Modifier.scale(scale: Float): Modifier =
        this.graphicsLayer(scaleX = scale, scaleY = scale)

