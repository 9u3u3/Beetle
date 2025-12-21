package com.bsbeetle.bananaspaghetti.ui.screens.sessions

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.fileHandling.fileLauncher
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.BrowserSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.DemoSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SessionViewModel

// NO_PORTRAIT

@Composable
fun ZoomOut(navController: NavController, viewModel: SessionViewModel = viewModel()) {
        val fileViewModel: FileViewModel = LocalFileViewModel.current
        val bufferViewModel: com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel =
                com.bsbeetle.bananaspaghetti.LocalBufferViewModel.current
        val editorViewModel: com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel =
                com.bsbeetle.bananaspaghetti.LocalEditorViewModel.current
        var showNewSessionDialog by remember { mutableStateOf(false) }
        var showEditorMenu by remember { mutableStateOf(false) }
        var showNotebookMenu by remember { mutableStateOf(false) }
        var pendingEditorName by remember { mutableStateOf("Editor") }
        var pendingNotebookName by remember { mutableStateOf("Notebook") }
        val context = LocalContext.current
        val launcher = fileLauncher(context, navController)

        // Notebook file launcher - creates NotebookSession instead of EditorSession
        val notebookLauncher =
                rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                                val uri: android.net.Uri? = result.data?.data
                                if (uri != null) {
                                        val flags =
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        context.contentResolver.takePersistableUriPermission(
                                                uri,
                                                flags
                                        )

                                        // Reset file selection state for new notebook (don't clear
                                        // buffer - session
                                        // switch handles it)
                                        fileViewModel.selectedUri = null
                                        fileViewModel.currentFilePath = ""

                                        if (android.provider.DocumentsContract.isDocumentUri(
                                                        context,
                                                        uri
                                                )
                                        ) {
                                                // File selected - create notebook session with file
                                                val fileName =
                                                        com.bsbeetle.bananaspaghetti.utils
                                                                .fileHandling.getFileName(
                                                                context,
                                                                uri
                                                        )
                                                fileViewModel.selectedUri = uri
                                                fileViewModel.selectedFolderUri = null

                                                val session =
                                                        com.bsbeetle.bananaspaghetti.utils
                                                                .sessionHandling.NotebookSession(
                                                                context.applicationContext,
                                                                fileName
                                                        )
                                                session.setFilePath(uri.toString())
                                                session.id?.let { id ->
                                                        bufferViewModel.setSessionId(id)
                                                }

                                                navController.navigate(Screen.Notebook.route) {
                                                        popUpTo(Screen.ZoomOut.route) {
                                                                inclusive = true
                                                        }
                                                }
                                        } else {
                                                // Folder selected - create notebook session with
                                                // folder
                                                fileViewModel.selectedFolderUri = uri
                                                val folderDoc =
                                                        androidx.documentfile.provider.DocumentFile
                                                                .fromTreeUri(context, uri)

                                                val session =
                                                        com.bsbeetle.bananaspaghetti.utils
                                                                .sessionHandling.NotebookSession(
                                                                context.applicationContext,
                                                                folderDoc?.name ?: "Notebook"
                                                        )
                                                session.projectRoot = folderDoc
                                                // Initialize WebView early so page starts loading
                                                // before navigation
                                                session.getOrInitView()
                                                session.id?.let { id ->
                                                        bufferViewModel.setSessionId(id)
                                                }

                                                navController.navigate(Screen.Notebook.route) {
                                                        popUpTo(Screen.ZoomOut.route) {
                                                                inclusive = true
                                                        }
                                                }
                                        }
                                }
                        }
                }

        // Capture snapshots when screen is shown
        LaunchedEffect(Unit) { viewModel.captureAllSnapshots() }

        BackHandler {
                // Always go to Home when pressing back from ZoomOut
                navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                Column(modifier = Modifier.fillMaxSize()) {
                        // Top bar
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Settings button removed

                                IconButton(
                                        onClick = { showNewSessionDialog = true },
                                        modifier = Modifier.size(36.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Session",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                        }

                        // Session Grid - 2 columns, larger cards
                        LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                        ) {
                                // Filter out internal sessions like PythonREPL-Headless
                                val visibleSessions =
                                        SessionList.sessions.filter { session ->
                                                !session.name.contains(
                                                        "Headless",
                                                        ignoreCase = true
                                                ) &&
                                                        !session.name.contains(
                                                                "Runner-",
                                                                ignoreCase = true
                                                        )
                                        }
                                items(visibleSessions) { session ->
                                        SessionCard(
                                                session = session,
                                                snapshot = viewModel.sessionSnapshots[session.id],
                                                onClick = {
                                                        // Switch to this session's buffer (each
                                                        // session has its own file
                                                        // list)
                                                        if (session is EditorSession ||
                                                                        session is
                                                                                com.bsbeetle.bananaspaghetti.utils.sessionHandling.NotebookSession
                                                        ) {
                                                                session.id?.let {
                                                                        bufferViewModel
                                                                                .setSessionId(it)
                                                                }
                                                                fileViewModel.selectedUri = null
                                                                fileViewModel.currentFilePath = ""

                                                                // CRITICAL: Update
                                                                // editorViewModel's webView to this
                                                                // session's
                                                                // webView
                                                                // This fixes the issue where only
                                                                // the latest session would work
                                                                editorViewModel.setEditorWebView(
                                                                        session.getOrInitView()
                                                                )
                                                        }
                                                        SessionList.activeSessionIndex =
                                                                SessionList.sessions.indexOf(
                                                                        session
                                                                )
                                                        // Sync file tree with session's folder when
                                                        // switching to
                                                        // EditorSession
                                                        if (session is EditorSession) {
                                                                session.projectRoot?.let {
                                                                        projectRoot ->
                                                                        fileViewModel
                                                                                .selectedFolderUri =
                                                                                projectRoot.uri
                                                                }
                                                        }
                                                        navigateToSession(navController, session)
                                                },
                                                onClose = {
                                                        // Clear buffer for the specific session
                                                        // being closed
                                                        if (session is EditorSession) {
                                                                session.id?.let { id ->
                                                                        bufferViewModel
                                                                                .clearSessionBuffer(
                                                                                        id,
                                                                                        SessionType
                                                                                                .EDITOR
                                                                                )
                                                                }
                                                                fileViewModel.selectedUri = null
                                                                fileViewModel.currentFilePath = ""
                                                        }
                                                        session.destroy()
                                                        viewModel.removeSnapshot(session.id)
                                                }
                                        )
                                }
                        }
                }

                // New Session Dialog
                if (showNewSessionDialog) {
                        NewSessionDialog(
                                onDismiss = { showNewSessionDialog = false },
                                onCreateSession = { sessionType, sessionName ->
                                        // Create new session
                                        val finalName =
                                                if (sessionName.isBlank())
                                                        sessionType.name.lowercase()
                                                                .replaceFirstChar { it.uppercase() }
                                                else sessionName
                                        val appContext = navController.context.applicationContext

                                        // Reset file selection state for new editor (don't clear
                                        // buffer - session
                                        // switch handles it)
                                        if (sessionType == SessionType.EDITOR) {
                                                fileViewModel.selectedUri = null
                                                fileViewModel.currentFilePath = ""
                                        }

                                        when (sessionType) {
                                                SessionType.EDITOR -> {
                                                        showNewSessionDialog = false
                                                        showEditorMenu = true
                                                        pendingEditorName = finalName
                                                }
                                                SessionType.NOTEBOOK -> {
                                                        // Show file options menu for Notebook (like
                                                        // Editor)
                                                        showNewSessionDialog = false
                                                        showNotebookMenu = true
                                                        pendingNotebookName = finalName
                                                }
                                                SessionType.BROWSER -> {
                                                        BrowserSession(appContext, name = finalName)
                                                        showNewSessionDialog = false
                                                }
                                                SessionType.TERMINAL -> {
                                                        TerminalSession(appContext, finalName)
                                                        showNewSessionDialog = false
                                                }
                                                SessionType.DEMO -> {
                                                        DemoSession(appContext, finalName)
                                                        showNewSessionDialog = false
                                                }
                                        }
                                }
                        )
                }

                // Editor Menu (3 simple buttons)
                if (showEditorMenu) {
                        EditorMenu(
                                onDismiss = { showEditorMenu = false },
                                onOpenFolder = {
                                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                        launcher.launch(intent)
                                },
                                onCreateNewFile = {
                                        val intent =
                                                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                                        addCategory(Intent.CATEGORY_OPENABLE)
                                                        type = "*/*"
                                                }
                                        launcher.launch(intent)
                                },
                                onOpenFile = {
                                        val intent =
                                                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                                        addCategory(Intent.CATEGORY_OPENABLE)
                                                        type = "*/*"
                                                }
                                        launcher.launch(intent)
                                }
                        )
                }

                // Notebook Menu (same as Editor Menu but creates NotebookSession)
                if (showNotebookMenu) {
                        NotebookMenu(
                                onDismiss = { showNotebookMenu = false },
                                onOpenFolder = {
                                        showNotebookMenu = false
                                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                        notebookLauncher.launch(intent)
                                },
                                onCreateNewFile = {
                                        showNotebookMenu = false
                                        val intent =
                                                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                                        addCategory(Intent.CATEGORY_OPENABLE)
                                                        type = "*/*"
                                                        putExtra(Intent.EXTRA_TITLE, "notebook.py")
                                                }
                                        notebookLauncher.launch(intent)
                                },
                                onOpenFile = {
                                        showNotebookMenu = false
                                        val intent =
                                                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                                        addCategory(Intent.CATEGORY_OPENABLE)
                                                        type = "*/*"
                                                }
                                        notebookLauncher.launch(intent)
                                }
                        )
                }
        }
}

