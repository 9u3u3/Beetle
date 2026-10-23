package com.bsbeetle.bananaspaghetti.directGit.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import com.bsbeetle.bananaspaghetti.utils.git.BranchInfo

/**
 * Dialog for viewing and managing Git branches.
 */
@Composable
fun BranchDialog(
    branches: List<BranchInfo>,
    onDismiss: () -> Unit,
    onCheckout: (String) -> Unit,
    onDelete: (String, Boolean) -> Unit,
    onNewBranch: () -> Unit,
    onMerge: ((String) -> Unit)? = null
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
                        color = GitTheme.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(local) { b -> BranchItem(b, onCheckout, onDelete, onMerge) }
            }
            if (remote.isNotEmpty()) {
                item {
                    Text(
                        "REMOTE",
                        color = GitTheme.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(remote) { b -> BranchItem(b, onCheckout, onDelete, onMerge) }
            }
        }
    }
}

@Composable
private fun BranchItem(
    branch: BranchInfo,
    onCheckout: (String) -> Unit,
    onDelete: (String, Boolean) -> Unit,
    onMerge: ((String) -> Unit)?
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteFromRemote by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = GitTheme.BgCard,
            titleContentColor = GitTheme.TextPrimary,
            textContentColor = GitTheme.TextSecondary,
            title = { Text("Delete Branch") },
            text = {
                Column {
                    Text("Delete branch '${branch.name}'?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = deleteFromRemote,
                            onCheckedChange = { deleteFromRemote = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GitTheme.AccentRed,
                                uncheckedColor = GitTheme.TextTertiary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Also delete from remote", color = GitTheme.TextSecondary, fontSize = 13.sp)
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
                ) { Text("Delete", color = GitTheme.AccentRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = GitTheme.TextTertiary)
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckout(branch.name) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (branch.isCurrent) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
            null,
            tint = if (branch.isCurrent) GitTheme.AccentGreen else GitTheme.TextTertiary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            branch.name,
            color = if (branch.isCurrent) GitTheme.AccentGreen else GitTheme.TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        if (!branch.isCurrent && !branch.isRemote && onMerge != null) {
            IconButton(onClick = { onMerge(branch.name) }, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Rounded.CallMerge,
                    "Merge into current branch",
                    tint = GitTheme.AccentBlue.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (!branch.isCurrent && !branch.isRemote) {
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    null,
                    tint = GitTheme.AccentRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Dialog for creating a new branch.
 */
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
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Branch Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GitTheme.AccentBlue,
                unfocusedBorderColor = GitTheme.BorderColor,
                focusedLabelColor = GitTheme.AccentBlue,
                unfocusedLabelColor = GitTheme.TextTertiary,
                cursorColor = GitTheme.AccentBlue
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Create from:", color = GitTheme.TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = sourceBranch,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GitTheme.AccentBlue,
                    unfocusedBorderColor = GitTheme.BorderColor,
                    cursorColor = GitTheme.AccentBlue
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(GitTheme.BgCard)
            ) {
                branches.forEach { branch ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (branch.isCurrent) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        tint = GitTheme.AccentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(branch.name, color = GitTheme.TextPrimary)
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
            colors = ButtonDefaults.buttonColors(containerColor = GitTheme.AccentBlue),
            shape = RoundedCornerShape(6.dp)
        ) { Text("Create Branch") }
    }
}
