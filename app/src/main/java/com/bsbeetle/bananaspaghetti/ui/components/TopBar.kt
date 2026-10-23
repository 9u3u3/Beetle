package com.bsbeetle.bananaspaghetti.ui.components

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalAiChatViewModel
import com.bsbeetle.bananaspaghetti.LocalBrowserViewModel
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.fileHandling.readFromFile
import com.bsbeetle.bananaspaghetti.utils.fileHandling.saveFile
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.BrowserSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import coil.compose.AsyncImage
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.data.sidebar.FileIconUtils
import com.bsbeetle.bananaspaghetti.utils.resourceHandling.ImageLoaderSingleton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TopBar(modifier: Modifier = Modifier, context: Context) {
        val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
        val editorViewModel: EditorViewModel = LocalEditorViewModel.current
        val fileViewModel: FileViewModel = LocalFileViewModel.current
        val keyboardViewModel = LocalKeyboardViewModel.current
        val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val webView = editorViewModel.webView
        val coroutineScope = rememberCoroutineScope()
        
        // Scale bar height based on settings
        val baseHeight = if (isLandscape) 18.dp else 28.dp
        val barHeight = baseHeight * uiState.topBarScale

        // State for unsaved file close confirmation
        var fileToClose by remember { mutableStateOf<DocumentFile?>(null) }
        var showCloseConfirmation by remember { mutableStateOf(false) }

        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
                                .fillMaxWidth()
                                .height(barHeight)
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
                                BufferCard(
                                        file = documentFile,
                                        isSelected =
                                                documentFile.uri.toString() ==
                                                        fileViewModel.currentFilePath,
                                        isDirty =
                                                bufferViewModel.dirtyFiles.contains(
                                                        documentFile.uri.toString()
                                                ),
                                        onClick = {
                                                android.util.Log.d(
                                                        "TopBar",
                                                        "======== TAB CLICK: ${documentFile.name} ========"
                                                )
                                                android.util.Log.d(
                                                        "TopBar",
                                                        "Current file before switch: ${bufferViewModel.currentFile.value?.name}"
                                                )

                                                // Save current content before switching (with small
                                                // delay for async save)
                                                bufferViewModel.saveCurrentContentToBuffer()

                                                coroutineScope.launch {
                                                        android.util.Log.d(
                                                                "TopBar",
                                                                "Waiting 50ms for save to complete..."
                                                        )
                                                        delay(
                                                                50
                                                        ) // Small delay to ensure save completes

                                                        android.util.Log.d(
                                                                "TopBar",
                                                                "Delay complete, switching to ${documentFile.name}"
                                                        )
                                                        android.util.Log.d(
                                                                "TopBar",
                                                                "Buffer map size: ${bufferViewModel.bufferLine.size}"
                                                        )
                                                        android.util.Log.d(
                                                                "TopBar",
                                                                "URIs in buffer: ${bufferViewModel.bufferLine.keys.take(5)}"
                                                        )

                                                        fileViewModel.selectedUri = documentFile.uri
                                                        fileViewModel.currentFilePath =
                                                                documentFile.uri.toString()

                                                        // Update current file tracker
                                                        bufferViewModel.currentFile.value =
                                                                documentFile
                                                        android.util.Log.d(
                                                                "TopBar",
                                                                "Updated currentFile to: ${documentFile.name}"
                                                        )

                                                        val fileUri = documentFile.uri.toString()
                                                        val bufferedContent =
                                                                bufferViewModel.bufferLine[
                                                                        fileUri] // Use URI as key
                                                        if (bufferedContent != null) {
                                                                android.util.Log.d(
                                                                        "TopBar",
                                                                        "✓ Loading from buffer (URI: $fileUri): ${bufferedContent.length} chars"
                                                                )
                                                                android.util.Log.d(
                                                                        "TopBar",
                                                                        "  First 50 chars: ${bufferedContent.take(50)}"
                                                                )
                                                                webView?.let { wv ->
                                                                    loadContentWhenReady(
                                                                            wv,
                                                                            bufferedContent
                                                                    )
                                                                }
                                                        } else {
                                                                android.util.Log.d(
                                                                        "TopBar",
                                                                        "✗ No buffer found for URI: $fileUri, reading from disk"
                                                                )
                                                                val content =
                                                                        readFromFile(
                                                                                context,
                                                                                documentFile.uri
                                                                        )
                                                                android.util.Log.d(
                                                                        "TopBar",
                                                                        "  Read from disk: ${content.length} chars"
                                                                )
                                                                webView?.let { wv ->
                                                                    loadContentWhenReady(
                                                                            wv,
                                                                            content
                                                                    )
                                                                }
                                                                bufferViewModel.bufferLine[
                                                                        fileUri] =
                                                                        content // Use URI as key
                                                        }

                                                        android.util.Log.d(
                                                                "TopBar",
                                                                "======== TAB SWITCH COMPLETE ========"
                                                        )
                                                }
                                        },
                                        onClose = {
                                                // Check if file has unsaved changes
                                                if (bufferViewModel.dirtyFiles.contains(
                                                                documentFile.uri.toString()
                                                        )
                                                ) {
                                                        fileToClose = documentFile
                                                        showCloseConfirmation = true
                                                } else {
                                                        // Close immediately if no unsaved changes
                                                        bufferViewModel.filesOpenedList.remove(
                                                                documentFile
                                                        )
                                                        bufferViewModel.bufferLine.remove(
                                                                documentFile.uri.toString()
                                                        )
                                                        bufferViewModel.dirtyFiles.remove(
                                                                documentFile.uri.toString()
                                                        )

                                                        if (documentFile.uri.toString() ==
                                                                        fileViewModel
                                                                                .currentFilePath
                                                        ) {
                                                                val nextFile =
                                                                        bufferViewModel
                                                                                .filesOpenedList
                                                                                .firstOrNull()
                                                                if (nextFile != null) {
                                                                        fileViewModel.selectedUri =
                                                                                nextFile.uri
                                                                        fileViewModel
                                                                                .currentFilePath =
                                                                                nextFile.uri
                                                                                        .toString()
                                                                        val content =
                                                                                bufferViewModel
                                                                                        .bufferLine[
                                                                                        nextFile.uri
                                                                                                .toString()]
                                                                                        ?: ""
                                                                        webView?.let { wv ->
                                                                            loadContentWhenReady(
                                                                                    wv,
                                                                                    content
                                                                            )
                                                                        }
                                                                } else {
                                                                        fileViewModel.selectedUri =
                                                                                null
                                                                        fileViewModel
                                                                                .currentFilePath =
                                                                                null
                                                                        webView?.let { wv ->
                                                                            loadContentWhenReady(
                                                                                    wv,
                                                                                    ""
                                                                            )
                                                                        }
                                                                }
                                                        }
                                                }
                                        },
                                        context = context
                                )
                        }

                        // Remote (SSH) file tabs
                        items(
                                items = bufferViewModel.remoteFilesOpenedList,
                                key = { "remote:$it" }
                        ) { remotePath ->
                                RemoteBufferCard(
                                        path = remotePath,
                                        isSelected = fileViewModel.currentFilePath == remotePath,
                                        isDirty = bufferViewModel.dirtyFiles.contains(remotePath),
                                        onClick = {
                                                // Load from buffer immediately, then trigger
                                                // currentRemotePath change for LSP setup
                                                val cached = bufferViewModel.bufferLine[remotePath]
                                                if (cached != null) {
                                                        webView?.let { loadContentWhenReady(it, cached) }
                                                        fileViewModel.currentFilePath = remotePath
                                                }
                                                fileViewModel.currentRemotePath = remotePath
                                        },
                                        onClose = {
                                                bufferViewModel.remoteFilesOpenedList.remove(remotePath)
                                                bufferViewModel.bufferLine.remove(remotePath)
                                                bufferViewModel.dirtyFiles.remove(remotePath)
                                                if (fileViewModel.currentFilePath == remotePath) {
                                                        val nextRemote =
                                                                bufferViewModel.remoteFilesOpenedList.firstOrNull()
                                                        if (nextRemote != null) {
                                                                val cached = bufferViewModel.bufferLine[nextRemote]
                                                                if (cached != null) {
                                                                        webView?.let { loadContentWhenReady(it, cached) }
                                                                        fileViewModel.currentFilePath = nextRemote
                                                                }
                                                                fileViewModel.currentRemotePath = nextRemote
                                                        } else {
                                                                fileViewModel.currentRemotePath = null
                                                                fileViewModel.currentFilePath = null
                                                                webView?.let { loadContentWhenReady(it, "") }
                                                        }
                                                }
                                        }
                                )
                        }
                }
        }

        // Confirmation dialog for closing unsaved files
        if (showCloseConfirmation && fileToClose != null) {
                androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showCloseConfirmation = false },
                        title = { Text("Unsaved Changes", color = Color.White) },
                        text = {
                                Text(
                                        "${fileToClose?.name} has unsaved changes. Do you want to save them?",
                                        color = Color(0xFFB0B0B0)
                                )
                        },
                        confirmButton = {
                                androidx.compose.material3.TextButton(
                                        onClick = {
                                                // Save and close
                                                fileToClose?.let { file ->
                                                        com.bsbeetle.bananaspaghetti.utils
                                                                .fileHandling.saveFile(
                                                                fileViewModel,
                                                                bufferViewModel,
                                                                context
                                                        )
                                                        // Close after saving
                                                        bufferViewModel.filesOpenedList.remove(file)
                                                        bufferViewModel.bufferLine.remove(
                                                                file.uri.toString()
                                                        )
                                                        bufferViewModel.dirtyFiles.remove(
                                                                file.uri.toString()
                                                        )

                                                        if (file.uri.toString() ==
                                                                        fileViewModel
                                                                                .currentFilePath
                                                        ) {
                                                                val nextFile =
                                                                        bufferViewModel
                                                                                .filesOpenedList
                                                                                .firstOrNull()
                                                                if (nextFile != null) {
                                                                        fileViewModel.selectedUri =
                                                                                nextFile.uri
                                                                        fileViewModel
                                                                                .currentFilePath =
                                                                                nextFile.uri
                                                                                        .toString()
                                                                        val content =
                                                                                bufferViewModel
                                                                                        .bufferLine[
                                                                                        nextFile.uri
                                                                                                .toString()]
                                                                                        ?: ""
                                                                        webView?.let { wv ->
                                                                            loadContentWhenReady(
                                                                                    wv,
                                                                                    content
                                                                            )
                                                                        }
                                                                } else {
                                                                        fileViewModel.selectedUri =
                                                                                null
                                                                        fileViewModel
                                                                                .currentFilePath =
                                                                                null
                                                                        webView?.let { wv ->
                                                                            loadContentWhenReady(
                                                                                    wv,
                                                                                    ""
                                                                            )
                                                                        }
                                                                }
                                                        }
                                                }
                                                showCloseConfirmation = false
                                                fileToClose = null
                                        }
                                ) { Text("Save", color = Color(0xFF4CAF50)) }
                        },
                        dismissButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        androidx.compose.material3.TextButton(
                                                onClick = {
                                                        // Close without saving
                                                        fileToClose?.let { file ->
                                                                bufferViewModel.filesOpenedList
                                                                        .remove(file)
                                                                bufferViewModel.bufferLine.remove(
                                                                        file.uri.toString()
                                                                )
                                                                bufferViewModel.dirtyFiles.remove(
                                                                        file.uri.toString()
                                                                )

                                                                if (file.uri.toString() ==
                                                                                fileViewModel
                                                                                        .currentFilePath
                                                                ) {
                                                                        val nextFile =
                                                                                bufferViewModel
                                                                                        .filesOpenedList
                                                                                        .firstOrNull()
                                                                        if (nextFile != null) {
                                                                                fileViewModel
                                                                                        .selectedUri =
                                                                                        nextFile.uri
                                                                                fileViewModel
                                                                                        .currentFilePath =
                                                                                        nextFile.uri
                                                                                                .toString()
                                                                                val content =
                                                                                        bufferViewModel
                                                                                                .bufferLine[
                                                                                                nextFile.uri
                                                                                                        .toString()]
                                                                                                ?: ""
                                                                                                webView?.let { wv ->
                                                                                    loadContentWhenReady(
                                                                                            wv,
                                                                                            content
                                                                                    )
                                                                                }
                                                                        } else {
                                                                                fileViewModel
                                                                                        .selectedUri =
                                                                                        null
                                                                                fileViewModel
                                                                                        .currentFilePath =
                                                                                        null
                                                                                webView?.let { wv ->
                                                                                    loadContentWhenReady(
                                                                                            wv,
                                                                                            ""
                                                                                    )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                        showCloseConfirmation = false
                                                        fileToClose = null
                                                }
                                        ) {
                                                Text(
                                                        "Close Without Saving",
                                                        color = Color(0xFFF44336)
                                                )
                                        }

                                        androidx.compose.material3.TextButton(
                                                onClick = {
                                                        showCloseConfirmation = false
                                                        fileToClose = null
                                                }
                                        ) { Text("Cancel", color = Color(0xFFB0B0B0)) }
                                }
                        },
                        containerColor = Color(0xFF2B2B2B),
                        tonalElevation = 8.dp
                )
        }
}

