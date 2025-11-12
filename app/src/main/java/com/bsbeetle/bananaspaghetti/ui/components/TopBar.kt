package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
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
        modifier = if (showBufferTabs) {
            modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2D2D30), Color(0xFF252526))
                    )
                )
        } else {
            modifier.background(color)
        }
            .fillMaxWidth()
            .height(if (isLandscape) 18.dp else 28.dp)
    )
    {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = if (showBufferTabs) Arrangement.Start else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBufferTabs && context != null) {
                if (bufferViewModel.filesOpenedList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO FILES OPEN",
                            color = Color(0xFF6A6A6A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.2.sp
                        )
                    }
                } else {
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
            } else if (buttons != null) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    buttons.forEachIndexed { index, button ->
                        Box(
                            modifier = Modifier
                                .clickable { button.onClick() }
                                .background(Color.Transparent)
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                button.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = button.title ?: "",
                                        tint = Color(0xFFCCCCCC),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                button.imageRes?.let { imageRes ->
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = imageRes),
                                        contentDescription = button.title ?: "",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                button.title?.let {
                                    Text(
                                        text = it.uppercase(),
                                        color = Color(0xFFCCCCCC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        if (index < buttons.size - 1) {
                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .padding(vertical = 4.dp),
                                color = Color(0xFF3E3E42)
                            )
                        }
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
    onClose: () -> Unit,
    context: Context
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1E1E1E) else Color(0xFF2D2D30),
        animationSpec = tween(200),
        label = "bgColor"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "elevation"
    )

    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .height(if (isSelected) 28.dp else 24.dp)
            .shadow(elevation, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
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
                color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFFB0B0B0),
                fontSize = if (isSelected) 11.sp else 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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
}