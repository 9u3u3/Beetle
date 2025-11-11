package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.utils.readFromFile
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    color: Color,
    buttons: List<TopBarButtons>? = null,
    context: Context? = null,
    showBufferTabs: Boolean = false
) {
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val webView = editorViewModel.webView

    Box(
        modifier = modifier
            .background(color)
            .fillMaxWidth()
            .height(if (isLandscape) 15.dp else 25.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBufferTabs && context != null) {
                // Show buffer tabs in the middle
                if (bufferViewModel.filesOpenedList.isEmpty()) {
                    Text(
                        text = "No files open",
                        color = Color(0xFF858585),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(
                            items = bufferViewModel.filesOpenedList,
                            key = { it.uri.toString() }
                        ) { documentFile ->
                            FileTab(
                                fileName = documentFile.name ?: "Unknown",
                                isSelected = documentFile.uri.toString() == fileViewModel.currentFilePath,
                                onClick = {
                                    fileViewModel.selectedUri = documentFile.uri
                                    fileViewModel.currentFilePath = documentFile.uri.toString()

                                    // Load content from buffer if available
                                    val bufferedContent = bufferViewModel.bufferLine[documentFile]
                                    if (bufferedContent != null) {
                                        loadContentWhenReady(webView!!, bufferedContent)
                                    } else {
                                        // Read from file if not in buffer (shouldn't happen but safety)
                                        val content = readFromFile(context, documentFile.uri)
                                        loadContentWhenReady(webView!!, content)
                                        bufferViewModel.bufferLine[documentFile] = content
                                    }
                                },
                                onClose = {
                                    bufferViewModel.filesOpenedList.remove(documentFile)
                                    bufferViewModel.bufferLine.remove(documentFile)

                                    // If closing current file, switch to another or clear
                                    if (documentFile.uri.toString() == fileViewModel.currentFilePath) {
                                        val nextFile = bufferViewModel.filesOpenedList.firstOrNull()
                                        if (nextFile != null) {
                                            fileViewModel.selectedUri = nextFile.uri
                                            fileViewModel.currentFilePath = nextFile.uri.toString()
                                            val content = bufferViewModel.bufferLine[nextFile] ?: ""
                                            loadContentWhenReady(webView!!, content)
                                        } else {
                                            fileViewModel.selectedUri = null
                                            fileViewModel.currentFilePath = null
                                            loadContentWhenReady(webView!!, "")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (buttons != null) {
                // Show regular buttons
                Row(horizontalArrangement = Arrangement.Center) {
                    buttons.forEach { button ->
                        Box(
                            modifier = Modifier
                                .clickable { button.onClick() }
                                .background(Color.Transparent)
                                .fillMaxHeight()
                                .width(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Show icon if available
                                button.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = button.title ?: "",
                                        tint = Color.White,
                                        modifier = Modifier.height(16.dp)
                                    )
                                }

                                // Show image if available
                                button.imageRes?.let { imageRes ->
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = imageRes),
                                        contentDescription = button.title ?: "",
                                        modifier = Modifier.height(16.dp)
                                    )
                                }

                                // Show title if available
                                button.title?.let {
                                    Text(
                                        text = it,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = Color(0xFF8D8D8D)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTab(
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(if (isSelected) 25.dp else 23.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(
                if (isSelected) Color(0xFF2D2D30) else Color(0xFF252526)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // File name
        Text(
            text = fileName,
            color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFFCCCCCC),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close file",
                tint = if (isSelected) Color(0xFFCCCCCC) else Color(0xFF858585),
                modifier = Modifier.size(10.dp)
            )
        }
    }

    // Add visual separator
    Spacer(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(Color(0xFF1B1B1B))
    )
}