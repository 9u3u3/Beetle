package com.bsbeetle.bananaspaghetti.ui.components

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import com.bsbeetle.bananaspaghetti.LocalBrowserViewModel
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.fileHandling.readFromFile
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.BrowserSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TopBar(modifier: Modifier = Modifier, context: Context) {
        val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
        val editorViewModel: EditorViewModel = LocalEditorViewModel.current
        val fileViewModel: FileViewModel = LocalFileViewModel.current
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val webView = editorViewModel.webView
        val coroutineScope = rememberCoroutineScope()

        // State for unsaved file close confirmation
        var fileToClose by remember { mutableStateOf<DocumentFile?>(null) }
        var showCloseConfirmation by remember { mutableStateOf(false) }

        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
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
                                                                loadContentWhenReady(
                                                                        webView!!,
                                                                        bufferedContent
                                                                )
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
                                                                loadContentWhenReady(
                                                                        webView!!,
                                                                        content
                                                                )
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
                                                                        loadContentWhenReady(
                                                                                webView!!,
                                                                                content
                                                                        )
                                                                } else {
                                                                        fileViewModel.selectedUri =
                                                                                null
                                                                        fileViewModel
                                                                                .currentFilePath =
                                                                                null
                                                                        loadContentWhenReady(
                                                                                webView!!,
                                                                                ""
                                                                        )
                                                                }
                                                        }
                                                }
                                        },
                                        context = context
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
                                                                        loadContentWhenReady(
                                                                                webView!!,
                                                                                content
                                                                        )
                                                                } else {
                                                                        fileViewModel.selectedUri =
                                                                                null
                                                                        fileViewModel
                                                                                .currentFilePath =
                                                                                null
                                                                        loadContentWhenReady(
                                                                                webView!!,
                                                                                ""
                                                                        )
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
                                                                                loadContentWhenReady(
                                                                                        webView!!,
                                                                                        content
                                                                                )
                                                                        } else {
                                                                                fileViewModel
                                                                                        .selectedUri =
                                                                                        null
                                                                                fileViewModel
                                                                                        .currentFilePath =
                                                                                        null
                                                                                loadContentWhenReady(
                                                                                        webView!!,
                                                                                        ""
                                                                                )
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
        onFileClick: () -> Unit = {},
        onUndoClick: () -> Unit = {},
        onRedoClick: () -> Unit = {},
        onBranchClick: () -> Unit = {},
        onMenuClick: () -> Unit = {},
        showMenuIcon: Boolean = false,
        showBranchButton: Boolean = true
) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
                                .height(if (isLandscape) 18.dp else 28.dp)
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

                                VerticalDivider(
                                        modifier = Modifier.fillMaxHeight().width(1.dp),
                                        color = Color(0xFF3E3E42)
                                )
                        }

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )
                        // Save button
                        TopBarIconButton(icon = R.drawable.save_icon, onClick = onSaveClick)

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // File button
                        TopBarIconButton(
                                // title = "File",
                                icon = R.drawable.article_icon,
                                onClick = onFileClick
                        )

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // Undo button (icon only)
                        TopBarIconButton(icon = R.drawable.undo_icon, onClick = onUndoClick)

                        // Redo button (icon only)
                        TopBarIconButton(icon = R.drawable.redo_icon, onClick = onRedoClick)

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

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
private fun TopBarIconButton(icon: Int, onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.clickable { onClick() }.fillMaxHeight().padding(horizontal = 4.dp),
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
fun RightTopBar(modifier: Modifier = Modifier, navController: NavController) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val context = LocalContext.current
        val browserViewModel = LocalBrowserViewModel.current
        val fileViewModel = LocalFileViewModel.current

        // State for live server
        var isServerRunning by remember {
                mutableStateOf(
                        com.bsbeetle.bananaspaghetti.liveserver.LiveServerManager.isRunning()
                )
        }

        // Helper to save content before navigation
        val currentEditorSession =
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as?
                        EditorSession

        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
                                .height(if (isLandscape) 18.dp else 28.dp)
                                .padding(end = 4.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxHeight().wrapContentWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // Session/Fullscreen button
                        TopBarIconButton(
                                icon = R.drawable.zoom_in,
                                onClick = {
                                        currentEditorSession?.saveContent()
                                        navController.navigate(Screen.ZoomOut.route)
                                }
                        )

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // Play button - Live Server
                        TopBarIconButton(
                                icon = R.drawable.play_arrow,
                                onClick = {
                                        currentEditorSession?.saveContent()

                                        // Get current file info
                                        val currentFileUri = fileViewModel.selectedUri
                                        val currentFilePath =
                                                currentFileUri?.let { uri ->
                                                        com.bsbeetle.bananaspaghetti.liveserver
                                                                .LiveServerManager.getPathFromUri(
                                                                context,
                                                                uri
                                                        )
                                                }

                                        // Get file name from path
                                        val currentFileName =
                                                currentFilePath?.substringAfterLast("/")

                                        // Get parent directory of current file
                                        val fileParentDir =
                                                currentFilePath?.substringBeforeLast("/")

                                        // Get project root - try multiple methods
                                        val projectRoot = run {
                                                // Method 1: Use the open folder from sidebar
                                                fileViewModel.selectedFolderUri?.let { folderUri ->
                                                        com.bsbeetle.bananaspaghetti.liveserver
                                                                .LiveServerManager.getPathFromUri(
                                                                context,
                                                                folderUri
                                                        )
                                                }
                                                // Method 2: Editor session project root
                                                ?: currentEditorSession?.projectRoot?.uri?.let { uri
                                                                ->
                                                                com.bsbeetle.bananaspaghetti
                                                                        .liveserver
                                                                        .LiveServerManager
                                                                        .getPathFromUri(
                                                                                context,
                                                                                uri
                                                                        )
                                                        }
                                                        // Method 3: Use parent directory of current
                                                        // file
                                                        ?: fileParentDir
                                        }

                                        if (projectRoot == null) {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "Could not find file location",
                                                                android.widget.Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                return@TopBarIconButton
                                        }

                                        // Find actual project root by searching upward for
                                        // package.json or index.html
                                        val actualProjectRoot =
                                                com.bsbeetle.bananaspaghetti.liveserver
                                                        .LiveServerManager.findProjectRoot(
                                                        projectRoot
                                                )

                                        // Determine entry file - use current file if it's HTML,
                                        // otherwise null
                                        val entryFile =
                                                if (currentFileName?.endsWith(".html", true) ==
                                                                true ||
                                                                currentFileName?.endsWith(
                                                                        ".htm",
                                                                        true
                                                                ) == true
                                                ) {
                                                        currentFileName
                                                } else {
                                                        null // Will use index.html
                                                }

                                        // Start live server
                                        val result =
                                                com.bsbeetle.bananaspaghetti.liveserver
                                                        .LiveServerManager.startServer(
                                                        context,
                                                        actualProjectRoot,
                                                        entryFile
                                                )

                                        if (result.success) {
                                                isServerRunning = true

                                                // For React projects, automatically run the dev
                                                // server
                                                if (result.reactDevPort != null) {
                                                        val devPort = result.reactDevPort
                                                        val devCommand =
                                                                if (devPort == 5173) "npm run dev"
                                                                else "npm start"

                                                        // Show starting message
                                                        android.widget.Toast.makeText(
                                                                        context,
                                                                        "Starting dev server...",
                                                                        android.widget.Toast
                                                                                .LENGTH_SHORT
                                                                )
                                                                .show()

                                                        // Create a terminal session in the project
                                                        // directory
                                                        val termSession =
                                                                com.bsbeetle.bananaspaghetti.utils
                                                                        .sessionHandling
                                                                        .TerminalSession(
                                                                                context,
                                                                                "Dev Server",
                                                                                actualProjectRoot
                                                                        )
                                                        termSession.createView()

                                                        // Wait a bit for terminal to initialize,
                                                        // then run npm command
                                                        android.os.Handler(
                                                                        android.os.Looper
                                                                                .getMainLooper()
                                                                )
                                                                .postDelayed(
                                                                        {
                                                                                // Send cd command
                                                                                // first (in case
                                                                                // initialDir didn't
                                                                                // work)
                                                                                termSession
                                                                                        .insertText(
                                                                                                "cd \"$actualProjectRoot\" && $devCommand\n"
                                                                                        )

                                                                                // Wait for server
                                                                                // to start, then
                                                                                // open browser
                                                                                android.os.Handler(
                                                                                                android.os
                                                                                                        .Looper
                                                                                                        .getMainLooper()
                                                                                        )
                                                                                        .postDelayed(
                                                                                                {
                                                                                                        android.widget
                                                                                                                .Toast
                                                                                                                .makeText(
                                                                                                                        context,
                                                                                                                        "Dev server started! Opening browser...",
                                                                                                                        android.widget
                                                                                                                                .Toast
                                                                                                                                .LENGTH_SHORT
                                                                                                                )
                                                                                                                .show()

                                                                                                        // Open browser to dev server
                                                                                                        val urlToOpen =
                                                                                                                "http://localhost:$devPort"
                                                                                                        BrowserSession(
                                                                                                                context
                                                                                                        )
                                                                                                        browserViewModel
                                                                                                                .setDevWebView(
                                                                                                                        SessionList
                                                                                                                                        .sessions[
                                                                                                                                        SessionList
                                                                                                                                                .activeSessionIndex!!]
                                                                                                                                .view!!
                                                                                                                )
                                                                                                        browserViewModel
                                                                                                                .loadUrl(
                                                                                                                        urlToOpen
                                                                                                                )
                                                                                                        navController
                                                                                                                .navigate(
                                                                                                                        Screen.Browser
                                                                                                                                .route
                                                                                                                )
                                                                                                },
                                                                                                5000
                                                                                        ) // Wait 5
                                                                                // seconds
                                                                                // for dev
                                                                                // server
                                                                                // to
                                                                                // start
                                                                        },
                                                                        2000
                                                                ) // Wait 2 seconds for terminal to
                                                        // initialize

                                                        return@TopBarIconButton
                                                }

                                                // Show message for vanilla HTML
                                                android.widget.Toast.makeText(
                                                                context,
                                                                result.message,
                                                                android.widget.Toast.LENGTH_SHORT
                                                        )
                                                        .show()

                                                // Get URL to open (only for vanilla HTML)
                                                val urlToOpen =
                                                        result.serverUrl ?: "http://localhost:5500"

                                                // Create browser session and navigate
                                                BrowserSession(context)
                                                browserViewModel.setDevWebView(
                                                        SessionList.sessions[
                                                                        SessionList
                                                                                .activeSessionIndex!!]
                                                                .view!!
                                                )
                                                browserViewModel.loadUrl(urlToOpen)
                                                navController.navigate(Screen.Browser.route)
                                        } else {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                result.message,
                                                                android.widget.Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                        }
                                }
                        )

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // Browser button
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

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )

                        // Settings button
                        TopBarIconButton(
                                icon = R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                                onClick = {
                                        currentEditorSession?.saveContent()
                                        navController.navigate(Screen.Settings.route)
                                }
                        )

                        VerticalDivider(
                                modifier = Modifier.fillMaxHeight().width(1.dp),
                                color = Color(0xFF3E3E42)
                        )
                }
        }
}

@Composable
fun LeftTermTopBar(modifier: Modifier = Modifier) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Box(
                modifier =
                        modifier.background(Color(0xFF1B1B1B))
                                .fillMaxWidth()
                                .height(if (isLandscape) 18.dp else 28.dp)
                                .padding(start = 20.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                ) {}
        }
}
