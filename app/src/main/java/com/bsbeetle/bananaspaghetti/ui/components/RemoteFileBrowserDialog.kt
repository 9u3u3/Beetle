package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import com.bsbeetle.bananaspaghetti.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bsbeetle.bananaspaghetti.utils.ssh.RemoteDirEntry
import com.bsbeetle.bananaspaghetti.viewmodel.SshViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RemoteFileBrowserDialog(
    onDismiss: () -> Unit,
    onFolderSelected: (path: String) -> Unit,
    sshViewModel: SshViewModel,
    initialPath: String = "/"
) {
    val scope = rememberCoroutineScope()

    var currentPath by remember { mutableStateOf(initialPath) }
    var entries by remember { mutableStateOf<List<RemoteDirEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadPath(path: String) {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                val result = sshViewModel.fileSystem?.listDirectory(path) ?: emptyList()
                entries = result
            } catch (e: Exception) {
                error = "Failed to list $path: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(currentPath) {
        loadPath(currentPath)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF252526),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button (if not at root)
                    if (currentPath != "/" && currentPath != initialPath) {
                        TextButton(
                            onClick = {
                                val parent = currentPath.substringBeforeLast("/").ifEmpty { "/" }
                                currentPath = parent
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("←", color = Color(0xFF9CDCFE), fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = currentPath,
                        color = Color(0xFFD4D4D4),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFF3C3C3C), thickness = 0.5.dp)

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(28.dp),
                                color = Color(0xFF007ACC),
                                strokeWidth = 2.dp
                            )
                        }
                        error != null -> {
                            Text(
                                text = error!!,
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            )
                        }
                        entries.isEmpty() -> {
                            Text(
                                text = "Empty folder",
                                color = Color(0xFF858585),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                items(entries, key = { it.fullPath }) { entry ->
                                    RemoteEntryRow(
                                        entry = entry,
                                        onClick = {
                                            if (entry.isDir) {
                                                currentPath = entry.fullPath
                                            }
                                            // Files are not selectable — only folders
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF3C3C3C), thickness = 0.5.dp)

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF888888), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onFolderSelected(currentPath) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC))
                    ) {
                        Text("Open This Folder", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteEntryRow(entry: RemoteDirEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = entry.isDir, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                if (entry.isDir)
                    R.drawable.folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                else
                    R.drawable.article_icon
            ),
            contentDescription = null,
            tint = if (entry.isDir) Color(0xFFDCB67A) else Color(0xFF858585),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = entry.name,
            color = if (entry.isDir) Color(0xFFD4D4D4) else Color(0xFF858585),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (entry.isDir) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF555555),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
