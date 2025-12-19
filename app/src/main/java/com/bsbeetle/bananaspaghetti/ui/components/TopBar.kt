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
import androidx.compose.material.icons.filled.Search
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
import com.bsbeetle.bananaspaghetti.utils.fileHandling.saveFile
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
        onSearchClick: () -> Unit = {},
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

                        // Search button
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
                                        modifier = Modifier.size(14.dp)
                                )
                        }

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
        val bufferViewModel = LocalBufferViewModel.current

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

                        // Play button - Unified Code Runner
                        TopBarIconButton(
                                icon = R.drawable.play_arrow,
                                onClick = {
                                        android.util.Log.d("CodeRunner", "Play button clicked")

                                        // Get active editor session dynamically (not cached)
                                        val activeEditorSession =
                                                SessionList.sessions.getOrNull(
                                                        SessionList.activeSessionIndex ?: -1
                                                ) as?
                                                        EditorSession

                                        // Save file to disk before running (not just memory)
                                        saveFile(fileViewModel, bufferViewModel, context)

                                        // Get current file info
                                        val currentFileUri = fileViewModel.selectedUri
                                        if (currentFileUri == null) {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "No file selected",
                                                                android.widget.Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                android.util.Log.d("CodeRunner", "No file selected")
                                                return@TopBarIconButton
                                        }

                                        // Convert URI to file path
                                        val currentFilePath =
                                                getPathFromUri(context, currentFileUri)
                                        if (currentFilePath == null) {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "Could not resolve file path",
                                                                android.widget.Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                android.util.Log.e(
                                                        "CodeRunner",
                                                        "Could not resolve path for: $currentFileUri"
                                                )
                                                return@TopBarIconButton
                                        }

                                        android.util.Log.d(
                                                "CodeRunner",
                                                "Running file: $currentFilePath"
                                        )

                                        // Get file extension
                                        val fileName = currentFilePath.substringAfterLast("/")
                                        val extension = fileName.substringAfterLast(".").lowercase()
                                        android.util.Log.d("CodeRunner", "Extension: $extension")

                                        if (activeEditorSession == null) {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "No active editor session",
                                                                android.widget.Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                return@TopBarIconButton
                                        }

                                        // Get or create runner terminal (returns Pair with wasNew
                                        // flag)
                                        val (runnerTerminal, wasNew) =
                                                activeEditorSession.getOrCreateRunnerTerminal(
                                                        context
                                                )
                                        val isRerun = !wasNew

                                        android.util.Log.d(
                                                "CodeRunner",
                                                "Runner terminal: ${runnerTerminal.name}, wasNew: $wasNew, isRerun: $isRerun"
                                        )

                                        // Show toast
                                        android.widget.Toast.makeText(
                                                        context,
                                                        "Running $fileName...",
                                                        android.widget.Toast.LENGTH_SHORT
                                                )
                                                .show()

                                        // First run needs longer delay for terminal to init; rerun
                                        // is quick
                                        val initDelay = if (isRerun) 100L else 1500L

                                        android.util.Log.d(
                                                "CodeRunner",
                                                "Is rerun: $isRerun, delay: $initDelay"
                                        )

                                        // Wait for terminal to initialize (or short delay for
                                        // rerun)
                                        android.os.Handler(android.os.Looper.getMainLooper())
                                                .postDelayed(
                                                        {
                                                                // Set up output listener to detect
                                                                // missing packages
                                                                runnerTerminal.onOutput =
                                                                        { output ->
                                                                                if (output.contains(
                                                                                                "MISSING_PKG:"
                                                                                        )
                                                                                ) {
                                                                                        val pkg =
                                                                                                output.substringAfter(
                                                                                                                "MISSING_PKG:"
                                                                                                        )
                                                                                                        .substringBefore(
                                                                                                                "\n"
                                                                                                        )
                                                                                                        .trim()
                                                                                        android.util
                                                                                                .Log
                                                                                                .d(
                                                                                                        "CodeRunner",
                                                                                                        "Missing package detected: $pkg"
                                                                                                )
                                                                                        if (pkg.isNotEmpty()
                                                                                        ) {
                                                                                                android.os
                                                                                                        .Handler(
                                                                                                                android.os
                                                                                                                        .Looper
                                                                                                                        .getMainLooper()
                                                                                                        )
                                                                                                        .post {
                                                                                                                missingPackage =
                                                                                                                        pkg
                                                                                                                pendingFilePath =
                                                                                                                        currentFilePath
                                                                                                                showPackageDialog =
                                                                                                                        true
                                                                                                        }
                                                                                        }
                                                                                }
                                                                        }

                                                                // For reruns: send multiple Ctrl+C
                                                                // to ensure process is killed
                                                                // and reset terminal state
                                                                if (isRerun) {
                                                                        android.util.Log.d(
                                                                                "CodeRunner",
                                                                                "Sending multiple Ctrl+C for rerun"
                                                                        )
                                                                        // Send Ctrl+C 3 times with
                                                                        // newlines to ensure we
                                                                        // break out of any state
                                                                        runnerTerminal.insertText(
                                                                                "\u0003\n\u0003\n\u0003\n"
                                                                        )
                                                                }

                                                                // Delay then send command
                                                                val cmdDelay =
                                                                        if (isRerun) 500L else 100L
                                                                android.os.Handler(
                                                                                android.os.Looper
                                                                                        .getMainLooper()
                                                                        )
                                                                        .postDelayed(
                                                                                {
                                                                                        // For
                                                                                        // rerun,
                                                                                        // reset
                                                                                        // terminal
                                                                                        // state
                                                                                        // first
                                                                                        val command =
                                                                                                if (isRerun
                                                                                                ) {
                                                                                                        "stty sane; clear; bash \$PREFIX/code-runner.sh \"$currentFilePath\"\n"
                                                                                                } else {
                                                                                                        "clear; bash \$PREFIX/code-runner.sh \"$currentFilePath\"\n"
                                                                                                }
                                                                                        android.util
                                                                                                .Log
                                                                                                .d(
                                                                                                        "CodeRunner",
                                                                                                        "Sending command: $command"
                                                                                                )
                                                                                        runnerTerminal
                                                                                                .insertText(
                                                                                                        command
                                                                                                )
                                                                                },
                                                                                cmdDelay
                                                                        )

                                                                // For web files (html, jsx, tsx),
                                                                // open browser after delay
                                                                val webExtensions =
                                                                        setOf(
                                                                                "html",
                                                                                "htm",
                                                                                "jsx",
                                                                                "tsx"
                                                                        )
                                                                if (extension in webExtensions) {
                                                                        android.os.Handler(
                                                                                        android.os
                                                                                                .Looper
                                                                                                .getMainLooper()
                                                                                )
                                                                                .postDelayed(
                                                                                        {
                                                                                                // Detect if HTML is in a React/Vite project by checking for package.json
                                                                                                val port =
                                                                                                        if (extension in
                                                                                                                        setOf(
                                                                                                                                "jsx",
                                                                                                                                "tsx"
                                                                                                                        )
                                                                                                        ) {
                                                                                                                // JSX/TSX always use dev server
                                                                                                                5173
                                                                                                        } else {
                                                                                                                // For HTML, check if it's in a React/Vite project
                                                                                                                val fileDir =
                                                                                                                        java.io
                                                                                                                                .File(
                                                                                                                                        currentFilePath
                                                                                                                                )
                                                                                                                                .parentFile
                                                                                                                var checkDir =
                                                                                                                        fileDir
                                                                                                                var isReactProject =
                                                                                                                        false

                                                                                                                // Look up to 5 directories for package.json
                                                                                                                repeat(
                                                                                                                        5
                                                                                                                ) {
                                                                                                                        if (checkDir !=
                                                                                                                                        null
                                                                                                                        ) {
                                                                                                                                val pkgJson =
                                                                                                                                        java.io
                                                                                                                                                .File(
                                                                                                                                                        checkDir,
                                                                                                                                                        "package.json"
                                                                                                                                                )
                                                                                                                                if (pkgJson.exists()
                                                                                                                                ) {
                                                                                                                                        val content =
                                                                                                                                                try {
                                                                                                                                                        pkgJson.readText()
                                                                                                                                                } catch (
                                                                                                                                                        e:
                                                                                                                                                                Exception) {
                                                                                                                                                        ""
                                                                                                                                                }
                                                                                                                                        if (content.contains(
                                                                                                                                                        "\"react\""
                                                                                                                                                ) ||
                                                                                                                                                        content.contains(
                                                                                                                                                                "\"vite\""
                                                                                                                                                        ) ||
                                                                                                                                                        content.contains(
                                                                                                                                                                "\"next\""
                                                                                                                                                        )
                                                                                                                                        ) {
                                                                                                                                                isReactProject =
                                                                                                                                                        true
                                                                                                                                                android.util
                                                                                                                                                        .Log
                                                                                                                                                        .d(
                                                                                                                                                                "CodeRunner",
                                                                                                                                                                "Detected React project for browser"
                                                                                                                                                        )
                                                                                                                                        }
                                                                                                                                }
                                                                                                                                checkDir =
                                                                                                                                        checkDir.parentFile
                                                                                                                        }
                                                                                                                }

                                                                                                                if (isReactProject
                                                                                                                )
                                                                                                                        5173
                                                                                                                else
                                                                                                                        5500
                                                                                                        }
                                                                                                val urlToOpen =
                                                                                                        "http://localhost:$port"
                                                                                                android.util
                                                                                                        .Log
                                                                                                        .d(
                                                                                                                "CodeRunner",
                                                                                                                "Opening browser: $urlToOpen"
                                                                                                        )

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
                                                                                        3000
                                                                                ) // Wait 3s for
                                                                        // server to start
                                                                }
                                                        },
                                                        1500
                                                ) // Wait 1.5s for terminal initialization
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
                                                                "apk add $pkgToInstall && clear && bash \$PREFIX/code-runner.sh \"$fileToRun\"\n"
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

/** Convert a content URI to a real file path. Works with SAF (Storage Access Framework) URIs. */
private fun getPathFromUri(context: Context, uri: android.net.Uri): String? {
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
