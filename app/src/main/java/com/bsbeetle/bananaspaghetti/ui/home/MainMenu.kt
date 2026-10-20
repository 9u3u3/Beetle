package com.bsbeetle.bananaspaghetti.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalAuthViewModel
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.LocalSshViewModel
import com.bsbeetle.bananaspaghetti.LocalTerminalViewModel
import com.bsbeetle.bananaspaghetti.ui.components.RemoteFileBrowserDialog
import com.bsbeetle.bananaspaghetti.ui.components.SshConnectDialog
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.components.SessionTypeSelectionDialog
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.ui.theme.KarmaticFont
import com.bsbeetle.bananaspaghetti.ui.theme.born2bsporty
import com.bsbeetle.bananaspaghetti.utils.fileHandling.getFileName
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.NotebookSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionFactory
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import com.bsbeetle.bananaspaghetti.viewmodel.TerminalViewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

private const val PREFS_NAME = "beetle_storage_prefs"
private const val KEY_SHOWN_STORAGE_HINT = "shown_storage_hint"

// Action type enum for tracking which action triggered the session type dialog
private enum class FileAction {
    NEW_FILE,
    OPEN_FILE,
    OPEN_FOLDER
}

/**
 * Main menu component for home screen.
 * Contains Beetle branding and primary action buttons.
 */
