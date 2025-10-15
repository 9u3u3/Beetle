package com.bsbeetle.bananaspaghetti.utils

import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.ExpandViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

@Composable
fun Tree(
    modifier: Modifier = Modifier,
    webView: WebView
) {

    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
    val context = LocalContext.current
    var pickedFolder: DocumentFile? = null
    val uri = fileViewModel.selectedFolderUri
    val scrollState = rememberScrollState()

    uri?.let {
        try {
            pickedFolder = DocumentFile.fromTreeUri(context, uri)
        } catch (e: Exception) {
            Log.e("Tree", "Folder not found: ${e.message}")
        }

        LazyColumn(
            modifier = modifier.padding(5.dp).horizontalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            pickedFolder?.listFiles()?.forEach { file ->
                item {
                    if(file.isFile) {
                        val fileName: String = getFileName(context, file.uri)
                        Row (
                            modifier = Modifier.clickable {
                                // Buffer line implementation
                                bufferViewModel.bufferLine[file] = readFromFile(context, file.uri)
                                loadContentWhenReady(webView, bufferViewModel.bufferLine[file]!!)
                                bufferViewModel.filesOpenedList += file
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = getIconResourceId(fileName)),
                                contentDescription = "file icon",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$fileName",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    } else if(file.isDirectory) {
                        DirectoryNode(
                            depth = 0,
                            directory = file,
                            webView
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun DirectoryNode(
    depth: Int,
    directory: DocumentFile,
    webView: WebView
) {

    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
    val context = LocalContext.current
    val indent = depth * 7.dp


    // State to track expand/collapse
    // The composes recomposes when this value is changed
    // In kotlin the composable's recomposes when when any observable state values change their values
    val expandViewModel: ExpandViewModel = viewModel()
    var isExpanded = expandViewModel.expandedFolders[directory.uri.toString()] ?: false

    Column {
        Row(
            modifier = Modifier.padding(start = indent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = if(isExpanded) R.drawable.folder_exp else R.drawable.folder_noexp),
                contentDescription = "folder open or close",
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = getFileName(context, directory.uri),
                color = Color.White,
                modifier = Modifier
                    .clickable { expandViewModel.expandedFolders[directory.uri.toString()] = !isExpanded }, // toggle expand
                fontSize = 15.sp
            )
        }

        // If expanded, show children
        if (isExpanded) {
            directory.listFiles().forEach { file ->
                if (file.isFile) {
                    val fileName: String = getFileName(context, file.uri)
                    Row (
                        modifier = Modifier
                            .padding(start = indent + 7.dp)
                            .clickable{
                                // Buffer line implementation
                                bufferViewModel.bufferLine[file] = readFromFile(context, file.uri)
                                loadContentWhenReady(webView, bufferViewModel.bufferLine[file]!!)
                                bufferViewModel.filesOpenedList += file
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = getIconResourceId(fileName)),
                            contentDescription = "file icon",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$fileName",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                } else if (file.isDirectory) {
                    DirectoryNode(
                        depth = depth + 1,
                        directory = file,
                        webView
                    )
                }
            }
        }
    }
}