@Composable
fun LeftTopBar(
        modifier: Modifier = Modifier,
        onSaveClick: () -> Unit = {},
        onRunClick: () -> Unit = {},
        onFileClick: () -> Unit = {},
        onUndoClick: () -> Unit = {},
        onRedoClick: () -> Unit = {},
        onSearchClick: () -> Unit = {},
        onBranchClick: () -> Unit = {},
        onMenuClick: () -> Unit = {},
        showMenuIcon: Boolean = false,
        showBranchButton: Boolean = true
) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val keyboardViewModel = LocalKeyboardViewModel.current
        val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
        
        // Scale bar height and icon sizes based on settings
        val baseHeight = if (isLandscape) 18.dp else 28.dp
        val barHeight = baseHeight * uiState.topBarScale
        val iconSize = 14.dp * uiState.topBarScale

        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
                                .height(barHeight)
                                .padding(start = 4.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxHeight().wrapContentWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Add menu icon for sidebar (portrait only)
                        if (showMenuIcon) {
                                IconButton(onClick = onMenuClick) {
                                        Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Toggle Sidebar",
                                                tint = Color(0xFF858585)
                                        )
                                }

                        }

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )
                        // Save button
                        TopBarIconButton(icon = R.drawable.save_icon, onClick = onSaveClick)

                        // Run button (play icon)
                        TopBarIconButton(icon = R.drawable.play_arrow, onClick = onRunClick)

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // File button - only show in landscape (in portrait, it's in the sidebar)
                        if (isLandscape) {
                                TopBarIconButton(
                                        icon = R.drawable.article_icon,
                                        onClick = onFileClick
                                )

                                VerticalDivider(
                                        modifier = Modifier.fillMaxHeight().width(1.dp),
                                        color = Color(0xFF3E3E42)
                                )
                        }

                        // Undo button (icon only)
                        TopBarIconButton(icon = R.drawable.undo_icon, onClick = onUndoClick)

                        // Redo button (icon only)
                        TopBarIconButton(icon = R.drawable.redo_icon, onClick = onRedoClick)

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // Search button - only show in landscape (in portrait, it's in the sidebar)
                        if (isLandscape) {
                                Box(
                                        modifier =
                                                Modifier.clickable { onSearchClick() }
                                                        .fillMaxHeight()
                                                        .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = Color(0xFFCCCCCC),
                                                modifier = Modifier.size(iconSize)
                                        )
                                }

                                VerticalDivider(
                                        modifier = Modifier.fillMaxHeight().width(1.dp),
                                        color = Color(0xFF3E3E42)
                                )
                        }

                        // Branch button (conditional)
                        if (showBranchButton) {
                                TopBarIconButton(
                                        icon = R.drawable.graph_icon,
                                        onClick = onBranchClick
                                )

                                VerticalDivider(
                                        modifier = Modifier.fillMaxHeight().width(1.dp),
                                        color = Color(0xFF3E3E42)
                                )
                        }
                }
        }
}

