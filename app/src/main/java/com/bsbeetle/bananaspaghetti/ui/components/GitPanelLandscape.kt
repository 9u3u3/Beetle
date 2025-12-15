package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitUiState
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Git actions grid for landscape mode.
 * Contains Pull/Push/Fetch, Add All/Stash/History, and Commit button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GitActionsGrid(
    uiState: GitUiState,
    gitViewModel: GitViewModel?,
    onCommitClick: () -> Unit,
    onPushClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onPullFromBranchClick: () -> Unit = {}
) {
    var showPullMenu by remember { mutableStateOf(false) }
    var showStashMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Row 1: Pull (dropdown), Push, Fetch
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pull Dropdown
            Box {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPullMenu = true }
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.ArrowDownward,
                        "Pull",
                        tint = GitTheme.AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pull", color = GitTheme.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Rounded.ExpandMore, null, tint = GitTheme.TextTertiary, modifier = Modifier.size(10.dp))
                    }
                }

                DropdownMenu(
                    expanded = showPullMenu,
                    onDismissRequest = { showPullMenu = false },
                    modifier = Modifier
                        .background(GitTheme.BgCard)
                        .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(4.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Pull (current)", color = GitTheme.TextPrimary, fontSize = 12.sp) },
                        onClick = {
                            gitViewModel?.pull()
                            showPullMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.ArrowDownward, null, tint = GitTheme.AccentBlue, modifier = Modifier.size(16.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Pull from branch...", color = GitTheme.TextPrimary, fontSize = 12.sp) },
                        onClick = {
                            onPullFromBranchClick()
                            showPullMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.AccountTree, null, tint = GitTheme.AccentGreen, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            CompactActionIcon(icon = Icons.Rounded.ArrowUpward, label = "Push", color = GitTheme.AccentGreen, onClick = onPushClick)
            CompactActionIcon(icon = Icons.Rounded.Sync, label = "Fetch", color = GitTheme.TextSecondary, onClick = { gitViewModel?.fetch() })
        }

        // Row 2: Add All, Stash (dropdown), History
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactActionIcon(
                icon = Icons.Rounded.Add,
                label = "Add All",
                color = GitTheme.AccentGreen,
                onClick = {
                    val allFiles = uiState.modifiedFiles + uiState.untrackedFiles
                    allFiles.forEach { gitViewModel?.stageFile(it) }
                }
            )

            // Stash Dropdown
            Box {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showStashMenu = true }
                        .padding(4.dp)
                ) {
                    Icon(Icons.Rounded.Archive, "Stash", tint = GitTheme.TextSecondary, modifier = Modifier.size(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stash", color = GitTheme.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Rounded.ExpandMore, null, tint = GitTheme.TextTertiary, modifier = Modifier.size(10.dp))
                    }
                }

                DropdownMenu(
                    expanded = showStashMenu,
                    onDismissRequest = { showStashMenu = false },
                    modifier = Modifier
                        .background(GitTheme.BgCard)
                        .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(4.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Stash Changes", color = GitTheme.TextPrimary, fontSize = 12.sp) },
                        onClick = {
                            gitViewModel?.stash()
                            showStashMenu = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Archive, null, tint = GitTheme.AccentBlue, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Pop Stash", color = GitTheme.TextPrimary, fontSize = 12.sp) },
                        onClick = {
                            gitViewModel?.stashPop()
                            showStashMenu = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null, tint = GitTheme.AccentGreen, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            CompactActionIcon(icon = Icons.Rounded.History, label = "History", color = GitTheme.TextSecondary, onClick = onHistoryClick)
        }

        // Full-width Commit button
        Button(
            onClick = onCommitClick,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GitTheme.AccentBlue),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Commit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Compact action icon with press animation and tooltip.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactActionIcon(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var showTooltip by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) color.copy(alpha = 0.15f) else Color.Transparent)
            .combinedClickable(
                onClick = {
                    isPressed = true
                    onClick()
                    coroutineScope.launch {
                        delay(150)
                        isPressed = false
                    }
                },
                onLongClick = {
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        showTooltip = true
                    }
                }
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))

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
                    Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

/**
 * Changes list page for landscape mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangesListPage(
    stagedFiles: List<String>,
    modifiedFiles: List<String>,
    untrackedFiles: List<String>,
    conflictedFiles: List<String>,
    selectedFiles: Set<String>,
    onToggleFile: (String) -> Unit,
    onSelectAll: () -> Unit,
    onFileClick: (String) -> Unit
) {
    val allChanges = (stagedFiles + modifiedFiles + untrackedFiles + conflictedFiles).distinct().sorted()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectAll)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Checkbox(
                    checked = selectedFiles.size == allChanges.size && allChanges.isNotEmpty(),
                    onCheckedChange = { onSelectAll() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = GitTheme.AccentBlue,
                        uncheckedColor = GitTheme.TextTertiary,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.scale(0.7f)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("CHANGES (${allChanges.size})", color = GitTheme.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (allChanges.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No changes", color = GitTheme.TextTertiary, fontSize = 11.sp)
                    }
                }
            }

            items(allChanges) { path ->
                val isStaged = stagedFiles.contains(path)
                val status = when {
                    untrackedFiles.contains(path) -> "U"
                    modifiedFiles.contains(path) -> "M"
                    conflictedFiles.contains(path) -> "C"
                    else -> "S"
                }
                CompactFileItem(
                    path = path,
                    status = status,
                    isStaged = isStaged,
                    isSelected = selectedFiles.contains(path),
                    onToggle = { onToggleFile(path) },
                    onClick = { onFileClick(path) }
                )
            }
        }
    }
}

/**
 * Compact file item for changes list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactFileItem(
    path: String,
    status: String,
    isStaged: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val file = java.io.File(path)
    val fileName = file.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) GitTheme.BgHover else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = GitTheme.AccentBlue,
                    uncheckedColor = GitTheme.TextTertiary,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.scale(0.6f)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))

        Text(
            status,
            color = when (status) {
                "U" -> GitTheme.AccentGreen
                "M" -> GitTheme.AccentYellow
                "C" -> Color.Red
                else -> GitTheme.AccentBlue
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(14.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            fileName,
            color = when (status) {
                "U" -> GitTheme.AccentGreen
                "M" -> GitTheme.AccentYellow
                else -> GitTheme.TextPrimary
            },
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Git action icon for grid display.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GitActionIcon(
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
                .background(GitTheme.BgCard)
                .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(6.dp))
                .combinedClickable(onClick = onClick, onLongClick = { showTooltip = true })
                .padding(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = GitTheme.TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
        }

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
                    Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}