@Composable
fun EditorMenu(
        onDismiss: () -> Unit,
        onOpenFolder: () -> Unit,
        onOpenFile: () -> Unit,
        onCreateNewFile: () -> Unit
) {
        Box(
                modifier =
                        Modifier.fillMaxSize().background(Color(0x99000000)).clickable {
                                onDismiss()
                        },
                contentAlignment = Alignment.Center
        ) {
                Column(
                        modifier =
                                Modifier.width(300.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2A2A2A))
                                        .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Text(
                                text = "Open Editor",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Open Folder button
                        Button(
                                onClick = onOpenFolder,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3A3A3A)
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                painter =
                                                        androidx.compose.ui.res.painterResource(
                                                                id =
                                                                        com.bsbeetle
                                                                                .bananaspaghetti
                                                                                .R
                                                                                .drawable
                                                                                .folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                                        ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Text("Open a Folder", color = Color.White)
                                }
                        }

                        // Open File button
                        Button(
                                onClick = onOpenFile,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3A3A3A)
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                painter =
                                                        androidx.compose.ui.res.painterResource(
                                                                id =
                                                                        com.bsbeetle
                                                                                .bananaspaghetti
                                                                                .R
                                                                                .drawable
                                                                                .file_open_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                                        ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Text("Open a File", color = Color.White)
                                }
                        }

                        // Create New File button
                        Button(
                                onClick = onCreateNewFile,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3A3A3A)
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                painter =
                                                        androidx.compose.ui.res.painterResource(
                                                                id =
                                                                        com.bsbeetle
                                                                                .bananaspaghetti
                                                                                .R
                                                                                .drawable
                                                                                .article_icon
                                                        ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Text("Create a New File", color = Color.White)
                                }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(onClick = onDismiss) {
                                Text("Cancel", color = Color(0xFF888888))
                        }
                }
        }
}