@Composable
private fun TopBarIconButton(icon: Int, onClick: () -> Unit, iconSize: androidx.compose.ui.unit.Dp = 14.dp) {
        Box(
                modifier =
                        Modifier.clickable { onClick() }.fillMaxHeight().padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
        ) {
                Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(iconSize)
                )
        }
}

/**
 * Same affordance for a vector icon. The AI toggle needs a tint that reflects on/off state, which
 * the drawable-backed overload above has no reason to carry.
 */
@Composable
private fun TopBarIconButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        tint: Color = Color(0xFFCCCCCC),
        iconSize: androidx.compose.ui.unit.Dp = 14.dp
) {
        Box(
                modifier =
                        Modifier.clickable { onClick() }.fillMaxHeight().padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = "AI assistant",
                        tint = tint,
                        modifier = Modifier.size(iconSize)
                )
        }
}

@Composable
fun RightTopBar(modifier: Modifier = Modifier, navController: NavController) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val context = LocalContext.current
        val browserViewModel = LocalBrowserViewModel.current
        val fileViewModel = LocalFileViewModel.current
        val bufferViewModel = LocalBufferViewModel.current
        val keyboardViewModel = LocalKeyboardViewModel.current
        val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
        val aiChatViewModel = LocalAiChatViewModel.current
        val aiPanelOpen by aiChatViewModel.isPanelOpen.collectAsStateWithLifecycle()

        // Scale bar height based on settings
        val baseHeight = if (isLandscape) 18.dp else 28.dp
        val barHeight = baseHeight * uiState.topBarScale

        // Helper to save content before navigation
        val currentEditorSession =
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as?
                        EditorSession

        // Package install dialog state
        var showPackageDialog by remember { mutableStateOf(false) }
        var missingPackage by remember { mutableStateOf("") }
        var pendingFilePath by remember { mutableStateOf<String?>(null) }

        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
                                .height(barHeight)
                                .padding(end = 4.dp),
                contentAlignment = Alignment.CenterEnd
        ) {
                Row(
                        modifier = Modifier.fillMaxHeight().wrapContentWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Session/ZoomOut button - only show in landscape (in portrait, it's in the sidebar)
                        if (isLandscape) {
                                VerticalDivider(
                                        modifier = Modifier.fillMaxHeight().width(1.dp),
                                        color = Color(0xFF3E3E42)
                                )

                                TopBarIconButton(
                                        icon = R.drawable.zoom_in,
                                        onClick = {
                                                currentEditorSession?.saveContent()
                                                navController.navigate(Screen.ZoomOut.route)
                                        }
                                )
                        }

                        // Browser button - only show in landscape (in portrait, it's in the sidebar)
                        if (isLandscape) {
                                VerticalDivider(
                                        modifier = Modifier.fillMaxHeight().width(1.dp),
                                        color = Color(0xFF3E3E42)
                                )

                                TopBarIconButton(
                                        icon = R.drawable.web_icon,
                                        onClick = {
                                                currentEditorSession?.saveContent()
                                                BrowserSession(context)
                                                browserViewModel.setDevWebView(
                                                        SessionList.sessions[
                                                                        SessionList.activeSessionIndex!!]
                                                                .view!!
                                                )
                                                navController.navigate(Screen.Browser.route)
                                        }
                                )
                        }

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // AI assistant toggle. Takes over the editor pane rather than opening a
                        // sheet, so it reads as a mode switch and is tinted while it is active.
                        TopBarIconButton(
                                icon = androidx.compose.material.icons.Icons.Outlined.AutoAwesome,
                                tint =
                                        if (aiPanelOpen) Color(0xFF007ACC)
                                        else Color(0xFFCCCCCC),
                                onClick = { aiChatViewModel.togglePanel() }
                        )

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // Back button
                        TopBarIconButton(
                                icon = R.drawable.back_logout,
                                onClick = {
                                        currentEditorSession?.saveContent()
                                        navController.popBackStack()
                                }
                        )

                        // Settings button - only show in landscape (in portrait, it's in the sidebar)
                        if (isLandscape) {
                                VerticalDivider(
                                        modifier = Modifier.fillMaxHeight().width(1.dp),
                                        color = Color(0xFF3E3E42)
                                )

                                TopBarIconButton(
                                        icon = R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                                        onClick = {
                                                currentEditorSession?.saveContent()
                                                navController.navigate(Screen.Settings.route)
                                        }
                                )
                        }
                }
        }

        // Package Install Dialog
        if (showPackageDialog) {
                androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                                showPackageDialog = false
                                missingPackage = ""
                                pendingFilePath = null
                        },
                        title = { Text("Missing Package", color = Color.White) },
                        text = {
                                Text(
                                        "Would you like to install '$missingPackage'? It is needed to run this code.",
                                        color = Color(0xFFCCCCCC)
                                )
                        },
                        confirmButton = {
                                androidx.compose.material3.TextButton(
                                        onClick = {
                                                showPackageDialog = false
                                                val pkgToInstall = missingPackage
                                                val fileToRun = pendingFilePath
                                                missingPackage = ""
                                                pendingFilePath = null

                                                // Install package and re-run
                                                currentEditorSession?.let { session ->
                                                        val (terminal, _) =
                                                                session.getOrCreateRunnerTerminal(
                                                                        context
                                                                )
                                                        android.util.Log.d(
                                                                "CodeRunner",
                                                                "Installing package: $pkgToInstall"
                                                        )

                                                        // Install then re-run
                                                        val installCmd =
                                                                "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y $pkgToInstall && clear && bash \$PREFIX/code-runner.sh \"$fileToRun\"\n"
                                                        terminal.insertText(installCmd)

                                                        android.widget.Toast.makeText(
                                                                        context,
                                                                        "Installing $pkgToInstall...",
                                                                        android.widget.Toast
                                                                                .LENGTH_SHORT
                                                                )
                                                                .show()
                                                }
                                        }
                                ) { Text("Install", color = Color(0xFF4CAF50)) }
                        },
                        dismissButton = {
                                androidx.compose.material3.TextButton(
                                        onClick = {
                                                showPackageDialog = false
                                                missingPackage = ""
                                                pendingFilePath = null
                                        }
                                ) { Text("Cancel", color = Color(0xFFFF5252)) }
                        },
                        containerColor = Color(0xFF2D2D2D),
                        tonalElevation = 8.dp
                )
        }
}

