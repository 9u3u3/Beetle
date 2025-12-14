package com.bsbeetle.bananaspaghetti.directGit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * GitTerminalView - Displays Tenebrio terminal output for Git operations
 *
 * Shows:
 * - Real-time command execution
 * - Command history
 * - Success/error indicators
 * - Collapsible UI
 */
@Composable
fun GitTerminalView(
        commandHistory: List<String>,
        modifier: Modifier = Modifier,
        initiallyExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new commands are added
    LaunchedEffect(commandHistory.size) {
        if (commandHistory.isNotEmpty() && isExpanded) {
            listState.animateScrollToItem(commandHistory.size - 1)
        }
    }

    Card(
            modifier = modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E) // Dark terminal background
                    )
    ) {
        Column {
            // Header
            Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2D2D2D),
                    onClick = { isExpanded = !isExpanded }
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🖥️", fontSize = 16.sp)
                        Text(
                                text = "Git Terminal (Tenebrio)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF4EC9B0) // Terminal green
                        )
                        if (commandHistory.isNotEmpty()) {
                            Text(
                                    text = "(${commandHistory.size} commands)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                                imageVector =
                                        if (isExpanded) Icons.Default.KeyboardArrowDown
                                        else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Color.Gray
                        )
                    }
                }
            }

            // Terminal content
            AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
            ) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF1E1E1E))
                ) {
                    if (commandHistory.isEmpty()) {
                        // Empty state
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text = "No commands executed yet",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        // Command history
                        LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) { items(commandHistory) { line -> TerminalLine(line) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLine(line: String) {
    val color =
            when {
                line.startsWith("$") -> Color(0xFF4EC9B0) // Command - cyan
                line.startsWith("✅") -> Color(0xFF6A9955) // Success - green
                line.startsWith("❌") -> Color(0xFFF48771) // Error - red
                line.startsWith("🔑") || line.startsWith("🔐") || line.startsWith("🔓") ->
                        Color(0xFFDCDCAA) // SSH operations - yellow
                line.startsWith("📁") ||
                        line.startsWith("📂") ||
                        line.startsWith("📤") ||
                        line.startsWith("📥") -> Color(0xFF569CD6) // File operations - blue
                line.startsWith("⚠️") -> Color(0xFFCE9178) // Warning - orange
                else -> Color(0xFFD4D4D4) // Default - light gray
            }

    Text(
            text = line,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = color,
            lineHeight = 16.sp
    )
}
