package com.bsbeetle.bananaspaghetti.directGit.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import com.bsbeetle.bananaspaghetti.utils.git.BranchInfo

/**
 * Dialog to select which remote branch to pull from.
 */
@Composable
fun PullBranchDialog(
    remoteBranches: List<BranchInfo>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GitTheme.BgCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(12.dp))
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.ArrowDownward,
                            null,
                            tint = GitTheme.AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Pull from Remote Branch",
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

                // Branch List
                if (remoteBranches.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.CloudOff,
                                null,
                                tint = GitTheme.TextTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No remote branches found",
                                color = GitTheme.TextSecondary,
                                fontSize = 13.sp
                            )
                            Text("Try fetching first", color = GitTheme.TextTertiary, fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(remoteBranches) { branch ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val branchName = branch.name.substringAfter("/")
                                        onSelect(branchName)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Cloud,
                                    null,
                                    tint = GitTheme.AccentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(branch.name, color = GitTheme.TextPrimary, fontSize = 14.sp)
                            }
                            HorizontalDivider(color = GitTheme.BorderColor.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