@Composable
fun LeftTermTopBar(modifier: Modifier = Modifier) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val keyboardViewModel = LocalKeyboardViewModel.current
        val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
        
        // Scale bar height based on settings
        val baseHeight = if (isLandscape) 18.dp else 28.dp
        val barHeight = baseHeight * uiState.topBarScale

        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
                                .fillMaxWidth()
                                .height(barHeight)
                                .padding(start = 20.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                ) {}
        }
}

/**
 * Terminal Sidebar - Simplified sidebar for terminal portrait mode
 * Only contains ZoomOut, Browser, and Settings buttons
 */
@Composable
fun TerminalSidebar(
        isVisible: Boolean,
        onDismiss: () -> Unit,
        onZoomOutClick: () -> Unit,
        onSettingsClick: () -> Unit
) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val sidebarWidth = screenWidth * 0.75f

        // Animated offset for slide-in effect
        val offsetX by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isVisible) 0.dp else -sidebarWidth,
                animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                label = "sidebarOffset"
        )

        // Only render when visible or animating
        if (isVisible || offsetX > -sidebarWidth) {
                Box(modifier = Modifier.fillMaxSize()) {
                        // Scrim (background overlay)
                        if (isVisible) {
                                Box(
                                        modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .clickable { onDismiss() }
                                )
                        }

                        // Sidebar panel
                        Column(
                                modifier = Modifier
                                        .offset(x = offsetX)
                                        .width(sidebarWidth)
                                        .fillMaxHeight()
                                        .background(Color(0xFF1E1E1E))
                                        .padding(top = 40.dp) // Account for status bar
                        ) {
                                // Header
                                Box(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF2D2D30))
                                                .padding(16.dp)
                                ) {
                                        Text(
                                                "Quick Actions",
                                                color = Color.White,
                                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                        )
                                }

                                androidx.compose.material3.HorizontalDivider(
                                        thickness = 1.dp,
                                        color = Color(0xFF2D2D2D)
                                )

                                // Action buttons
                                Column(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        // ZoomOut / Sessions button
                                        TerminalSidebarButton(
                                                icon = R.drawable.zoom_in,
                                                label = "Sessions",
                                                onClick = {
                                                        onZoomOutClick()
                                                        onDismiss()
                                                }
                                        )

                                        // Settings button
                                        TerminalSidebarButton(
                                                icon = R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                                                label = "Settings",
                                                onClick = {
                                                        onSettingsClick()
                                                        onDismiss()
                                                }
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun TerminalSidebarButton(
        icon: Int,
        label: String,
        onClick: () -> Unit
) {
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2D2D30))
                        .clickable { onClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
        ) {
                Icon(
                        painter = painterResource(icon),
                        contentDescription = label,
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                        text = label,
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                )
        }
}

/**
 * Terminal Portrait Top Bar - Simplified top bar for terminal in portrait mode
 * Has menu button on left and exit button on right
 */
@Composable
fun TerminalPortraitTopBar(
        modifier: Modifier = Modifier, 
        onMenuClick: () -> Unit,
        navController: NavController
) {
        val keyboardViewModel = LocalKeyboardViewModel.current
        val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
        
        // Scale bar height based on settings
        val barHeight = 28.dp * uiState.topBarScale
        
        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
                                .fillMaxWidth()
                                .height(barHeight)
                                .padding(horizontal = 4.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Menu button (left side)
                        Row(
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                IconButton(onClick = onMenuClick) {
                                        Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Toggle Sidebar",
                                                tint = Color(0xFF858585)
                                        )
                                }
                        }
                        
                        // Exit button (right side)
                        Row(
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                TopBarIconButton(
                                        icon = R.drawable.back_logout,
                                        onClick = {
                                                navController.popBackStack()
                                        }
                                )
                        }
                }
        }
}

