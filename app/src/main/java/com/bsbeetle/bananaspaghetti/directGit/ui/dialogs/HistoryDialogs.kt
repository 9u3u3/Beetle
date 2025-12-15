package com.bsbeetle.bananaspaghetti.directGit.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import com.bsbeetle.bananaspaghetti.utils.git.CommitInfo
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dialog showing commit history.
 */
@Composable
fun CommitHistoryDialog(
    commits: List<CommitInfo>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GitTheme.BgDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(12.dp))
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Commit History",
                        color = GitTheme.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = GitTheme.TextTertiary)
                    }
                }
                HorizontalDivider(color = GitTheme.BorderColor)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(commits) { commit ->
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    commit.shortHash,
                                    color = GitTheme.AccentBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    commit.message,
                                    color = GitTheme.TextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${commit.author} • ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(commit.date)}",
                                color = GitTheme.TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        HorizontalDivider(color = GitTheme.BorderColor)
                    }
                }
            }
        }
    }
}

/**
 * Dialog showing file diff with colorized output.
 */
@Composable
fun DiffDialog(
    diffText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(GitTheme.BgDark)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(GitTheme.BgCard).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Difference,
                            null,
                            tint = GitTheme.AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "File Diff",
                            color = GitTheme.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = GitTheme.TextTertiary)
                    }
                }
                HorizontalDivider(color = GitTheme.BorderColor)

                // Diff Content with colorized lines
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(diffText.lines()) { line ->
                        val (bgColor, textColor) = when {
                            line.startsWith("+") && !line.startsWith("+++") ->
                                Color(0xFF2D4F3E) to Color(0xFF73C991)
                            line.startsWith("-") && !line.startsWith("---") ->
                                Color(0xFF4F2D2D) to Color(0xFFF87171)
                            line.startsWith("@@") -> Color(0xFF2D3A4F) to Color(0xFF6BB3F5)
                            else -> Color.Transparent to GitTheme.TextSecondary
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(
                                line,
                                color = textColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
