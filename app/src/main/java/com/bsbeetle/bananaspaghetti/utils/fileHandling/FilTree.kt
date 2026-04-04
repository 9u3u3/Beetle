package com.bsbeetle.bananaspaghetti.utils.fileHandling

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.LocalSshViewModel
import com.bsbeetle.bananaspaghetti.data.sidebar.FileItem
import com.bsbeetle.bananaspaghetti.data.sidebar.FileTreeItem
import com.bsbeetle.bananaspaghetti.data.sidebar.buildFileTree
import com.bsbeetle.bananaspaghetti.data.sidebar.buildRemoteFileTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Tree(modifier: Modifier = Modifier) {

    val fileViewModel = LocalFileViewModel.current
    val sshViewModel = LocalSshViewModel.current
    val context = LocalContext.current
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    val scrollState = rememberScrollState()

    // Auto-expand the SSH root folder whenever remote mode is active or the base path changes.
    LaunchedEffect(fileViewModel.isRemoteMode, fileViewModel.remoteBasePath) {
        val base = fileViewModel.remoteBasePath
        if (fileViewModel.isRemoteMode && base != null) {
            expandedFolders = expandedFolders + base
        }
    }

    // Observe SSH session liveness.
    val sessionDown by sshViewModel.sessionDown.collectAsStateWithLifecycle()
    val sshConnecting by sshViewModel.connecting.collectAsStateWithLifecycle()

    // Auto-reconnect as soon as the session drops.
    LaunchedEffect(sessionDown) {
        if (sessionDown) {
            val params = sshViewModel.connectionParams
            if (params != null) sshViewModel.connect(params, context)
        }
    }

    // Multi-select state from FileViewModel
    val isMultiSelectMode = fileViewModel.isMultiSelectMode
    val selectedFilePaths = fileViewModel.selectedFilePaths

    // Remote files loaded asynchronously (SFTP must not run on main thread)
    var remoteFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    LaunchedEffect(
        fileViewModel.isRemoteMode,
        fileViewModel.remoteBasePath,
        expandedFolders,
        fileViewModel.fileTreeRefreshCounter,
        sessionDown
    ) {
        if (!fileViewModel.isRemoteMode || sessionDown) {
            remoteFiles = emptyList()
            return@LaunchedEffect
        }
        val sshFs = sshViewModel.fileSystem
        val basePath = fileViewModel.remoteBasePath
        if (sshFs != null && basePath != null) {
            remoteFiles = withContext(Dispatchers.IO) {
                buildRemoteFileTree(sshFs, basePath, 0, expandedFolders)
            }
        } else {
            remoteFiles = emptyList()
        }
    }

    val localFiles = remember(fileViewModel.selectedFolderUri, expandedFolders, fileViewModel.fileTreeRefreshCounter) {
        val rootUri = fileViewModel.selectedFolderUri
        if (rootUri != null) {
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            rootDoc?.let { buildFileTree(it, 0, expandedFolders) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    val files = if (fileViewModel.isRemoteMode) remoteFiles else localFiles

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .horizontalScroll(scrollState)
    ) {
        if (sessionDown && fileViewModel.isRemoteMode) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (sshConnecting) {
                        Text(
                            text = "Reconnecting…",
                            color = Color(0xFFFFAA44),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "SSH session disconnected",
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val params = sshViewModel.connectionParams
                                if (params != null) sshViewModel.connect(params, context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3A3A3A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reconnect", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else if (files.isEmpty() && fileViewModel.selectedFolderUri == null && !fileViewModel.isRemoteMode) {
            item {
                Text(
                    text = "No folder selected. Open a folder to see files.",
                    color = Color(0xFF858585),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(files, key = { it.path }) { file ->
                FileTreeItem(
                    file = file,
                    isExpanded = expandedFolders.contains(file.path),
                    isSelected = fileViewModel.currentFilePath == file.path,
                    onToggleExpand = {
                        expandedFolders = if (expandedFolders.contains(file.path)) {
                            expandedFolders - file.path
                        } else {
                            expandedFolders + file.path
                        }
                    },
                    onFileClick = {
                        if (fileViewModel.isRemoteMode && !file.isDirectory) {
                            fileViewModel.currentRemotePath = file.path
                            fileViewModel.currentFilePath = file.path
                        } else if (!fileViewModel.isRemoteMode) {
                            fileViewModel.selectedUri = Uri.parse(file.path)
                            fileViewModel.currentFilePath = file.path
                        }
                    },
                    isMultiSelectMode = if (fileViewModel.isRemoteMode) false else isMultiSelectMode,
                    isMultiSelected = selectedFilePaths.contains(file.path),
                    onLongPress = {
                        if (!fileViewModel.isRemoteMode) {
                            fileViewModel.enterMultiSelectMode(file.path)
                        }
                    },
                    onMultiSelectTap = {
                        fileViewModel.toggleFileSelection(file.path)
                    }
                )
            }
        }
    }
}