/** Convert a content URI to a real file path. Works with SAF (Storage Access Framework) URIs. */
internal fun getPathFromUri(context: Context, uri: android.net.Uri): String? {
        android.util.Log.d("CodeRunner", "getPathFromUri: $uri")

        // If it's already a file path
        if (uri.scheme == "file") {
                return uri.path
        }

        // For content URIs, try to extract the path from the document ID
        if (uri.scheme == "content") {
                // Check if it's a document URI
                val docId =
                        try {
                                android.provider.DocumentsContract.getDocumentId(uri)
                        } catch (e: Exception) {
                                try {
                                        android.provider.DocumentsContract.getTreeDocumentId(uri)
                                } catch (e2: Exception) {
                                        null
                                }
                        }

                android.util.Log.d("CodeRunner", "Document ID: $docId")

                if (docId != null) {
                        // Common format: "primary:path/to/folder" or "raw:/storage/emulated/0/path"
                        val split = docId.split(":")
                        if (split.size >= 2) {
                                val type = split[0]
                                val path = split.subList(1, split.size).joinToString(":")

                                return when {
                                        type == "primary" -> "/storage/emulated/0/$path"
                                        type == "raw" -> path
                                        type.startsWith("/") -> docId // Already a path
                                        else -> "/storage/$type/$path" // Try volume path
                                }
                        } else if (docId.startsWith("/")) {
                                return docId
                        }
                }

                // Fallback: try to get the path directly
                uri.path?.let { path ->
                        // SAF URIs often have format: /tree/primary:xxx/document/primary:xxx
                        val primaryMatch = Regex("primary:(.+)").find(path)
                        if (primaryMatch != null) {
                                return "/storage/emulated/0/${primaryMatch.groupValues[1]}"
                        }
                }
        }

        android.util.Log.w("CodeRunner", "Could not extract path from URI: $uri")
        return null
}