@Composable
fun NotebookMenu(
        onDismiss: () -> Unit,
        onOpenFolder: () -> Unit,
        onOpenFile: () -> Unit,
        onCreateNewFile: () -> Unit
) {
        Box(
                modifier =
                        Modifier.fillMaxSize().background(Color(0x99000000)).clickable {
                                onDismiss()
                        },
                contentAlignment = Alignment.Center
        ) {
                Column(
                        modifier =
                                Modifier.width(300.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2A2A2A))
                                        .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Text(
                                text = "Open Notebook",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Open Folder button
                        Button(
                                onClick = onOpenFolder,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3A3A3A)
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                painter =
                                                        androidx.compose.ui.res.painterResource(
                                                                id =
                                                                        com.bsbeetle
                                                                                .bananaspaghetti
                                                                                .R
                                                                                .drawable
                                                                                .folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                                        ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Text("Open a Folder", color = Color.White)
                                }
                        }

                        // Open File button
                        Button(
                                onClick = onOpenFile,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3A3A3A)
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                painter =
                                                        androidx.compose.ui.res.painterResource(
                                                                id =
                                                                        com.bsbeetle
                                                                                .bananaspaghetti
                                                                                .R
                                                                                .drawable
                                                                                .file_open_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                                        ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Text("Open a File", color = Color.White)
                                }
                        }

                        // Create New File button
                        Button(
                                onClick = onCreateNewFile,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3A3A3A)
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Icon(
                                                painter =
                                                        androidx.compose.ui.res.painterResource(
                                                                id =
                                                                        com.bsbeetle
                                                                                .bananaspaghetti
                                                                                .R
                                                                                .drawable
                                                                                .note_add_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                                        ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                        )
                                        Text("Create a New Notebook", color = Color.White)
                                }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(onClick = onDismiss) {
                                Text("Cancel", color = Color(0xFF888888))
                        }
                }
        }
}

