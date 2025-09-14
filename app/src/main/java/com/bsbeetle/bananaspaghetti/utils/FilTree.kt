package com.bsbeetle.bananaspaghetti.utils

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.times
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

@Composable
fun Tree(modifier: Modifier = Modifier, fileViewModel: FileViewModel) {

    val context = LocalContext.current
    var pickedFolder: DocumentFile? = null
    val uri = fileViewModel.selectedFolderUri

    uri?.let {
        try {
            pickedFolder = DocumentFile.fromTreeUri(context, uri)
        } catch (e: Exception) {
            Log.e("Tree", "Folder not found: ${e.message}")
        }

        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            pickedFolder?.listFiles()?.forEach { file ->
                item {
                    if(file.isFile) {
                        Text(
                            text = "\uD83D\uDCDD ${getFileName(context, file.uri)}",
                            color = Color.White,
                            modifier = Modifier
                                .padding(start = 7.dp),
                            fontSize = 15.sp
                        )
                    } else if(file.isDirectory) {
                        DirectoryNode(depth = 1, directory = file)
                    }
                }
            }
        }
    }
}

@Composable
fun DirectoryNode(depth: Int, directory: DocumentFile) {
    val context = LocalContext.current
    val indent = depth * 7.dp

    // State to track expand/collapse
    // The composes recomposes when this value is changed
    // In kotlin the composable's recomposes when when any observable state values change their values
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        // Directory row (clickable)
        Text(
            text = (if (isExpanded) "📂 " else "📁 ") + getFileName(context, directory.uri),
            color = Color.White,
            modifier = Modifier
                .padding(start = indent)
                .clickable { isExpanded = !isExpanded }, // toggle expand
            fontSize = 15.sp
        )

        // If expanded, show children
        if (isExpanded) {
            directory.listFiles().forEach { file ->
                if (file.isFile) {
                    Text(
                        text = "📝 ${getFileName(context, file.uri)}",
                        color = Color.White,
                        modifier = Modifier.padding(start = indent + 7.dp),
                        fontSize = 15.sp
                    )
                } else if (file.isDirectory) {
                    DirectoryNode(depth = depth + 1, directory = file)
                }
            }
        }
    }
}