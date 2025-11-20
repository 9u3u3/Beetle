package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.data.sidebar.FileIconUtils
import com.bsbeetle.bananaspaghetti.utils.ImageLoaderSingleton
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.utils.readFromFile
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    context: Context
) {
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val webView = editorViewModel.webView

    Box(
        modifier = modifier
            .background(Color(0xFF1B1B1B))
            .fillMaxWidth()
            .height(if (isLandscape) 18.dp else 28.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 4.dp)
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

                        val bufferedContent = bufferViewModel.bufferLine[documentFile]
                        if (bufferedContent != null) {
                            loadContentWhenReady(webView!!, bufferedContent)
                        } else {
                            val content = readFromFile(context, documentFile.uri)
                            loadContentWhenReady(webView!!, content)
                            bufferViewModel.bufferLine[documentFile] = content
                        }
                    },
                    onClose = {
                        bufferViewModel.filesOpenedList.remove(documentFile)
                        bufferViewModel.bufferLine.remove(documentFile)

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
                    },
                    context
                )
            }
        }
    }
}

@Composable
private fun FileTab(
    fileName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(Color(0xFF1B1B1B))
    ) {
        Row(
            modifier = Modifier
                .height(17.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val iconPath = FileIconUtils.getIconPath(fileName)
            AsyncImage(
                model = iconPath,
                imageLoader = ImageLoaderSingleton.get(context),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            AnimatedContent(
                targetState = fileName,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                },
                label = "fileName"
            ) { name ->
                Text(
                    text = name,
                    color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (isSelected) Color(0xFFB0B0B0) else Color(0xFF6A6A6A),
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // Not showing up
        if (isSelected) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF4379FF))
            )
        }

    }
}

@Composable
fun LeftTopBar(
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {},
    onFileClick: () -> Unit = {},
    onUndoClick: () -> Unit = {},
    onRedoClick: () -> Unit = {},
    onRunClick: () -> Unit = {},
    onDebugClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .background(Color(0xFF1B1B1B))
            .fillMaxWidth()
            .height(if (isLandscape) 18.dp else 28.dp)
            .padding(start = 20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            // Save button
            TopBarIconButton(
                icon = R.drawable.save_icon,
                onClick = onSaveClick
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            // File button
            TopBarIconButton(
                // title = "File",
                icon = R.drawable.article_icon,
                onClick = onFileClick
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            // Undo button (icon only)
            TopBarIconButton(
                icon = R.drawable.undo_icon,
                onClick = onUndoClick
            )

            // Redo button (icon only)
            TopBarIconButton(
                icon = R.drawable.redo_icon,
                onClick = onRedoClick
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            // Run button
            TopBarIconButton(
                // title = "Run",
                icon = R.drawable.play_arrow,
                onClick = onRunClick
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            // Git (icon only)
            TopBarIconButton(
                icon = R.drawable.graph_icon,
                onClick = onDebugClick
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxHeight()
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun RightTopBar(
    modifier: Modifier = Modifier,
    isTerminal: Boolean = false,
    onFullscreenClick: () -> Unit = {},
    onTerminalClick: () -> Unit = {},
    onExtensionsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onWebClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .background(Color(0xFF1B1B1B))
            .fillMaxWidth()
            .height(if (isLandscape) 18.dp else 28.dp)
            .padding(end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            // Fullscreen button
            TopBarIconButton(
                icon = R.drawable.zoom_in,
                onClick = onFullscreenClick
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            TopBarIconButton(
                icon = R.drawable.web_icon,
                onClick = onWebClick
            )

            if(!isTerminal) {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = Color(0xFF3E3E42)
                )

                // Ai button
                TopBarIconButton(
                    icon = R.drawable.network_intel,
                    onClick = onTerminalClick
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            // Extensions button
            TopBarIconButton(
                icon = R.drawable.extension,
                onClick = onExtensionsClick
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )

            // Settings button
            TopBarIconButton(
                icon = R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                onClick = onSettingsClick
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF3E3E42)
            )
        }
    }
}

@Composable
fun LeftTermTopBar(
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .background(Color(0xFF1B1B1B))
            .fillMaxWidth()
            .height(if (isLandscape) 18.dp else 28.dp)
            .padding(start = 20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {

        }
    }
}
