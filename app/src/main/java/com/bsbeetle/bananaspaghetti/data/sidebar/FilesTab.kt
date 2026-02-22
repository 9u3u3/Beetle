package com.bsbeetle.bananaspaghetti.data.sidebar

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.LocalSshViewModel
import com.bsbeetle.bananaspaghetti.utils.ssh.SshFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FilesTab(
    currentPath: String?,
    onFileSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val fileViewModel = LocalFileViewModel.current
    val sshViewModel = LocalSshViewModel.current

    // Multi-select state from FileViewModel
    val isMultiSelectMode = fileViewModel.isMultiSelectMode
    val selectedFilePaths = fileViewModel.selectedFilePaths

    LaunchedEffect(Unit) {
        Log.d("FilesTab", "selectedUri: ${fileViewModel.selectedUri}")
        Log.d("FilesTab", "selectedFolderUri: ${fileViewModel.selectedFolderUri}")
    }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Remote files loaded asynchronously (SFTP must not run on main thread)
    var remoteFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    LaunchedEffect(
        fileViewModel.isRemoteMode,
        fileViewModel.remoteBasePath,
        expandedFolders,
        fileViewModel.fileTreeRefreshCounter
    ) {
        if (fileViewModel.isRemoteMode) {
            val sshFs = sshViewModel.fileSystem
            val basePath = fileViewModel.remoteBasePath
            if (sshFs != null && basePath != null) {
                remoteFiles = withContext(Dispatchers.IO) {
                    buildRemoteFileTree(sshFs, basePath, 0, expandedFolders)
                }
            } else {
                remoteFiles = emptyList()
            }
        } else {
            remoteFiles = emptyList()
        }
    }

    // Local files loaded synchronously (DocumentFile is fine on main thread)
    val localFiles = remember(
        fileViewModel.selectedFolderUri,
        expandedFolders,
        searchQuery,
        fileViewModel.fileTreeRefreshCounter
    ) {
        val rootUri = fileViewModel.selectedFolderUri
        if (rootUri != null) {
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            rootDoc?.let { buildFileTree(it, 0, expandedFolders, searchQuery.isNotEmpty()) }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    val files = if (fileViewModel.isRemoteMode) remoteFiles else localFiles
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Modern search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = {
                Text("Search files...", color = Color(0xFF6A6A6A), fontSize = 13.sp)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF858585),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF858585),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007ACC),
                unfocusedBorderColor = Color(0xFF3C3C3C),
                focusedTextColor = Color(0xFFE8E8E8),
                unfocusedTextColor = Color(0xFFCCCCCC),
                cursorColor = Color(0xFF007ACC),
                focusedContainerColor = Color(0xFF2D2D2D),
                unfocusedContainerColor = Color(0xFF252526)
            ),
            singleLine = true,
            shape = RoundedCornerShape(6.dp)
        )

        // File tree with smooth animations
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val filteredFiles = files.filter {
                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
            }

//            if (filteredFiles.isEmpty() && fileViewModel.selectedUri == null) {
            if (filteredFiles.isEmpty() && fileViewModel.selectedFolderUri == null && !fileViewModel.isRemoteMode) {
                item {
                    Text(
                        text = "No folder selected. Open a folder to see files.",
                        color = Color(0xFF858585),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(filteredFiles, key = { it.path }) { file ->
                    FileTreeItem(
                        file = file,
                        isExpanded = expandedFolders.contains(file.path),
                        isSelected = currentPath == file.path,
                        onToggleExpand = {
                            expandedFolders = if (expandedFolders.contains(file.path)) {
                                expandedFolders - file.path
                            } else {
                                expandedFolders + file.path
                            }
                        },
                        onFileClick = {
                            if (fileViewModel.isRemoteMode && !file.isDirectory) {
                                // For remote files, set currentRemotePath — triggers
                                // LaunchedEffect in InitialEditorUI to load content via SFTP
                                fileViewModel.currentRemotePath = file.path
                                fileViewModel.currentFilePath = file.path
                            } else {
                                onFileSelected(file.path)
                            }
                        },
                        // Multi-select not supported in remote mode
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
}


// Helper to build remote file tree via SFTP — same FileItem type as local tree
fun buildRemoteFileTree(
    sshFs: SshFileSystem,
    path: String,
    level: Int,
    expandedFolders: Set<String>
): List<FileItem> {
    val result = mutableListOf<FileItem>()

    if (level == 0) {
        // Add root folder itself
        val rootName = path.substringAfterLast("/").ifEmpty { path }
        result.add(FileItem(name = rootName, path = path, isDirectory = true, level = 0))
        if (expandedFolders.contains(path)) {
            result.addAll(buildRemoteFileTree(sshFs, path, 1, expandedFolders))
        }
        return result
    }

    val entries = try {
        sshFs.listDirectory(path)
    } catch (_: Exception) {
        emptyList()
    }

    for (entry in entries) {
        result.add(
            FileItem(
                name = entry.name,
                path = entry.fullPath,
                isDirectory = entry.isDir,
                level = level
            )
        )
        if (entry.isDir && expandedFolders.contains(entry.fullPath)) {
            result.addAll(buildRemoteFileTree(sshFs, entry.fullPath, level + 1, expandedFolders))
        }
    }

    return result
}

// Helper function to build file tree recursively
fun buildFileTree(
    doc: DocumentFile,
    level: Int,
    expandedFolders: Set<String>,
    searchActive: Boolean = false,
    includeRoot: Boolean = true
): List<FileItem> {
    val result = mutableListOf<FileItem>()
    
    // Add root folder as first item (level 0)
    if (includeRoot && level == 0) {
        val rootPath = doc.uri.toString()
        val rootName = doc.name ?: "Project"
        
        result.add(
            FileItem(
                name = rootName,
                path = rootPath,
                isDirectory = true,
                level = 0
            )
        )
        
        // If root is expanded or search is active, add children at level 1
        if (searchActive || expandedFolders.contains(rootPath)) {
            result.addAll(buildFileTreeChildren(doc, 1, expandedFolders, searchActive))
        }
        return result
    }
    
    return buildFileTreeChildren(doc, level, expandedFolders, searchActive)
}

// Helper to build children of a folder
private fun buildFileTreeChildren(
    doc: DocumentFile,
    level: Int,
    expandedFolders: Set<String>,
    searchActive: Boolean
): List<FileItem> {
    val result = mutableListOf<FileItem>()

    doc.listFiles().sortedWith(
        compareBy<DocumentFile> { !it.isDirectory }
            .thenBy { it.name?.lowercase() }
    ).forEach { child ->
        val childPath = child.uri.toString()
        val childName = child.name ?: "Unknown"

        result.add(
            FileItem(
                name = childName,
                path = childPath,
                isDirectory = child.isDirectory,
                level = level
            )
        )

        // If it's a directory and expanded, recursively add children
        if (child.isDirectory && (searchActive || expandedFolders.contains(childPath))) {
            result.addAll(buildFileTreeChildren(child, level + 1, expandedFolders, searchActive))
        }
    }

    return result
}