package com.bsbeetle.bananaspaghetti.directGit.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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

// --- Theme Colors ---
private val BgDark = Color(0xFF1E1E1E)
private val BgCard = Color(0xFF252526)
private val BgHover = Color(0xFF2D2D2D)
private val AccentBlue = Color(0xFF007ACC)
private val AccentGreen = Color(0xFF73C991)
private val AccentOrange = Color(0xFFFF8C00)
private val AccentRed = Color(0xFFF48771)
private val AccentYellow = Color(0xFFE2C08D)
private val TextPrimary = Color(0xFFE1E1E1)
private val TextSecondary = Color(0xFFAAAAAA)
private val TextTertiary = Color(0xFF666666)
private val BorderColor = Color(0xFF333333)

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

    // Show SSH setup dialog if keys are not configured (only once per session)
    var hasShownSshDialog by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.sshKeysConfigured) {
        if (!uiState.sshKeysConfigured && !showSshSetupDialog && !hasShownSshDialog) {
            showSshSetupDialog = true
            hasShownSshDialog = true
        }
    }

    // Action to retry after auth
    var authAction by remember { mutableStateOf<((String, String, Boolean) -> Unit)?>(null) }

    // Unified changes list
    val allChanges =
            remember(uiState.stagedFiles, uiState.modifiedFiles, uiState.untrackedFiles) {
                (uiState.stagedFiles + uiState.modifiedFiles + uiState.untrackedFiles)
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

    Column(modifier = Modifier.fillMaxSize().background(BgDark)) {
        // --- Header ---
        // === Git Header ===
        GitHeader(
                branch = uiState.currentBranch.ifEmpty { "master" },
                ahead = uiState.aheadCount,
                behind = uiState.behindCount,
                onBranchClick = { showBranchDialog = true },
                onFetch = { viewModel.fetch() },
                onPull = { viewModel.pull() },
                onPush = { viewModel.push() },
                onStash = { viewModel.stash() },
                onStashPop = { viewModel.stashPop() },
                onHistory = { showHistoryDialog = true },
                onRefresh = { viewModel.refreshStatus() },
                onSshSetupClick = { showSshSetupDialog = true },
                sshKeysConfigured = uiState.sshKeysConfigured
        )

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
                                                            checkedColor = AccentBlue,
                                                            uncheckedColor = TextTertiary,
                                                            checkmarkColor = Color.White
                                                    ),
                                            modifier = Modifier.scale(0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        "CHANGES (${allChanges.size})",
                                        color = TextSecondary,
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
                                            color = TextTertiary,
                                            fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        items(allChanges) { path ->
                            val isStaged = uiState.stagedFiles.contains(path)
                            val status =
                                    when {
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
                                    onClick = { viewModel.loadDiff(path) }
                            )
                        }
                    }

                    // Commit Section
                    Column(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .background(BgCard)
                                            .border(
                                                    1.dp,
                                                    BorderColor,
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
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                        ),
                                cursorBrush = SolidColor(AccentBlue),
                                decorationBox = { innerTextField ->
                                    Box(
                                            modifier =
                                                    Modifier.fillMaxWidth()
                                                            .height(80.dp)
                                                            .background(
                                                                    BgDark,
                                                                    RoundedCornerShape(6.dp)
                                                            )
                                                            .border(
                                                                    1.dp,
                                                                    BorderColor,
                                                                    RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(8.dp)
                                    ) {
                                        if (commitMessage.isEmpty()) {
                                            Text(
                                                    "Commit message...",
                                                    color = TextTertiary,
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
                                                    containerColor = AccentBlue,
                                                    disabledContainerColor =
                                                            AccentBlue.copy(alpha = 0.5f)
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
                                                    containerColor = BgHover,
                                                    contentColor = TextPrimary,
                                                    disabledContainerColor =
                                                            BgHover.copy(alpha = 0.5f)
                                            ),
                                    border = BorderStroke(1.dp, BorderColor),
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

        // --- Console / Status Bar (includes terminal commands) ---
        ConsoleStatusBar(
                lastError = uiState.lastError,
                lastSuccess = uiState.lastSuccess,
                operation = uiState.operationInProgress,
                isLoading = uiState.isLoading,
                showConsole = showConsole,
                logs = uiState.logs,
                terminalHistory = uiState.terminalHistory,
                onToggleConsole = { showConsole = !showConsole },
                onClearMessages = { viewModel.clearMessages() },
                onAuthRequest = { showAuthDialog = true }
        )
    }
}

// --- Components ---

@Composable
fun GitHeader(
        branch: String,
        ahead: Int,
        behind: Int,
        onBranchClick: () -> Unit,
        onFetch: () -> Unit,
        onPull: () -> Unit,
        onPush: () -> Unit,
        onStash: () -> Unit,
        onStashPop: () -> Unit,
        onHistory: () -> Unit,
        onRefresh: () -> Unit,
        onSshSetupClick: () -> Unit,
        sshKeysConfigured: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth().background(BgCard).border(0.dp, Color.Transparent)) {
        // Top Bar
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Branch Selector (Pill Shape)
            Surface(
                    color = BgDark,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.clickable(onClick = onBranchClick)
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                            Icons.Rounded.AccountTree,
                            null,
                            tint = AccentBlue,
                            modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            branch,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                            Icons.Rounded.ExpandMore,
                            null,
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ahead/Behind Indicator
                if (ahead > 0 || behind > 0) {
                    Row(
                            modifier =
                                    Modifier.background(BgDark, RoundedCornerShape(4.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (behind > 0) {
                            Icon(
                                    Icons.Rounded.ArrowDownward,
                                    null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(12.dp)
                            )
                            Text(
                                    "$behind",
                                    color = AccentBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 2.dp, end = 6.dp)
                            )
                        }
                        if (ahead > 0) {
                            Icon(
                                    Icons.Rounded.ArrowUpward,
                                    null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(12.dp)
                            )
                            Text(
                                    "$ahead",
                                    color = AccentGreen,
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
                                                        AccentOrange.copy(alpha = 0.2f)
                                                else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                        )
                ) {
                    IconButton(onClick = onSshSetupClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                                Icons.Rounded.Key,
                                null,
                                tint = if (!sshKeysConfigured) AccentOrange else TextSecondary,
                                modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(
                            Icons.Rounded.Refresh,
                            null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = BorderColor)

        // Actions Toolbar
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(Icons.Rounded.ArrowDownward, "Pull", onPull)
            ActionButton(Icons.Rounded.ArrowUpward, "Push", onPush)
            ActionButton(Icons.Rounded.Sync, "Fetch", onFetch)

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
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                "Stash",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                        )
                        Icon(
                                Icons.Rounded.ArrowDropDown,
                                null,
                                tint = TextTertiary,
                                modifier = Modifier.size(10.dp)
                        )
                    }
                }

                DropdownMenu(
                        expanded = showStashMenu,
                        onDismissRequest = { showStashMenu = false },
                        modifier =
                                Modifier.background(BgCard)
                                        .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                ) {
                    DropdownMenuItem(
                            text = { Text("Stash Changes", color = TextPrimary) },
                            onClick = {
                                onStash()
                                showStashMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                        Icons.Rounded.Archive,
                                        null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(18.dp)
                                )
                            }
                    )
                    DropdownMenuItem(
                            text = { Text("Pop Stash", color = TextPrimary) },
                            onClick = {
                                onStashPop()
                                showStashMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                        Icons.Rounded.Unarchive,
                                        null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(18.dp)
                                )
                            }
                    )
                }
            }
            ActionButton(Icons.Rounded.History, "History", onHistory)
        }
        HorizontalDivider(color = BorderColor)
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
        Icon(icon, tooltip, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(tooltip, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                            .background(if (isSelected) BgHover else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    colors =
                            CheckboxDefaults.colors(
                                    checkedColor = AccentBlue,
                                    uncheckedColor = TextTertiary,
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
                            tint = AccentBlue,
                            modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                        fileName,
                        color =
                                if (status == "U") AccentGreen
                                else if (status == "M") AccentYellow else TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isStaged) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (parentPath.isNotEmpty()) {
                Text(
                        parentPath,
                        color = TextTertiary,
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
                            "U" -> AccentGreen.copy(alpha = 0.1f)
                            "M" -> AccentYellow.copy(alpha = 0.1f)
                            else -> AccentBlue.copy(alpha = 0.1f)
                        },
                shape = RoundedCornerShape(4.dp),
                border =
                        BorderStroke(
                                1.dp,
                                when (status) {
                                    "U" -> AccentGreen.copy(alpha = 0.3f)
                                    "M" -> AccentYellow.copy(alpha = 0.3f)
                                    else -> AccentBlue.copy(alpha = 0.3f)
                                }
                        )
        ) {
            Text(
                    when (status) {
                        "U" -> "Added"
                        "M" -> "Modified"
                        else -> status
                    },
                    color =
                            when (status) {
                                "U" -> AccentGreen
                                "M" -> AccentYellow
                                else -> AccentBlue
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

    Column(modifier = Modifier.fillMaxWidth().background(BgCard)) {
        HorizontalDivider(color = BorderColor)

        // Status Bar
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(32.dp)
                                .background(BgDark)
                                .clickable(onClick = onToggleConsole)
                                .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            val rotation by animateFloatAsState(if (showConsole) 180f else 0f)
            Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp).rotate(rotation)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (isLoading || operation != null) {
                CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.dp,
                        color = AccentBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(operation ?: "Working...", color = TextSecondary, fontSize = 11.sp)
            } else if (lastError != null) {
                Icon(Icons.Rounded.Error, null, tint = AccentRed, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        lastError,
                        color = AccentRed,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                )
                if (lastError.contains("Authentication")) {
                    Text(
                            "Fix",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onAuthRequest() }
                    )
                }
                IconButton(onClick = onClearMessages, modifier = Modifier.size(16.dp)) {
                    Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = TextTertiary,
                            modifier = Modifier.size(12.dp)
                    )
                }
            } else if (lastSuccess != null) {
                Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = AccentGreen,
                        modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        lastSuccess,
                        color = AccentGreen,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearMessages, modifier = Modifier.size(16.dp)) {
                    Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = TextTertiary,
                            modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Text("Console", color = TextTertiary, fontSize = 11.sp)
            }
        }

        // Console Content
        AnimatedVisibility(visible = showConsole) {
            Column(
                    modifier = Modifier.fillMaxWidth().height(300.dp).background(Color(0xFF121212))
            ) {
                // Tabs
                Row(
                        modifier = Modifier.fillMaxWidth().background(BgDark),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    ConsoleTab("Terminal", selectedTab == 0) { selectedTab = 0 }
                    ConsoleTab("Git Logs", selectedTab == 1) { selectedTab = 1 }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onClearMessages, modifier = Modifier.size(32.dp)) {
                        Icon(
                                Icons.Rounded.Delete,
                                null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                        )
                    }
                }
                HorizontalDivider(color = BorderColor)

                SelectionContainer {
                    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        if (selectedTab == 0) {
                            // Terminal View
                            LazyColumn(reverseLayout = true) {
                                items(terminalHistory.reversed()) { line ->
                                    val color =
                                            when {
                                                line.startsWith("$") -> AccentBlue
                                                line.startsWith("✅") -> AccentGreen
                                                line.startsWith("❌") -> AccentRed
                                                line.startsWith("🔑") ||
                                                        line.startsWith("🔐") ||
                                                        line.startsWith("📦") -> AccentYellow
                                                line.startsWith("📁") ||
                                                        line.startsWith("📂") ||
                                                        line.startsWith("📥") -> AccentBlue
                                                line.startsWith("⚠️") -> AccentOrange
                                                else -> TextSecondary
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
                                                LogType.COMMAND -> AccentBlue
                                                LogType.ERROR -> AccentRed
                                                LogType.SUCCESS -> AccentGreen
                                                LogType.INFO -> TextSecondary
                                            }
                                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                                log.timestamp,
                                                color = TextTertiary,
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
                color = if (isSelected) AccentBlue else TextSecondary,
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
                    tint = AccentOrange,
                    modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    "Storage Access Required",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    "Git operations need access to read and write files. " +
                            "Please grant 'All files access' permission.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
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
                Text("I've granted permission", color = AccentBlue)
            }
        }
    }
}

@Composable
fun EmptyState(onInit: () -> Unit, onClone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Source, null, tint = TextTertiary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    "No Git Repository Found",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    "Initialize a new repository or clone an existing one.",
                    color = TextTertiary,
                    fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                        onClick = onInit,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Initialize Repository")
                }
                Button(
                        onClick = onClone,
                        colors = ButtonDefaults.buttonColors(containerColor = BgHover),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clone Repository")
                }
            }
        }
    }
}

// --- Dialogs (Styled) ---

@Composable
private fun StyledDialog(
        title: String,
        onDismiss: () -> Unit,
        content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp),
                modifier =
                        Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                            title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                                Icons.Rounded.Close,
                                null,
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun BranchDialog(
        branches: List<BranchInfo>,
        onDismiss: () -> Unit,
        onCheckout: (String) -> Unit,
        onDelete: (String, Boolean) -> Unit,
        onNewBranch: () -> Unit
) {
    StyledDialog(title = "Branches", onDismiss = onDismiss) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onNewBranch) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Branch")
            }
        }

        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
            val local = branches.filter { !it.isRemote }
            val remote = branches.filter { it.isRemote }

            if (local.isNotEmpty()) {
                item {
                    Text(
                            "LOCAL",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(local) { b -> BranchItem(b, onCheckout, onDelete) }
            }
            if (remote.isNotEmpty()) {
                item {
                    Text(
                            "REMOTE",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(remote) { b -> BranchItem(b, onCheckout, onDelete) }
            }
        }
    }
}

@Composable
private fun BranchItem(
        branch: BranchInfo,
        onCheckout: (String) -> Unit,
        onDelete: (String, Boolean) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteFromRemote by remember { mutableStateOf(false) }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = BgCard,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = { Text("Delete Branch") },
                text = {
                    Column {
                        Text("Delete branch '${branch.name}'?")
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                    checked = deleteFromRemote,
                                    onCheckedChange = { deleteFromRemote = it },
                                    colors =
                                            CheckboxDefaults.colors(
                                                    checkedColor = AccentRed,
                                                    uncheckedColor = TextTertiary
                                            )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Also delete from remote", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                onDelete(branch.name, deleteFromRemote)
                                showDeleteDialog = false
                                deleteFromRemote = false
                            }
                    ) { Text("Delete", color = AccentRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = TextTertiary)
                    }
                }
        )
    }

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable { onCheckout(branch.name) }
                            .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                if (branch.isCurrent) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
                null,
                tint = if (branch.isCurrent) AccentGreen else TextTertiary,
                modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
                branch.name,
                color = if (branch.isCurrent) AccentGreen else TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
        )

        if (!branch.isCurrent && !branch.isRemote) {
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                Icon(
                        Icons.Rounded.Delete,
                        null,
                        tint = AccentRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBranchDialog(
        branches: List<BranchInfo>,
        currentBranch: String,
        onDismiss: () -> Unit,
        onCreate: (name: String, sourceBranch: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sourceBranch by remember { mutableStateOf(currentBranch) }
    var expanded by remember { mutableStateOf(false) }

    StyledDialog(title = "Create Branch", onDismiss = onDismiss) {
        // Branch Name
        OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Branch Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors =
                        OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentBlue,
                                unfocusedLabelColor = TextTertiary,
                                cursorColor = AccentBlue
                        )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Source Branch Dropdown
        Text("Create from:", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                    value = sourceBranch,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = BorderColor,
                                    cursorColor = AccentBlue
                            )
            )

            ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(BgCard)
            ) {
                branches.forEach { branch ->
                    DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (branch.isCurrent) {
                                        Icon(
                                                Icons.Rounded.Check,
                                                null,
                                                tint = AccentGreen,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(branch.name, color = TextPrimary)
                                }
                            },
                            onClick = {
                                sourceBranch = branch.name
                                expanded = false
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
                onClick = { onCreate(name, sourceBranch) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(6.dp)
        ) { Text("Create Branch") }
    }
}

@Composable
fun AuthDialog(onDismiss: () -> Unit, onConfirm: (String, String, Boolean) -> Unit) {
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var save by remember { mutableStateOf(true) }

    StyledDialog(title = "Authentication Required", onDismiss = onDismiss) {
        Text("Enter your GitHub credentials to continue.", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors =
                        OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentBlue,
                                unfocusedLabelColor = TextTertiary
                        )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Personal Access Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors =
                        OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentBlue,
                                unfocusedLabelColor = TextTertiary
                        )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                    checked = save,
                    onCheckedChange = { save = it },
                    colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
            )
            Text("Save credentials securely", color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
                onClick = { onConfirm(username, token, save) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(6.dp)
        ) { Text("Authenticate") }
    }
}

@Composable
fun RemoteDialog(currentUrl: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by remember { mutableStateOf(currentUrl) }
    StyledDialog(title = "Remote Settings", onDismiss = onDismiss) {
        OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Remote URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors =
                        OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentBlue,
                                unfocusedLabelColor = TextTertiary
                        )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
                onClick = { onConfirm(url) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(6.dp)
        ) { Text("Save Remote") }
    }
}

@Composable
fun CloneDialog(onDismiss: () -> Unit, onClone: (String, String) -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var targetPath by remember { mutableStateOf(File(context.filesDir, "alpine/home/").absolutePath + "/") }

    StyledDialog(title = "Clone Repository", onDismiss = onDismiss) {
        Text(
                "Clone a Git repository using SSH authentication.",
                color = TextSecondary,
                fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = url,
                onValueChange = { newUrl ->
                    url = newUrl
                    // Auto-update target path if it's still the default or empty
                    if (newUrl.isNotBlank()) {
                        val repoName = newUrl.substringAfterLast("/").removeSuffix(".git")
                        if (repoName.isNotBlank()) {
                            targetPath = File(context.filesDir, "alpine/home/$repoName").absolutePath
                        }
                    }
                },
                label = { Text("Repository URL") },
                placeholder = { Text("git@github.com:user/repo.git") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors =
                        OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentBlue,
                                unfocusedLabelColor = TextTertiary
                        )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
                value = targetPath,
                onValueChange = { targetPath = it },
                label = { Text("Clone To (Full Path)") },
                placeholder = { Text(File(context.filesDir, "alpine/home/my-project").absolutePath) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors =
                        OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentBlue,
                                unfocusedLabelColor = TextTertiary
                        )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
                "Tip: Use internal storage for better compatibility with Alpine Linux",
                color = TextTertiary,
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SSH Info
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Info, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    "Using SSH authentication - ensure your SSH keys are configured",
                    color = TextSecondary,
                    fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
                onClick = { onClone(url, targetPath) },
                enabled = url.isNotBlank() && targetPath.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clone Repository")
        }
    }
}

@Composable
fun CommitHistoryDialog(commits: List<CommitInfo>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
                colors = CardDefaults.cardColors(containerColor = BgDark),
                modifier =
                        Modifier.fillMaxWidth()
                                .fillMaxHeight(0.8f)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
        ) {
            Column {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            "Commit History",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = TextTertiary)
                    }
                }
                HorizontalDivider(color = BorderColor)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(commits) { commit ->
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                        commit.shortHash,
                                        color = AccentBlue,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        commit.message,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                    "${commit.author} • ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(commit.date)}",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                            )
                        }
                        HorizontalDivider(color = BorderColor)
                    }
                }
            }
        }
    }
}

@Composable
fun DiffDialog(diffText: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
                colors = CardDefaults.cardColors(containerColor = BgDark),
                modifier =
                        Modifier.fillMaxSize()
                                .padding(16.dp)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
        ) {
            Column {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            "File Diff",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = TextTertiary)
                    }
                }
                HorizontalDivider(color = BorderColor)
                Box(
                        modifier =
                                Modifier.weight(1f)
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                            diffText,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// Helper for Modifier.scale
private fun Modifier.scale(scale: Float): Modifier =
        this.graphicsLayer(scaleX = scale, scaleY = scale)