@Composable
fun MainMenu(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    val buttonFontSize = 30.sp
    val terminalViewModel: TerminalViewModel = LocalTerminalViewModel.current
    val fileViewModel = LocalFileViewModel.current
    val editorViewModel = LocalEditorViewModel.current
    val bufferViewModel = LocalBufferViewModel.current
    
    // SharedPreferences to track if storage hint has been shown
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var hasShownHint by remember { mutableStateOf(prefs.getBoolean(KEY_SHOWN_STORAGE_HINT, false)) }
    
    // State for showing the storage hint dialog
    var showStorageHintDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    // State for Report Issue dialog
    var showReportIssueDialog by remember { mutableStateOf(false) }
    val authViewModel = LocalAuthViewModel.current
    val userEmail by authViewModel.userEmail.collectAsState()
    
    val sshViewModel = LocalSshViewModel.current

    // State for SSH Remote flow
    var showSshConnectDialog by remember { mutableStateOf(false) }
    var showRemoteFileBrowserDialog by remember { mutableStateOf(false) }

    // State for session type selection
    var showSessionTypeDialog by remember { mutableStateOf(false) }
    var currentFileAction by remember { mutableStateOf<FileAction?>(null) }
    var selectedSessionType by remember { mutableStateOf<String?>(null) } // "editor" or "notebook"
    
    // Launcher for Editor sessions
    val editorLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                
                // Always clear SSH remote state when opening a local file/folder
                fileViewModel.isRemoteMode = false
                fileViewModel.remoteBasePath = null
                fileViewModel.currentRemotePath = null
                editorViewModel.stopAllRemoteLsp()
                bufferViewModel.remoteFilesOpenedList.clear()

                if (DocumentsContract.isDocumentUri(context, uri)) {
                    // File selected - create Editor session
                    fileViewModel.selectedUri = uri
                    fileViewModel.selectedFolderUri = null

                    val fileName = getFileName(context, uri)
                    val session = SessionFactory.createSession(context.applicationContext, fileName, uri.toString())
                    session.id?.let { id -> bufferViewModel.setSessionId(id) }

                    val view = session.getOrInitView()
                    editorViewModel.setEditorWebView(view)
                    navController.navigate(Screen.OpenFile.route)
                } else {
                    // Folder selected - create Editor session with folder
                    fileViewModel.selectedFolderUri = uri
                    val folderDoc = DocumentFile.fromTreeUri(context, uri)
                    val session = EditorSession(context.applicationContext, folderDoc?.name ?: "Project")
                    session.projectRoot = folderDoc
                    session.id?.let { id -> bufferViewModel.setSessionId(id) }
                    editorViewModel.setEditorWebView(SessionList.sessions[SessionList.activeSessionIndex!!].view!!)
                    navController.navigate(Screen.OpenFolder.route)
                }
            }
        }
    }
    
    // Launcher for Notebook sessions
    val notebookLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    // File selected - create Notebook session
                    fileViewModel.selectedUri = uri
                    fileViewModel.selectedFolderUri = null
                    
                    val fileName = getFileName(context, uri)
                    val session = NotebookSession(context.applicationContext, fileName)
                    session.setFilePath(uri.toString())
                    session.id?.let { id -> bufferViewModel.setSessionId(id) }
                    
                    navController.navigate(Screen.Notebook.route)
                } else {
                    // Folder selected - create Notebook session with folder
                    fileViewModel.selectedFolderUri = uri
                    val folderDoc = DocumentFile.fromTreeUri(context, uri)
                    val session = NotebookSession(context.applicationContext, folderDoc?.name ?: "Notebook")
                    session.projectRoot = folderDoc
                    session.id?.let { id -> bufferViewModel.setSessionId(id) }
                    navController.navigate(Screen.Notebook.route)
                }
            }
        }
    }
    
    // Helper function to check and show hint before action
    fun executeWithHintCheck(action: () -> Unit) {
        if (!hasShownHint) {
            pendingAction = action
            showStorageHintDialog = true
        } else {
            action()
        }
    }
    
    // Helper to launch file picker after session type is selected
    fun launchFilePicker(fileAction: FileAction, sessionType: String) {
        val intent = when (fileAction) {
            FileAction.NEW_FILE -> Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            FileAction.OPEN_FILE -> Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            FileAction.OPEN_FOLDER -> Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
        
        if (sessionType == "editor") {
            editorLauncher.launch(intent)
        } else {
            notebookLauncher.launch(intent)
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        Column {
            // Header with logo and title
            Row(
                modifier = Modifier.width(400.dp).height(80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Image(
                    painter = painterResource(R.drawable.beetle),
                    contentDescription = "Beetle logo",
                    modifier = Modifier.size(65.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                // The version sits under the title. At the right edge of the row, it went under
                // the home screen icons on narrow portrait screens.
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Beetle",
                        fontFamily = KarmaticFont,
                        color = Color.White,
                        fontSize = 45.sp,
                        style = TextStyle(shadow = Shadow(color = Color(0xFFFFFFFF), offset = Offset(0f, 0f), blurRadius = 15f))
                    )
                    val versionName = remember {
                        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                                .getOrNull()
                    }
                    Text(
                        text = "v${versionName ?: "?"}",
                        color = Color(0xFF666666),
                        fontSize = 14.sp,
                        fontFamily = born2bsporty
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            val backgroundColor = Color(0xFFFFFFFF)

            val backdrop = rememberLayerBackdrop {
                drawRect(backgroundColor)
                drawContent()
            }

            // New file / Open file row
            Row(
                modifier = Modifier.width(400.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    text = "New file",
                    backdrop = backdrop,
                    fontSize = buttonFontSize,
                    modifier = Modifier.weight(1f).height(50.dp),
                    onClick = {
                        executeWithHintCheck {
                            currentFileAction = FileAction.NEW_FILE
                            showSessionTypeDialog = true
                        }
                    }
                )

                GlassButton(
                    text = "Open file",
                    backdrop = backdrop,
                    fontSize = buttonFontSize,
                    modifier = Modifier.weight(1f).height(50.dp),
                    onClick = {
                        executeWithHintCheck {
                            currentFileAction = FileAction.OPEN_FILE
                            showSessionTypeDialog = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Open folder button
            GlassButton(
                text = "Open folder",
                backdrop = backdrop,
                fontSize = buttonFontSize,
                modifier = Modifier.width(400.dp).height(50.dp),
                onClick = {
                    executeWithHintCheck {
                        currentFileAction = FileAction.OPEN_FOLDER
                        showSessionTypeDialog = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // SSH Remote button
            GlassButton(
                text = "SSH Remote",
                backdrop = backdrop,
                fontSize = buttonFontSize,
                modifier = Modifier.width(400.dp).height(50.dp),
                onClick = { showSshConnectDialog = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Terminal button
            GlassButton(
                text = "Terminal",
                backdrop = backdrop,
                fontSize = buttonFontSize,
                modifier = Modifier.width(400.dp).height(50.dp),
                onClick = {
                    val sshConn = sshViewModel.connection
                    if (fileViewModel.isRemoteMode && sshConn != null) {
                        // Open an SSH terminal connected to the remote machine
                        com.bsbeetle.bananaspaghetti.utils.sessionHandling.SshTerminalSession(
                            context.applicationContext,
                            "SSH Terminal",
                            sshConn
                        )
                    } else {
                        // Open a local Ubuntu (proot) terminal
                        val currentPath = fileViewModel.currentFilePath
                            ?: (SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession)
                                ?.projectRoot?.uri?.toString()
                        TerminalSession(context.applicationContext, initialDir = currentPath)
                    }
                    terminalViewModel.setTerminalWebView(
                        SessionList.sessions[SessionList.activeSessionIndex!!].getOrInitView()
                    )
                    navController.navigate(Screen.Terminal.route)
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Report Issue button - Red text with transparent background, smaller
            TextButton(
                onClick = { showReportIssueDialog = true },
                modifier = Modifier.width(400.dp).height(40.dp)
            ) {
                Text(
                    text = "Report Issue",
                    fontFamily = KarmaticFont,
                    fontSize = 18.sp,
                    color = Color(0xFFFF4444)
                )
            }
            
        }
    }
    
    // Session Type Selection Dialog
    if (showSessionTypeDialog && currentFileAction != null) {
        SessionTypeSelectionDialog(
            onDismiss = {
                showSessionTypeDialog = false
                currentFileAction = null
            },
            onSelectEditor = {
                currentFileAction?.let { action ->
                    launchFilePicker(action, "editor")
                }
                showSessionTypeDialog = false
                currentFileAction = null
            },
            onSelectNotebook = {
                currentFileAction?.let { action ->
                    launchFilePicker(action, "notebook")
                }
                showSessionTypeDialog = false
                currentFileAction = null
            }
        )
    }
    
    // Storage hint dialog - shows only on first use
    if (showStorageHintDialog) {
        AlertDialog(
            onDismissRequest = {
                showStorageHintDialog = false
                pendingAction = null
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCCCCCC),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF007ACC),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Beetle Storage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "To access your Beetle app storage:",
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Step 1
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = null,
                                tint = Color(0xFF007ACC),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tap the menu icon (three lines)", fontSize = 14.sp)
                        }
                        // Step 2
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = Color(0xFF007ACC),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select \"Beetle\" from the list", fontSize = 14.sp)
                        }
                        // Step 3
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF007ACC),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Navigate and select your folder", fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tip section
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFE8AB53),
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This is your Ubuntu Linux home directory. Make sure Linux is properly initialized before using file operations.",
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Mark as shown
                        prefs.edit().putBoolean(KEY_SHOWN_STORAGE_HINT, true).apply()
                        hasShownHint = true
                        showStorageHintDialog = false
                        
                        // Execute the pending action
                        pendingAction?.invoke()
                        pendingAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007ACC)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got it!")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStorageHintDialog = false
                        pendingAction = null
                    }
                ) {
                    Text("Cancel", color = Color(0xFF888888))
                }
            }
        )
    }
    
    // Report Issue dialog
    if (showReportIssueDialog) {
        ReportIssueDialog(
            userEmail = userEmail,
            onEmailChange = { authViewModel.setUserEmail(it) },
            onDismiss = { showReportIssueDialog = false }
        )
    }

    // SSH Connect Dialog (from home screen)
    if (showSshConnectDialog) {
        SshConnectDialog(
            onDismiss = { showSshConnectDialog = false },
            onConnected = {
                showSshConnectDialog = false
                showRemoteFileBrowserDialog = true
            },
            sshViewModel = sshViewModel
        )
    }

    // Remote File Browser Dialog (from home screen)
    if (showRemoteFileBrowserDialog) {
        RemoteFileBrowserDialog(
            onDismiss = { showRemoteFileBrowserDialog = false },
            onFolderSelected = { selectedPath ->
                showRemoteFileBrowserDialog = false
                // Clear any previous remote state (stale buffers / LSP from prior SSH session)
                editorViewModel.stopAllRemoteLsp()
                bufferViewModel.remoteFilesOpenedList.clear()
                val params = sshViewModel.connectionParams
                val sessionName = if (params != null)
                    "${params.username}@${params.host}:${selectedPath.substringAfterLast("/")}"
                else "Remote"
                val newSession = EditorSession(context.applicationContext, sessionName)
                // Persist remote state ON the session so it survives session switches
                newSession.isRemoteMode = true
                newSession.remoteBasePath = selectedPath
                editorViewModel.setEditorWebView(newSession.getOrInitView())
                fileViewModel.isRemoteMode = true
                fileViewModel.remoteBasePath = selectedPath
                fileViewModel.selectedFolderUri = null
                fileViewModel.currentRemotePath = null
                navController.navigate(Screen.OpenFolder.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            },
            sshViewModel = sshViewModel,
            initialPath = "/"
        )
    }
}

/**
 * Glass-effect button component used in MainMenu.
 */
@Composable
private fun GlassButton(
    text: String,
    backdrop: com.kyant.backdrop.Backdrop,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(8.dp) },
                    effects = {
                        vibrancy()
                        blur(15f.dp.toPx())
                        lens(25f.dp.toPx(), 50f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.01f))
                    }
                )
        )

        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = fontSize,
                fontFamily = born2bsporty,
                color = Color(0xFF989898),
                style = TextStyle(shadow = Shadow(color = Color(0xFF989898), offset = Offset(0f, 0f), blurRadius = 15f)),
                modifier = Modifier.padding(3.dp)
            )
        }
    }
}
