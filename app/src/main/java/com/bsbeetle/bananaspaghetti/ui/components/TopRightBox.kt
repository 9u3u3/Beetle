package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.directGit.ui.BranchDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.CommitHistoryDialog
import com.bsbeetle.bananaspaghetti.directGit.ui.FileChangeItem
import com.bsbeetle.bananaspaghetti.directGit.ui.NewBranchDialog
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel

// Theme colors (matching GitTab.kt)
private val BgDark = Color(0xFF1E1E1E)
private val BgCard = Color(0xFF252526)
private val BgHover = Color(0xFF2D2D2D)
private val AccentBlue = Color(0xFF007ACC)
private val AccentGreen = Color(0xFF73C991)
private val AccentOrange = Color(0xFFFF8C00)
private val TextPrimary = Color(0xFFE1E1E1)
private val TextSecondary = Color(0xFFAAAAAA)
private val TextTertiary = Color(0xFF666666)
private val BorderColor = Color(0xFF333333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopRightBox(
    modifier: Modifier = Modifier,
    isVisible: Boolean = false,
    gitViewModel: GitViewModel? = null,
    currentFilePath: String? = null,
    selectedFolderUri: android.net.Uri? = null,
    onCommitClick: (selectedFiles: List<String>) -> Unit = {},
    onPushClick: () -> Unit = {}
) {
    // Collect uiState from GitViewModel (same as portrait mode)
    val uiState by (gitViewModel?.uiState ?: kotlinx.coroutines.flow.MutableStateFlow(
        com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitUiState()
    )).collectAsState()
    
    var showBranchDialog by remember { mutableStateOf(false) }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    
    // Debug: Log uiState changes
    android.util.Log.d("TopRightBox", "📊 uiState: staged=${uiState.stagedFiles.size}, modified=${uiState.modifiedFiles.size}, untracked=${uiState.untrackedFiles.size}, branch=${uiState.currentBranch}")
    
    // Initialize Git root directory from file path (same as portrait mode)
    LaunchedEffect(currentFilePath) {
        android.util.Log.d("TopRightBox", "🔍 LaunchedEffect(currentFilePath): $currentFilePath")
        if (currentFilePath != null) {
            var dir = java.io.File(currentFilePath).parentFile
            while (dir != null) {
                if (java.io.File(dir, ".git").exists()) {
                    android.util.Log.d("TopRightBox", "✅ Found .git at: ${dir.absolutePath}")
                    gitViewModel?.setRootDir(dir.absolutePath)
                    break
                }
                dir = dir.parentFile
            }
            if (dir == null && currentFilePath.isNotEmpty()) {
                val fallbackDir = java.io.File(currentFilePath).parentFile?.absolutePath ?: ""
                android.util.Log.d("TopRightBox", "⚠️ No .git found, using fallback: $fallbackDir")
                gitViewModel?.setRootDir(fallbackDir)
            }
        } else {
            android.util.Log.d("TopRightBox", "⚠️ currentFilePath is null")
        }
    }
    
    // Also watch for folder selection (same as portrait mode)
    LaunchedEffect(selectedFolderUri) {
        android.util.Log.d("TopRightBox", "🔍 LaunchedEffect(selectedFolderUri): $selectedFolderUri")
        if (selectedFolderUri != null) {
            val uriPath = selectedFolderUri.toString()
            val realPath = when {
                uriPath.contains("primary:") -> {
                    val pathPart = uriPath.substringAfter("primary:")
                        .substringBefore("/document/")
                        .replace("%2F", "/")
                        .replace("%3A", ":")
                    "/storage/emulated/0/$pathPart"
                }
                uriPath.contains("primary%3A") -> {
                    val pathPart = uriPath.substringAfter("primary%3A")
                        .substringBefore("/document/")
                        .replace("%2F", "/")
                        .replace("%3A", ":")
                    "/storage/emulated/0/$pathPart"
                }
                else -> null
            }
            
            android.util.Log.d("TopRightBox", "📂 Extracted path from URI: $realPath")
            
            if (realPath != null) {
                val dir = java.io.File(realPath)
                if (dir.exists() && java.io.File(dir, ".git").exists()) {
                    android.util.Log.d("TopRightBox", "✅ Found .git in folder: ${dir.absolutePath}")
                    gitViewModel?.setRootDir(dir.absolutePath)
                } else if (dir.exists()) {
                    var parentDir = dir.parentFile
                    var foundGit = false
                    while (parentDir != null) {
                        if (java.io.File(parentDir, ".git").exists()) {
                            android.util.Log.d("TopRightBox", "✅ Found .git in parent: ${parentDir.absolutePath}")
                            gitViewModel?.setRootDir(parentDir.absolutePath)
                            foundGit = true
                            break
                        }
                        parentDir = parentDir.parentFile
                    }
                    if (!foundGit) {
                        android.util.Log.d("TopRightBox", "⚠️ No .git found, using dir: ${dir.absolutePath}")
                        gitViewModel?.setRootDir(dir.absolutePath)
                    }
                }
            }
        } else {
            android.util.Log.d("TopRightBox", "⚠️ selectedFolderUri is null")
        }
    }
    
    // Auto-refresh when panel becomes visible
    LaunchedEffect(isVisible) {
        android.util.Log.d("TopRightBox", "👁️ LaunchedEffect(isVisible): $isVisible, gitViewModel=${gitViewModel != null}")
        if (isVisible) {
            android.util.Log.d("TopRightBox", "🔄 Calling refreshStatus()")
            gitViewModel?.refreshStatus()
        }
    }
    
    // All changes combined
    val allChanges = remember(uiState.stagedFiles, uiState.modifiedFiles, uiState.untrackedFiles) {
        (uiState.stagedFiles + uiState.modifiedFiles + uiState.untrackedFiles)
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
        CommitHistoryDialog(
            commits = uiState.commits,
            onDismiss = { showHistoryDialog = false }
        )
    }

    ConstraintLayout(
        modifier = modifier.background(Color(0xFF050505))
    ) {
        val (gridContent) = createRefs()

        if (isVisible) {
            Column(
                modifier = Modifier
                    .constrainAs(gridContent) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Branch Name (Clickable)
                Surface(
                    color = BgDark,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.clickable { showBranchDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AccountTree,
                            null,
                            tint = AccentBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            uiState.currentBranch.ifEmpty { "master" },
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.ExpandMore,
                            null,
                            tint = TextSecondary,
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
                                onCommitClick = { onCommitClick(selectedFiles.toList()) },
                                onPushClick = onPushClick,
                                onHistoryClick = { showHistoryDialog = true }
                            )
                        }
                        1 -> {
                            // Page 1: Changes List with Selection (shows staged separately)
                            ChangesListPage(
                                stagedFiles = uiState.stagedFiles,
                                modifiedFiles = uiState.modifiedFiles,
                                untrackedFiles = uiState.untrackedFiles,
                                selectedFiles = selectedFiles,
                                onToggleFile = { path ->
                                    selectedFiles = if (selectedFiles.contains(path)) {
                                        selectedFiles - path
                                    } else {
                                        selectedFiles + path
                                    }
                                },
                                onSelectAll = {
                                    val all = (uiState.stagedFiles + uiState.modifiedFiles + uiState.untrackedFiles).distinct()
                                    selectedFiles = if (selectedFiles.size == all.size) {
                                        emptySet()
                                    } else {
                                        all.toSet()
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
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == index) 6.dp else 4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) AccentBlue
                                    else TextTertiary
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GitActionsGrid(
    uiState: com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitUiState,
    gitViewModel: GitViewModel?,
    onCommitClick: () -> Unit,
    onPushClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Pull, Push, Add All
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GitActionIcon(
                icon = Icons.Rounded.ArrowDownward,
                label = "Pull",
                color = AccentBlue,
                onClick = { gitViewModel?.pull() },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GitActionIcon(
                icon = Icons.Rounded.ArrowUpward,
                label = "Push",
                color = AccentGreen,
                onClick = onPushClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GitActionIcon(
                icon = Icons.Rounded.Add,
                label = "Add",
                color = TextSecondary,
                onClick = {
                    val allFiles = uiState.modifiedFiles + uiState.untrackedFiles
                    allFiles.forEach { gitViewModel?.stageFile(it) }
                },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 2: Fetch, Stash, History
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GitActionIcon(
                icon = Icons.Rounded.Sync,
                label = "Fetch",
                color = TextSecondary,
                onClick = { gitViewModel?.fetch() },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GitActionIcon(
                icon = Icons.Rounded.Archive,
                label = "Stash",
                color = TextSecondary,
                onClick = { gitViewModel?.stash() },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GitActionIcon(
                icon = Icons.Rounded.History,
                label = "History",
                color = TextSecondary,
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 3: Commit and Push buttons (separate operations)
        Row(
            Modifier.fillMaxWidth().weight(1.2f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onCommitClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Commit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onPushClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen.copy(alpha = 0.8f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Push", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangesListPage(
    stagedFiles: List<String>,
    modifiedFiles: List<String>,
    untrackedFiles: List<String>,
    selectedFiles: Set<String>,
    onToggleFile: (String) -> Unit,
    onSelectAll: () -> Unit
) {
    val allChanges = (stagedFiles + modifiedFiles + untrackedFiles).distinct().sorted()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Select All Header (same as portrait)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectAll)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Checkbox(
                    checked = selectedFiles.size == allChanges.size && allChanges.isNotEmpty(),
                    onCheckedChange = { onSelectAll() },
                    colors = CheckboxDefaults.colors(
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

        // Changes List (same as portrait with FileChangeItem)
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (allChanges.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No changes detected", color = TextTertiary, fontSize = 13.sp)
                    }
                }
            }

            items(allChanges) { path ->
                val isStaged = stagedFiles.contains(path)
                val status = when {
                    untrackedFiles.contains(path) -> "U"
                    modifiedFiles.contains(path) -> "M"
                    else -> "S"
                }
                FileChangeItem(
                    path = path,
                    status = status,
                    isStaged = isStaged,
                    isSelected = selectedFiles.contains(path),
                    onToggle = { onToggleFile(path) },
                    onClick = { /* No diff view in landscape */ }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GitActionIcon(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(BgCard)
                .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showTooltip = true }
                )
                .padding(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                color = TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Tooltip on long press
        if (showTooltip) {
            Popup(
                alignment = Alignment.TopCenter,
                onDismissRequest = { showTooltip = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