@Composable
private fun RemoteBufferCard(
        path: String,
        isSelected: Boolean,
        isDirty: Boolean,
        onClick: () -> Unit,
        onClose: () -> Unit
) {
        val fileName = path.substringAfterLast("/").ifEmpty { path }
        val context = LocalContext.current
        androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                        .clickable(onClick = onClick)
                        .background(Color(0xFF1B1B1B))
        ) {
                Row(
                        modifier = androidx.compose.ui.Modifier
                                .height(28.dp)
                                .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        AsyncImage(
                                model = FileIconUtils.getIconPath(fileName),
                                imageLoader = ImageLoaderSingleton.get(context),
                                contentDescription = null,
                                modifier = androidx.compose.ui.Modifier.size(16.dp)
                        )

                        androidx.compose.material3.Text(
                                text = fileName,
                                color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = androidx.compose.ui.Modifier.widthIn(max = 150.dp)
                        )

                        if (isDirty) {
                                Box(
                                        modifier = androidx.compose.ui.Modifier
                                                .size(16.dp)
                                                .padding(4.dp)
                                                .background(
                                                        Color.White,
                                                        androidx.compose.foundation.shape.CircleShape
                                                )
                                                .clickable(onClick = onClose)
                                )
                        } else {
                                androidx.compose.material3.IconButton(
                                        onClick = onClose,
                                        modifier = androidx.compose.ui.Modifier.size(16.dp)
                                ) {
                                        androidx.compose.material3.Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = if (isSelected) Color(0xFFB0B0B0)
                                                else Color(0xFF6A6A6A),
                                                modifier = androidx.compose.ui.Modifier.size(10.dp)
                                        )
                                }
                        }
                }

                if (isSelected) {
                        androidx.compose.material3.HorizontalDivider(
                                modifier = androidx.compose.ui.Modifier
                                        .widthIn(min = 20.dp)
                                        .height(1.dp),
                                color = Color(0xFF4379FF)
                        )
                }
        }
}
