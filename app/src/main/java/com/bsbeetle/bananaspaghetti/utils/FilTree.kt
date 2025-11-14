package com.bsbeetle.bananaspaghetti.utils

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.data.sidebar.FileTreeItem
import com.bsbeetle.bananaspaghetti.data.sidebar.buildFileTree

@Composable
fun Tree (modifier: Modifier = Modifier) {

    val fileViewModel = LocalFileViewModel.current
    val context = LocalContext.current
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    val scrollState = rememberScrollState()

    val files = remember(fileViewModel.selectedFolderUri, expandedFolders) {
        val rootUri = fileViewModel.selectedFolderUri
        if (rootUri != null) {
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            rootDoc?.let { buildFileTree(it, 0, expandedFolders) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .horizontalScroll(scrollState)
    ) {
        if (files.isEmpty() && fileViewModel.selectedFolderUri == null) {
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
                    onFileClick = { fileViewModel.selectedUri = Uri.parse(file.path); fileViewModel.currentFilePath = file.path; }
                )
            }
        }
    }
}