package com.bsbeetle.bananaspaghetti.ui.components

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.BranchDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.CommitHistoryDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.NewBranchDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.PullBranchDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.dialogs.MergeEditorDialog
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel
import com.bsbeetle.bananaspaghetti.utils.fileHandling.FileOperations
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// File menu action enum

enum class TopRightMode {
    NONE,
    GIT,
    FILE,
    MULTI_SELECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopRightBox(
        modifier: Modifier = Modifier,
        mode: TopRightMode = TopRightMode.NONE,
        gitViewModel: GitViewModel? = null,
        fileViewModel: FileViewModel? = null,
        bufferViewModel: BufferViewModel? = null,
        currentFilePath: String? = null,
        onCommitClick: (selectedFiles: List<String>) -> Unit = {},
        onPushClick: () -> Unit = {},
        onFileOpened: (DocumentFile) -> Unit = {},
        onFolderOpened: (DocumentFile) -> Unit = {}
) {
    // Use LocalFileViewModel directly (same as GitTab) for live updates
    val localFileViewModel: FileViewModel = LocalFileViewModel.current
    val context = LocalContext.current

    // Collect uiState from GitViewModel (same as portrait mode)
    val uiState by
            (gitViewModel?.uiState
                            ?: kotlinx.coroutines.flow.MutableStateFlow(
                                    com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitUiState()
                            ))
                    .collectAsState()

    var showBranchDialog by remember { mutableStateOf(false) }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showPullBranchDialog by remember { mutableStateOf(false) }

    // DEBUG: Log mode immediately when composable runs
    android.util.Log.d(
            "TopRightBox",
            "🎯 TopRightBox composable: mode=$mode, gitViewModel=${gitViewModel != null}"
    )
    android.util.Log.d(
            "TopRightBox",
            "📂 localFileViewModel.currentFilePath=${localFileViewModel.currentFilePath}"
    )
    android.util.Log.d(
            "TopRightBox",
            "📂 localFileViewModel.selectedFolderUri=${localFileViewModel.selectedFolderUri}"
    )

    // Initialize Git from LocalFileViewModel's currentFilePath (same as GitTab)
    LaunchedEffect(localFileViewModel.currentFilePath, mode) {
        android.util.Log.d(
                "TopRightBox",
                "⚡ LaunchedEffect TRIGGERED: mode=$mode, path=${localFileViewModel.currentFilePath}"
        )
        if (mode == TopRightMode.GIT) {
            val rawPath = localFileViewModel.currentFilePath
            android.util.Log.d("TopRightBox", "🔍 Git mode, currentFilePath: $rawPath")

            // Handle content:// URIs - extract real path (same logic as selectedFolderUri)
            val realPath =
                    if (rawPath != null && rawPath.startsWith("content://")) {
                        try {
                            val uri = android.net.Uri.parse(rawPath)
                            when {
                                uri.authority == "${context.packageName}.documents" -> {
                                    // Ubuntu internal storage
                                    val docId =
                                            android.provider.DocumentsContract.getDocumentId(uri)
                                    // docId format: /data/user/0/.../ubuntu/home/folder/file.ext
                                    // Get parent directory
                                    java.io.File(docId).parent
                                }
                                else -> null
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TopRightBox", "Error parsing URI: $e")
                            null
                        }
                    } else {
                        rawPath?.let { java.io.File(it).parent }
                    }

            android.util.Log.d(
                    "TopRightBox",
                    "📂 Extracted realPath from currentFilePath: $realPath"
            )

            if (realPath != null) {
                var dir = java.io.File(realPath)
                // Walk up to find .git
                while (dir.parentFile != null) {
                    if (java.io.File(dir, ".git").exists()) {
                        android.util.Log.d("TopRightBox", "✅ Found .git at: ${dir.absolutePath}")
                        gitViewModel?.setRootDir(dir.absolutePath)
                        break
                    }
                    dir = dir.parentFile!!
                }
            }
            // Initialize Git to start file watcher
            gitViewModel?.initializeGit()
            gitViewModel?.refreshStatus()
        }
    }

    // Also watch for folder selection from LocalFileViewModel (same as GitTab)
    LaunchedEffect(localFileViewModel.selectedFolderUri, mode) {
        if (mode == TopRightMode.GIT) {
            val folderUri = localFileViewModel.selectedFolderUri
            android.util.Log.d("TopRightBox", "🔍 Git mode, selectedFolderUri: $folderUri")
            if (folderUri != null) {
                val uriPath = folderUri.toString()
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
                            // Handle Ubuntu internal storage URIs (same as GitTab)
                            folderUri.authority == "${context.packageName}.documents" -> {
                                android.provider.DocumentsContract.getDocumentId(folderUri)
                            }
                            else -> null
                        }

                android.util.Log.d("TopRightBox", "📂 Extracted realPath: $realPath")

                if (realPath != null) {
                    val dir = java.io.File(realPath)
                    if (dir.exists() && java.io.File(dir, ".git").exists()) {
                        android.util.Log.d(
                                "TopRightBox",
                                "✅ Found .git in folder: ${dir.absolutePath}"
                        )
                        gitViewModel?.setRootDir(dir.absolutePath)
                    } else if (dir.exists()) {
                        var parentDir = dir.parentFile
                        var foundGit = false
                        while (parentDir != null) {
                            if (java.io.File(parentDir, ".git").exists()) {
                                android.util.Log.d(
                                        "TopRightBox",
                                        "✅ Found .git in parent: ${parentDir.absolutePath}"
                                )
                                gitViewModel?.setRootDir(parentDir.absolutePath)
                                foundGit = true
                                break
                            }
                            parentDir = parentDir.parentFile
                        }
                        if (!foundGit) {
                            android.util.Log.d(
                                    "TopRightBox",
                                    "⚠️ No .git found, using: ${dir.absolutePath}"
                            )
                            gitViewModel?.setRootDir(dir.absolutePath)
                        }
                    }
                }
            }
        }
    }

    // Continuous refresh every 2 seconds while Git panel is visible
    LaunchedEffect(mode) {
        if (mode == TopRightMode.GIT) {
            android.util.Log.d("TopRightBox", "🔄 Starting continuous refresh loop")
            gitViewModel?.refreshStatus()
            while (true) {
                delay(2000)
                android.util.Log.d(
                        "TopRightBox",
                        "🔄 Polling refresh... (staged=${gitViewModel?.uiState?.value?.stagedFiles?.size}, modified=${gitViewModel?.uiState?.value?.modifiedFiles?.size})"
                )
                gitViewModel?.refreshStatus()
            }
        }
    }

    // All changes combined (same logic as GitTab)
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

    // Initialize with all files selected
    LaunchedEffect(allChanges) {
        if (selectedFiles.isEmpty() && allChanges.isNotEmpty()) {
            selectedFiles = allChanges.toSet()
        } else {
            selectedFiles = selectedFiles.intersect(allChanges.toSet())
        }
    }

    // Pager state
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Branch Dialog
    if (showBranchDialog) {
        BranchDialog(
                branches = uiState.branches,
                onDismiss = { showBranchDialog = false },
                onCheckout = { name ->
                    gitViewModel?.checkoutBranch(name)
                    showBranchDialog = false
                },
                onDelete = { name, deleteRemote ->
                    gitViewModel?.deleteBranchWithRemote(name, deleteRemote)
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
                    gitViewModel?.createBranchFrom(name, sourceBranch)
                    showNewBranchDialog = false
                }
        )
    }

    // History Dialog
    if (showHistoryDialog) {
        CommitHistoryDialog(commits = uiState.commits, onDismiss = { showHistoryDialog = false })
    }

    // Pull Branch Dialog (same as portrait mode)
    if (showPullBranchDialog) {
        PullBranchDialog(
                remoteBranches = uiState.branches.filter { it.isRemote },
                onDismiss = { showPullBranchDialog = false },
                onSelect = { branchName: String ->
                    gitViewModel?.pullFromBranch(branchName)
                    showPullBranchDialog = false
                }
        )
    }

    // Merge Editor Dialog (for conflict resolution)
    if (uiState.showMergeEditor &&
                    uiState.mergeEditorFile != null &&
                    uiState.mergeEditorContent != null
    ) {
        MergeEditorDialog(
                filePath = uiState.mergeEditorFile!!,
                content = uiState.mergeEditorContent!!,
                onSave = { path, content -> gitViewModel?.saveMergedContent(path, content) },
                onDismiss = { gitViewModel?.closeMergeEditor() }
        )
    }

    ConstraintLayout(modifier = modifier.background(Color(0xFF050505))) {
        val (gridContent) = createRefs()

        // Show panel if mode is set OR if multi-select mode is active
        if (mode != TopRightMode.NONE || localFileViewModel.isMultiSelectMode) {
            Box(
                    modifier =
                            Modifier.constrainAs(gridContent) {
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                        top.linkTo(parent.top)
                                        bottom.linkTo(parent.bottom)
                                        width = Dimension.fillToConstraints
                                        height = Dimension.fillToConstraints
                                    }
                                    .padding(8.dp)
            ) {
                // Multi-select mode takes priority
                if (localFileViewModel.isMultiSelectMode) {
                    MultiSelectActionsPanel(
                        fileViewModel = localFileViewModel,
                        selectedFilePaths = localFileViewModel.selectedFilePaths,
                        onOperationComplete = {}
                    )
                } else if (mode == TopRightMode.GIT) {
                    // Check if Git is configured and SSH key exists
                    val prefs = com.bsbeetle.bananaspaghetti.utils.preferences.EditorPreferences(context)
                    val sshKeyExists = remember {
                        val ubuntuPath = "${context.filesDir.absolutePath}/ubuntu/home/.ssh/id_ed25519.pub"
                        java.io.File(ubuntuPath).exists()
                    }
                    if (!prefs.isGitConfigured() || !sshKeyExists) {
                        // Show setup message
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                if (!prefs.isGitConfigured()) 
                                    "Setup Git in Settings"
                                else 
                                    "Generate SSH key in Settings",
                                color = GitTheme.TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Branch Name (Clickable)
                        Surface(
                                color = GitTheme.BgDark,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, GitTheme.BorderColor),
                                modifier = Modifier.clickable { showBranchDialog = true }
                        ) {
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                        Icons.Rounded.AccountTree,
                                        null,
                                        tint = GitTheme.AccentBlue,
                                        modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                        uiState.currentBranch.ifEmpty { "master" },
                                        color = GitTheme.TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                        Icons.Rounded.ExpandMore,
                                        null,
                                        tint = GitTheme.TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Swipeable Pager
                        HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                        ) { page ->
                            when (page) {
                                0 -> {
                                    // Page 0: Git Action Icons Grid
                                    GitActionsGrid(
                                            uiState = uiState,
                                            gitViewModel = gitViewModel,
                                            onCommitClick = {
                                                onCommitClick(selectedFiles.toList())
                                            },
                                            onPushClick = onPushClick,
                                            onHistoryClick = { showHistoryDialog = true },
                                            onPullFromBranchClick = { showPullBranchDialog = true }
                                    )
                                }
                                1 -> {
                                    // Page 1: Changes List with Selection (shows staged separately)
                                    ChangesListPage(
                                            stagedFiles = uiState.stagedFiles,
                                            modifiedFiles = uiState.modifiedFiles,
                                            untrackedFiles = uiState.untrackedFiles,
                                            conflictedFiles = uiState.conflictedFiles,
                                            selectedFiles = selectedFiles,
                                            onToggleFile = { path ->
                                                selectedFiles =
                                                        if (selectedFiles.contains(path)) {
                                                            selectedFiles - path
                                                        } else {
                                                            selectedFiles + path
                                                        }
                                            },
                                            onSelectAll = {
                                                val all =
                                                        (uiState.stagedFiles +
                                                                        uiState.modifiedFiles +
                                                                        uiState.untrackedFiles +
                                                                        uiState.conflictedFiles)
                                                                .distinct()
                                                selectedFiles =
                                                        if (selectedFiles.size == all.size) {
                                                            emptySet()
                                                        } else {
                                                            all.toSet()
                                                        }
                                            },
                                            onFileClick = { path ->
                                                if (uiState.conflictedFiles.contains(path)) {
                                                    gitViewModel?.openMergeEditor(path)
                                                }
                                            }
                                    )
                                }
                            }
                        }

                        // Page Indicator Dots
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(2) { index ->
                                Box(
                                        modifier =
                                                Modifier.padding(horizontal = 3.dp)
                                                        .size(
                                                                if (pagerState.currentPage == index)
                                                                        6.dp
                                                                else 4.dp
                                                        )
                                                        .clip(CircleShape)
                                                        .background(
                                                                if (pagerState.currentPage == index)
                                                                        GitTheme.AccentBlue
                                                                else GitTheme.TextTertiary
                                                        )
                                )
                            }
                        }
                    }
                    } // end else isGitConfigured
                } else if (mode == TopRightMode.FILE) {
                    FileOptionsPanel(
                            fileViewModel = fileViewModel,
                            bufferViewModel = bufferViewModel,
                            onFileOpened = onFileOpened,
                            onFolderOpened = onFolderOpened
                    )
                }
            }
        }
    }
}


// File components (FileOptionsPanel, FileOptionItem, InlineInputForm) are now in FilePanelLandscape.kt
// Git components (GitActionsGrid, etc.) are in GitPanelLandscape.kt