@Composable
fun SessionCard(
        session: Session,
        snapshot: android.graphics.Bitmap?,
        onClick: () -> Unit,
        onClose: () -> Unit
) {
        val isActive = SessionList.activeSessionIndex == session.id

        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(Color(0xFF252525), Color(0xFF151515))
                                        )
                                )
                                .border(
                                        width = if (isActive) 2.dp else 1.dp,
                                        brush =
                                                if (isActive)
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color(0xFF4A90E2),
                                                                                Color(0xFF9013FE)
                                                                        )
                                                        )
                                                else
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color(0xFF333333),
                                                                                Color(0xFF222222)
                                                                        )
                                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onClick() }
        ) {
                // Session preview (WebView snapshot)
                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        if (snapshot != null) {
                                Image(
                                        bitmap = snapshot.asImageBitmap(),
                                        contentDescription = "Session preview",
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF0D0D0D)),
                                        contentScale = ContentScale.Crop
                                )
                        } else {
                                // Fallback when snapshot is not available
                                Box(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .background(
                                                                Color(0xFF0D0D0D),
                                                                RoundedCornerShape(8.dp)
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                        ) {
                                                when (session.type) {
                                                        SessionType.EDITOR ->
                                                                Icon(
                                                                        painter =
                                                                                androidx.compose.ui
                                                                                        .res
                                                                                        .painterResource(
                                                                                                id =
                                                                                                        com.bsbeetle
                                                                                                                .bananaspaghetti
                                                                                                                .R
                                                                                                                .drawable
                                                                                                                .article_icon
                                                                                        ),
                                                                        contentDescription =
                                                                                "Editor",
                                                                        tint = Color(0xFFAAAAAA),
                                                                        modifier =
                                                                                Modifier.size(48.dp)
                                                                )
                                                        SessionType.BROWSER ->
                                                                Icon(
                                                                        painter =
                                                                                androidx.compose.ui
                                                                                        .res
                                                                                        .painterResource(
                                                                                                id =
                                                                                                        com.bsbeetle
                                                                                                                .bananaspaghetti
                                                                                                                .R
                                                                                                                .drawable
                                                                                                                .web_icon
                                                                                        ),
                                                                        contentDescription =
                                                                                "Browser",
                                                                        tint = Color(0xFFAAAAAA),
                                                                        modifier =
                                                                                Modifier.size(48.dp)
                                                                )
                                                        SessionType.TERMINAL ->
                                                                Icon(
                                                                        painter =
                                                                                androidx.compose.ui
                                                                                        .res
                                                                                        .painterResource(
                                                                                                id =
                                                                                                        com.bsbeetle
                                                                                                                .bananaspaghetti
                                                                                                                .R
                                                                                                                .drawable
                                                                                                                .terminal_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                                                                        ),
                                                                        contentDescription =
                                                                                "Terminal",
                                                                        tint = Color(0xFFAAAAAA),
                                                                        modifier =
                                                                                Modifier.size(48.dp)
                                                                )
                                                        SessionType.NOTEBOOK ->
                                                                Icon(
                                                                        painter =
                                                                                androidx.compose.ui
                                                                                        .res
                                                                                        .painterResource(
                                                                                                id =
                                                                                                        com.bsbeetle
                                                                                                                .bananaspaghetti
                                                                                                                .R
                                                                                                                .drawable
                                                                                                                .note_add_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                                                                        ),
                                                                        contentDescription =
                                                                                "Notebook",
                                                                        tint = Color(0xFFAAAAAA),
                                                                        modifier =
                                                                                Modifier.size(48.dp)
                                                                )
                                                        SessionType.DEMO ->
                                                                Icon(
                                                                        painter =
                                                                                androidx.compose.ui
                                                                                        .res
                                                                                        .painterResource(
                                                                                                id =
                                                                                                        com.bsbeetle
                                                                                                                .bananaspaghetti
                                                                                                                .R
                                                                                                                .drawable
                                                                                                                .extension
                                                                                        ),
                                                                        contentDescription = "Demo",
                                                                        tint = Color(0xFFAAAAAA),
                                                                        modifier =
                                                                                Modifier.size(48.dp)
                                                                )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text = session.name,
                                                        color = Color(0xFFAAAAAA),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Medium
                                                )
                                        }
                                }
                        }
                }

                // Close button with better visibility
                Box(
                        modifier =
                                Modifier.align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xCC000000))
                                        .clickable { onClose() },
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                        )
                }

                // Session name label (simplified - no icon)
                Box(
                        modifier =
                                Modifier.align(Alignment.BottomStart)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xCC000000))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                        Text(
                                text = session.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                        )
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionDialog(onDismiss: () -> Unit, onCreateSession: (SessionType, String) -> Unit) {
        var selectedType by remember { mutableStateOf(SessionType.TERMINAL) }
        var sessionName by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }

        Box(
                modifier =
                        Modifier.fillMaxSize().background(Color(0x99000000)).alpha(0.7f).clickable(
                                        enabled = false
                                ) {},
                contentAlignment = Alignment.Center
        ) {
                Box(
                        modifier =
                                Modifier.width(400.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2A2A2A))
                                        .padding(24.dp)
                ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                        text = "New session",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                )

                                // Session Type Dropdown
                                ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = !expanded },
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        OutlinedTextField(
                                                value =
                                                        selectedType.name.lowercase()
                                                                .replaceFirstChar {
                                                                        it.uppercase()
                                                                },
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = {
                                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                                expanded = expanded
                                                        )
                                                },
                                                colors =
                                                        OutlinedTextFieldDefaults.colors(
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White,
                                                                focusedBorderColor =
                                                                        Color(0xFF555555),
                                                                unfocusedBorderColor =
                                                                        Color(0xFF3A3A3A),
                                                                focusedContainerColor =
                                                                        Color(0xFF1A1A1A),
                                                                unfocusedContainerColor =
                                                                        Color(0xFF1A1A1A),
                                                                focusedTrailingIconColor =
                                                                        Color.White,
                                                                unfocusedTrailingIconColor =
                                                                        Color(0xFF888888)
                                                        ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().menuAnchor()
                                        )

                                        ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false },
                                                modifier = Modifier.background(Color(0xFF1A1A1A))
                                        ) {
                                                // Filter out DEMO from the list
                                                SessionType.values()
                                                        .filter { it != SessionType.DEMO }
                                                        .forEach { type ->
                                                                DropdownMenuItem(
                                                                        text = {
                                                                                Text(
                                                                                        text =
                                                                                                type.name
                                                                                                        .lowercase()
                                                                                                        .replaceFirstChar {
                                                                                                                it.uppercase()
                                                                                                        },
                                                                                        color =
                                                                                                Color.White
                                                                                )
                                                                        },
                                                                        onClick = {
                                                                                selectedType = type
                                                                                expanded = false
                                                                        },
                                                                        colors =
                                                                                MenuDefaults
                                                                                        .itemColors(
                                                                                                textColor =
                                                                                                        Color.White
                                                                                        )
                                                                )
                                                        }
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Session Name Input
                                OutlinedTextField(
                                        value = sessionName,
                                        onValueChange = { sessionName = it },
                                        placeholder = {
                                                Text(
                                                        text = "Session name",
                                                        color = Color(0xFF666666)
                                                )
                                        },
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = Color(0xFF555555),
                                                        unfocusedBorderColor = Color(0xFF3A3A3A),
                                                        focusedContainerColor = Color(0xFF1A1A1A),
                                                        unfocusedContainerColor = Color(0xFF1A1A1A)
                                                ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Buttons
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        Button(
                                                onClick = onDismiss,
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF3A3A3A)
                                                        ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                Text(
                                                        "Cancel",
                                                        color = Color.White,
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                        }

                                        Button(
                                                onClick = {
                                                        onCreateSession(selectedType, sessionName)
                                                },
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF5E5CE6)
                                                        ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                Text(
                                                        "Done",
                                                        color = Color.White,
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                        }
                                }
                        }
                }
        }
}

private fun navigateToSession(navController: NavController, session: Session) {
        when (session.type) {
                SessionType.EDITOR ->
                        navController.navigate(Screen.OpenFolder.route) {
                                popUpTo(Screen.ZoomOut.route) { inclusive = true }
                        }
                SessionType.BROWSER ->
                        navController.navigate(Screen.Browser.route) {
                                popUpTo(Screen.ZoomOut.route) { inclusive = true }
                        }
                SessionType.TERMINAL ->
                        navController.navigate(Screen.Terminal.route) {
                                popUpTo(Screen.ZoomOut.route) { inclusive = true }
                        }
                SessionType.NOTEBOOK ->
                        navController.navigate(Screen.Notebook.route) {
                                popUpTo(Screen.ZoomOut.route) { inclusive = true }
                        }
                SessionType.DEMO ->
                        navController.navigate(Screen.Demo.route) {
                                popUpTo(Screen.ZoomOut.route) { inclusive = true }
                        }
        }
}
