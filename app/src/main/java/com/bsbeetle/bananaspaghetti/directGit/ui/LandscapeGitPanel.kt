package com.bsbeetle.bananaspaghetti.directGit.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import java.io.File

// --- Theme Colors (Matching GitTab) ---
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
fun LandscapeGitPanel(viewModel: GitViewModel = viewModel()) {
    val context = LocalContext.current
    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val uiState by viewModel.uiState.collectAsState()
    var commitMessage by remember { mutableStateOf("") }

    // Dialog States (Reused from GitTab logic)
    var showAuthDialog by remember { mutableStateOf(false) }
    var showRemoteDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showCloneDialog by remember { mutableStateOf(false) }
    var showSshSetupDialog by remember { mutableStateOf(false) }

    // Unified changes list
    val allChanges = remember(uiState.stagedFiles, uiState.modifiedFiles, uiState.untrackedFiles) {
        (uiState.stagedFiles + uiState.modifiedFiles + uiState.untrackedFiles).distinct().sorted()
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

    // === Dialogs (Same as GitTab) ===
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
            onConfirm = { u, t, s -> /* Handle auth if needed, usually handled by ViewModel callback */ }
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
        // --- Compact Header ---
        LandscapeGitHeader(
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
            onRefresh = { viewModel.refreshStatus() }
        )

        HorizontalDivider(color = BorderColor)

        // --- Changes List ---
        Box(modifier = Modifier.weight(1f)) {
             if (!uiState.isInitialized) {
                EmptyState(onInit = { viewModel.initRepo() }, onClone = { showCloneDialog = true })
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Select All Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFiles = if (selectedFiles.size == allChanges.size) emptySet() else allChanges.toSet()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                    Checkbox(
                                        checked = selectedFiles.size == allChanges.size && allChanges.isNotEmpty(),
                                        onCheckedChange = { checked ->
                                            selectedFiles = if (checked) allChanges.toSet() else emptySet()
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AccentBlue,
                                            uncheckedColor = TextTertiary,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.scale(0.7f).size(24.dp)
                                    )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "CHANGES (${allChanges.size})",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (allChanges.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No changes", color = TextTertiary, fontSize = 12.sp)
                                }
                            }
                        }

                        items(allChanges) { path ->
                            val isStaged = uiState.stagedFiles.contains(path)
                            val status = when {
                                uiState.untrackedFiles.contains(path) -> "U"
                                uiState.modifiedFiles.contains(path) -> "M"
                                else -> "S"
                            }
                            // Reusing FileChangeItem from GitTab (must be public now)
                            FileChangeItem(
                                path = path,
                                status = status,
                                isStaged = isStaged,
                                isSelected = selectedFiles.contains(path),
                                onToggle = {
                                    selectedFiles = if (selectedFiles.contains(path)) selectedFiles - path else selectedFiles + path
                                },
                                onClick = { viewModel.loadDiff(path) }
                            )
                        }
                    }

                    // --- Compact Commit Section ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BgCard)
                            .border(1.dp, BorderColor, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = commitMessage,
                            onValueChange = { commitMessage = it },
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(AccentBlue),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(BgDark, RoundedCornerShape(4.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    if (commitMessage.isEmpty()) {
                                        Text("Message...", color = TextTertiary, fontSize = 12.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    if (commitMessage.isNotBlank()) {
                                        viewModel.doCommit(commitMessage, selectedFiles.toList())
                                        commitMessage = ""
                                    }
                                },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(0.dp),
                                enabled = selectedFiles.isNotEmpty() && commitMessage.isNotBlank()
                            ) {
                                Text("Commit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (commitMessage.isNotBlank()) {
                                        viewModel.commitAndPush(commitMessage, selectedFiles.toList())
                                        commitMessage = ""
                                    }
                                },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BgHover,
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, BorderColor),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(0.dp),
                                enabled = selectedFiles.isNotEmpty() && commitMessage.isNotBlank()
                            ) {
                                Text("Push", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeGitHeader(
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
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        // Row 1: Branch & Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = BgCard,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.weight(1f).clickable(onClick = onBranchClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.AccountTree, null, tint = AccentBlue, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        branch,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Refresh, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Actions (Icons only to save space)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CompactActionButton(Icons.Rounded.ArrowDownward, "Pull", onPull)
            CompactActionButton(Icons.Rounded.ArrowUpward, "Push", onPush)
            CompactActionButton(Icons.Rounded.Sync, "Fetch", onFetch)
            CompactActionButton(Icons.Rounded.Archive, "Stash", onStash)
            CompactActionButton(Icons.Rounded.History, "History", onHistory)
        }
    }
}

@Composable
private fun CompactActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tooltip: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(BgCard, RoundedCornerShape(4.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = tooltip, tint = TextSecondary, modifier = Modifier.size(14.dp))
    }
}
