package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.*
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bsbeetle.bananaspaghetti.data.sidebar.Sidebar
import com.bsbeetle.bananaspaghetti.data.sidebar.SidebarTab
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import com.bsbeetle.bananaspaghetti.ui.components.*
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.fileHandling.readFromFile
import com.bsbeetle.bananaspaghetti.utils.fileHandling.saveFile
import com.bsbeetle.bananaspaghetti.utils.languageHandling.LanguageRegistry
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.NotebookSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.viewmodel.*
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope

@SuppressLint("RememberReturnType")
@Composable
fun InitialEditorUI(navController: NavController) {

    // Log.d("Sessions", "Active Session Index: ${SessionList.activeSessionIndex}")

    // Get current editor session for content preservation
    // Use activeSessionIndex as key so it updates when session changes
    val currentSession = remember(SessionList.activeSessionIndex) {
        SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession
    }

    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current
    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
    val browserViewModel = LocalBrowserViewModel.current
    val sshViewModel = LocalSshViewModel.current
    val lspViewModel: LspViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val gitViewModel: com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel =
            androidx.lifecycle.viewmodel.compose.viewModel()
    val scope = rememberCoroutineScope()

    // Restore per-session state into FileViewModel whenever the active session changes.
    // Each EditorSession owns its own isRemoteMode/remoteBasePath/currentRemotePath so that
    // switching between an SSH session and a local session never clobbers the other's context.
    LaunchedEffect(SessionList.activeSessionIndex) {
        val activeSession =
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as?
                        EditorSession ?: return@LaunchedEffect
        fileViewModel.isRemoteMode = activeSession.isRemoteMode
        fileViewModel.remoteBasePath = activeSession.remoteBasePath
        fileViewModel.currentRemotePath = activeSession.currentRemotePath
        if (activeSession.isRemoteMode) {
            fileViewModel.selectedFolderUri = null
        } else {
            activeSession.projectRoot?.let { fileViewModel.selectedFolderUri = it.uri }
        }
        // Bump the counter so FilTree's LaunchedEffect always reloads for this session
        fileViewModel.refreshFileTree()
    }

    // Restore content when screen appears (after navigation back)
    LaunchedEffect(Unit) {
        delay(200) // Wait for WebView to be ready
        currentSession?.restoreContent()
    }

    val aiChatViewModel = com.bsbeetle.bananaspaghetti.LocalAiChatViewModel.current
    val aiPanelOpen by aiChatViewModel.isPanelOpen.collectAsStateWithLifecycle()

    BackHandler {
        // Back dismisses the assistant before it leaves the editor — the panel covers the editor,
        // so treating back as "leave the screen" would skip the state the user is actually looking
        // at and drop them on Home.
        if (aiPanelOpen) {
            aiChatViewModel.closePanel()
            return@BackHandler
        }
        // Save content before navigating away
        currentSession?.saveContent()
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    // Pass LspViewModel and BufferViewModel to active session if it's an EditorSession
    LaunchedEffect(Unit) {
        val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
        if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
            session.setLspViewModel(lspViewModel)
            session.setBufferViewModel(bufferViewModel)
            // Switch to this session's buffer
            session.id?.let { id -> bufferViewModel.setSessionId(id) }
        } else if (session is NotebookSession) {
            session.setBufferViewModel(bufferViewModel)
            // Switch to this session's buffer
            session.id?.let { id -> bufferViewModel.setSessionId(id) }
        }
    }

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()

    // Apply editor font size when it changes
    LaunchedEffect(uiState.editorFontSize) {
        editorViewModel.webView?.evaluateJavascript(
                "if(window.setFontSize) window.setFontSize(${uiState.editorFontSize});",
                null
        )
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layout =
            when (uiState.keyboardPage) {
                0 -> KeyboardLayout.qwertyLayout // Alphabets
                1 -> KeyboardLayout.editorLayout // Symbols
                else -> KeyboardLayout.terminalLayout // Navigation
            }

    // Monitor IME keyboard and apply bottom padding to editor
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeHeight = imeInsets.getBottom(density)

    // Apply bottom padding when IME keyboard is visible
    LaunchedEffect(imeHeight) {
        val paddingPx = imeHeight
        Log.d("EditorUI", "IME height changed: $paddingPx px")
        if (paddingPx > 0) {
            editorViewModel.webView?.let { webView ->
                val js =
                        "if(window.setBottomPadding) { window.setBottomPadding($paddingPx); console.log('[Editor] IME padding set to $paddingPx'); }"
                webView.evaluateJavascript(js, null)
                Log.d("EditorUI", "Applied IME bottom padding: $paddingPx")
            }
        } else {
            // Reset padding when keyboard is hidden (but keep bottom bar padding if present)
            Log.d("EditorUI", "IME hidden, padding will be managed by BottomBar")
        }
    }

    // UI States
    var showSidebar by rememberSaveable { mutableStateOf(false) }
    var showLanguagePopup by remember { mutableStateOf(false) }
    var showFileMenu by remember { mutableStateOf(false) }
    var topRightMode by remember { mutableStateOf(TopRightMode.NONE) }
    // Commit Dialog States
    var showCommitDialog by remember { mutableStateOf(false) }
    var isCommitAndPush by remember { mutableStateOf(false) }
    var pendingCommitFiles by remember { mutableStateOf(listOf<String>()) }
    var missingLanguageExt by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    
    // Run Button States
    var showRunPackageDialog by remember { mutableStateOf(false) }
    var runMissingPackage by remember { mutableStateOf("") }
    var runPendingFilePath by remember { mutableStateOf<String?>(null) }
    var expandBottomBar by remember { mutableStateOf(false) }

    // SSH Remote States
    var showSshConnectDialog by remember { mutableStateOf(false) }
    var showRemoteFileBrowserDialog by remember { mutableStateOf(false) }

    // 🚀 File Load (with LSP check & Performance fix)
    LaunchedEffect(fileViewModel.selectedUri) {
        // Check if this is a NotebookSession - they handle their own content loading
        val activeSession = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
        val isNotebookSession = activeSession is NotebookSession
        if (isNotebookSession) {
            Log.d("Buffer", "NotebookSession detected - will skip loadContentWhenReady")
        }

        // delay(50) removed to speed up LSP startup
        fileViewModel.selectedUri?.let { uri ->
            DocumentFile.fromSingleUri(context, uri)?.let documentFile@{ documentFile ->
                if (documentFile.isDirectory) {
                    Log.d("Buffer", "Opened directory: ${documentFile.name}, skipping LSP setup")
                    return@documentFile
                }

                if (bufferViewModel.filesOpenedList.none { it.uri.toString() == uri.toString() }) {
                    bufferViewModel.filesOpenedList.add(documentFile)
                    Log.d("Buffer", "Added: ${documentFile.name}")
                }

                // Check for Language Support (LSP)
                val extType = documentFile.name!!.substringAfterLast(".")
                val slug = LanguageRegistry.getSlugForExtension(extType)

                var lspPort = 8081 // Default port

                try {
                    if (slug != "plaintext") {
                        // Use isInstalledForExt to check both the slug and common aliases (e.g.,
                        // js/javascript)
                        if (!LanguageRegistry.isInstalledForExt(extType)) {
                            missingLanguageExt = extType
                            showLanguagePopup = true
                        } else {
                            // Get the actual installed slug (might be different from
                            // getSlugForExtension)
                            val installedSlug =
                                    LanguageRegistry.getInstalledSlugForExt(extType) ?: slug
                            // Start LSP and get port - show spinner while starting
                            lspViewModel.setLspStarting(true)
                            lspPort = editorViewModel.startLsp(installedSlug)
                            editorViewModel.awaitLspReady(installedSlug)
                            lspViewModel.setLspStarting(false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Buffer", "Error in file extension handling $e")
                }

                // Save current content before loading new file
                bufferViewModel.saveCurrentContentToBuffer()

                // Check if we already have this file in buffer (from tab switching)
                val fileUri = uri.toString()
                val bufferedContent = bufferViewModel.bufferLine[fileUri]

                val content =
                        if (bufferedContent != null) {
                            Log.d(
                                    "Buffer",
                                    "Using buffered content for ${documentFile.name}: ${bufferedContent.length} chars"
                            )
                            bufferedContent
                        } else {
                            Log.d("Buffer", "Reading from disk for ${documentFile.name}")
                            val diskContent = readFromFile(context, uri)
                            bufferViewModel.bufferLine[fileUri] = diskContent // Cache it
                            diskContent
                        }

                bufferViewModel.currentFile.value = documentFile // Track current file

                // Only load content via loadContentWhenReady for non-notebook sessions
                // NotebookSession handles its own content loading via setFilePath
                if (!isNotebookSession) {
                    loadContentWhenReady(editorViewModel.webView, content)
                } else {
                    Log.d("Buffer", "Skipping loadContentWhenReady for NotebookSession")
                }

                fileViewModel.currentFilePath = uri.toString()

                // Re-inject LspViewModel when file changes/session might change
                val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
                if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
                    session.setLspViewModel(lspViewModel)

                    // Determine project root from parent folder
                    // For files in ubuntu, we use the parent directory as project root
                    val parentFolder = documentFile.parentFile
                    val projectRoot =
                            if (parentFolder != null) {
                                // Convert content:// URI to a path the LSP can understand
                                // Typically files are in /data/.../ubuntu/home/... or similar
                                val parentPath = parentFolder.uri.path ?: "/home"
                                // Clean up the path - extract the part after "ubuntu" if present
                                val ubuntuIndex = parentPath.indexOf("/ubuntu/")
                                val cleanPath =
                                        if (ubuntuIndex >= 0) {
                                            parentPath.substring(ubuntuIndex + 7) // Skip "/ubuntu"
                                        } else {
                                            parentPath
                                        }
                                "file://$cleanPath"
                            } else {
                                "file:///home"
                            }

                    // Set root URI for project context
                    session.setRootUri(projectRoot)

                    // Set file path relative to project
                    val cleanName = documentFile.name?.replace(" ", "_") ?: "main.cpp"
                    val parentPath = documentFile.parentFile?.uri?.path ?: "/home"
                    val ubuntuIndex = parentPath.indexOf("/ubuntu/")
                    val cleanParentPath =
                            if (ubuntuIndex >= 0) {
                                parentPath.substring(ubuntuIndex + 7)
                            } else {
                                parentPath
                            }
                    session.setFilePath("$cleanParentPath/$cleanName")

                    // Use isInstalledForExt to check both the slug and common aliases
                    if (slug != "plaintext" && LanguageRegistry.isInstalledForExt(extType)) {
                        val installedSlug = LanguageRegistry.getInstalledSlugForExt(extType) ?: slug
                        Log.d(
                                "LSP-Debug",
                                "Calling setLanguage for installedSlug=$installedSlug (extType=$extType) on port=$lspPort"
                        )
                        session.setLanguage(installedSlug, lspPort)
                    } else {
                        Log.d(
                                "LSP-Debug",
                                "NOT calling setLanguage: extType=$extType, slug=$slug, isInstalledForExt=${LanguageRegistry.isInstalledForExt(extType)}"
                        )
                        // Clear language support for unsupported files
                        session.clearLanguage()
                    }
                }

                Log.d(
                        "Buffer",
                        "Opened: ${documentFile.name}, total: ${bufferViewModel.filesOpenedList.size}"
                )

                // Save to Recent Files
                com.bsbeetle.bananaspaghetti.utils.fileHandling.RecentFilesManager.addFile(
                        context,
                        uri,
                        documentFile.name ?: "Unknown"
                )
            }
        }
    }

    // 🌐 Remote File Load (via SFTP when isRemoteMode)
    LaunchedEffect(fileViewModel.currentRemotePath) {
        val remotePath = fileViewModel.currentRemotePath ?: return@LaunchedEffect
        if (!fileViewModel.isRemoteMode) return@LaunchedEffect

        // Persist the current remote path back into the session so it survives session switches
        val sessionForPath =
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as?
                        EditorSession
        sessionForPath?.currentRemotePath = remotePath

        val sshFs = sshViewModel.fileSystem ?: return@LaunchedEffect

        try {
            val content = withContext(Dispatchers.IO) { sshFs.readFile(remotePath) }

            loadContentWhenReady(editorViewModel.webView, content)
            bufferViewModel.bufferLine[remotePath] = content

            // Register in remote tab bar (deduped)
            if (!bufferViewModel.remoteFilesOpenedList.contains(remotePath)) {
                bufferViewModel.remoteFilesOpenedList.add(remotePath)
            }

            // Set up LSP for the remote file
            val ext = remotePath.substringAfterLast(".", "")
            val slug = LanguageRegistry.getSlugForExtension(ext)
            val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
            val hasRemoteLsp = slug != "plaintext" &&
                com.bsbeetle.bananaspaghetti.utils.ssh.RemoteLspBridge.getLspCommand(slug) != null
            if (hasRemoteLsp && session is EditorSession) {
                val sshConn = sshViewModel.connection
                if (sshConn != null) {
                    lspViewModel.setLspStarting(true)
                    val lspPort = editorViewModel.startRemoteLsp(slug, sshConn)
                    editorViewModel.awaitRemoteLspReady(slug)
                    lspViewModel.setLspStarting(false)
                    session.setRootUri("file://${fileViewModel.remoteBasePath ?: "/"}")
                    session.setFilePath(remotePath)
                    session.setLanguage(slug, lspPort)
                }
            } else if (session is EditorSession) {
                session.clearLanguage()
            }
        } catch (e: Exception) {
            Log.e("RemoteLoad", "Failed to load remote file: $remotePath", e)
        }
    }

    // Shared File Opening Logic
    val onFileOpened: (DocumentFile) -> Unit = { documentFile ->
        // Add file to buffer and load it
        if (bufferViewModel.filesOpenedList.none { it.uri == documentFile.uri }) {
            bufferViewModel.filesOpenedList.add(documentFile)
        }
        fileViewModel.selectedUri = documentFile.uri
        fileViewModel.currentFilePath = documentFile.uri.toString()
        bufferViewModel.currentFile.value = documentFile

        val content = readFromFile(context, documentFile.uri)
        bufferViewModel.bufferLine[documentFile.uri.toString()] = content // Use URI as key
        loadContentWhenReady(editorViewModel.webView, content)
    }

    val onFolderOpened: (DocumentFile) -> Unit = { folder ->
        // Create new editor session for folder
        // Session auto-registers and sets activeSessionIndex in its init{}
        val newSession = EditorSession(context.applicationContext, folder.name ?: "Project")
        newSession.projectRoot = folder

        // Reset remote mode — switching to local folder clears all SSH state
        fileViewModel.isRemoteMode = false
        fileViewModel.remoteBasePath = null
        fileViewModel.currentRemotePath = null
        editorViewModel.stopAllRemoteLsp()
        bufferViewModel.remoteFilesOpenedList.clear()

        // Update FileViewModel to show new folder in file tree
        fileViewModel.selectedFolderUri = folder.uri

        // Set up editor with new session's WebView
        editorViewModel.setEditorWebView(newSession.getOrInitView())

        // Navigate to editor with new session
        navController.navigate(Screen.OpenFolder.route) {
            popUpTo(Screen.Home.route) { inclusive = false }
        }
    }

    Box(Modifier.fillMaxSize()) {

        // ---------------- MAIN UI ----------------
        ConstraintLayout(
                Modifier.fillMaxSize()
                        .background(Color(0xFF1B1B1B))
                        .then(if (!isLandscape) Modifier.padding(top = 40.dp) else Modifier)
        ) {
            val (
                    topLeft,
                    topMiddle,
                    topRight,
                    leftBox,
                    rightBox,
                    leftKb,
                    editor,
                    rightKb,
                    bottomBarRef) =
                    createRefs()

            // Merged LeftTopBar: Includes Menu toggle (Block 1) AND Save functionality (Block 2)
            LeftTopBar(
                    modifier =
                            Modifier.constrainAs(topLeft) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                                end.linkTo(topMiddle.start)
                                width = Dimension.wrapContent
                            },
                    onMenuClick = { showSidebar = true },
                    showMenuIcon = !isLandscape,
                    showBranchButton = isLandscape,
                    onSaveClick = {
                        if (fileViewModel.isRemoteMode) {
                            val remotePath = fileViewModel.currentRemotePath
                            val sshFs = sshViewModel.fileSystem
                            if (remotePath != null && sshFs != null) {
                                val webView = editorViewModel.webView
                                webView?.evaluateJavascript(
                                    "window.editor?.state?.doc?.toString() || ''"
                                ) { result ->
                                    val content = com.bsbeetle.bananaspaghetti.utils.webViewHandling
                                        .JsResult.decodeString(result)
                                        ?: return@evaluateJavascript
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            sshFs.writeFile(remotePath, content)
                                            withContext(Dispatchers.Main) {
                                                bufferViewModel.markAsClean(remotePath)
                                                android.widget.Toast.makeText(
                                                    context, "File saved", android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("RemoteSave", "Failed to save: $remotePath", e)
                                        }
                                    }
                                }
                            }
                        } else {
                            saveFile(fileViewModel, bufferViewModel, context)
                        }
                    },
                    onRunClick = {
                        // Run the current file
                        Log.d("CodeRunner", "Run button clicked")
                        Log.d("CodeRunner", "ActiveSessionIndex: ${SessionList.activeSessionIndex}")
                        Log.d("CodeRunner", "Sessions count: ${SessionList.sessions.size}")
                        Log.d("CodeRunner", "selectedUri: ${fileViewModel.selectedUri}")
                        
                        // Try to get EditorSession from activeSessionIndex first, 
                        // then fall back to finding any EditorSession in the list
                        var currentEditorSession = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession
                        
                        if (currentEditorSession == null) {
                            // Fall back: find the first (or most recent) EditorSession
                            currentEditorSession = SessionList.sessions.filterIsInstance<EditorSession>().lastOrNull()
                            Log.d("CodeRunner", "Fallback: found EditorSession: $currentEditorSession")
                        }
                        
                        val currentUri = fileViewModel.selectedUri
                        
                        Log.d("CodeRunner", "currentEditorSession: $currentEditorSession")
                        
                        if (currentEditorSession == null) {
                            Log.e("CodeRunner", "No editor session found in session list")
                            android.widget.Toast.makeText(context, "No editor session", android.widget.Toast.LENGTH_SHORT).show()
                            return@LeftTopBar
                        }
                        
                        if (currentUri == null) {
                            Log.e("CodeRunner", "No file URI - selectedUri is null")
                            android.widget.Toast.makeText(context, "No file open", android.widget.Toast.LENGTH_SHORT).show()
                            return@LeftTopBar
                        }
                        
                        // First save the file
                        saveFile(fileViewModel, bufferViewModel, context)
                        
                        // Get the file path
                        val filePath = getPathFromUri(context, currentUri)
                        Log.d("CodeRunner", "Running file: $filePath (from URI: $currentUri)")
                        
                        if (filePath == null) {
                            android.widget.Toast.makeText(context, "Cannot get file path", android.widget.Toast.LENGTH_SHORT).show()
                            return@LeftTopBar
                        }
                        
                        // Get or create the runner terminal
                        Log.d("CodeRunner", "Getting runner terminal...")
                        val (terminal, wasNew) = currentEditorSession.getOrCreateRunnerTerminal(context)
                        Log.d("CodeRunner", "Got terminal: $terminal, wasNew: $wasNew")
                        
                        // Build the command
                        val runCmd = "clear && bash \$PREFIX/code-runner.sh \"$filePath\"\n"
                        Log.d("CodeRunner", "Inserting command: $runCmd")
                        
                        // Insert command into terminal
                        terminal.insertText(runCmd)
                        Log.d("CodeRunner", "Command inserted into terminal")
                        
                        // Expand bottom bar to show terminal output
                        expandBottomBar = true
                        
                        android.widget.Toast.makeText(context, "Running ${java.io.File(filePath).name}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onFileClick = {
                        if (isLandscape) {
                            topRightMode =
                                    if (topRightMode == TopRightMode.FILE) TopRightMode.NONE
                                    else TopRightMode.FILE
                        } else {
                            showFileMenu = true
                        }
                    },
                    onBranchClick = {
                        topRightMode =
                                if (topRightMode == TopRightMode.GIT) TopRightMode.NONE
                                else TopRightMode.GIT
                    },
                    onUndoClick = {
                        editorViewModel.webView?.evaluateJavascript(
                                "if(window.performUndo) performUndo();",
                                null
                        )
                    },
                    onRedoClick = {
                        editorViewModel.webView?.evaluateJavascript(
                                "if(window.performRedo) performRedo();",
                                null
                        )
                    },
                    onSearchClick = {
                        // Toggle the custom search bar overlay
                        showSearchBar = !showSearchBar
                    }
            )

            TopBar(
                    modifier =
                            Modifier.constrainAs(topMiddle) {
                                top.linkTo(parent.top)
                                start.linkTo(topLeft.end)
                                end.linkTo(topRight.start)
                                width = Dimension.fillToConstraints
                            },
                    context = context
            )

            RightTopBar(
                    modifier =
                            Modifier.constrainAs(topRight) {
                                top.linkTo(parent.top)
                                start.linkTo(topMiddle.end)
                                end.linkTo(parent.end)
                                width = Dimension.wrapContent
                            },
                    navController = navController
            )

            if (isLandscape) {
                TopLeftBox(
                        Modifier.constrainAs(leftBox) {
                            top.linkTo(topLeft.bottom)
                            bottom.linkTo(leftKb.top)
                            start.linkTo(parent.start)
                            end.linkTo(editor.start)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        }
                )

                TopRightBox(
                        Modifier.constrainAs(rightBox) {
                            top.linkTo(topRight.bottom)
                            bottom.linkTo(rightKb.top)
                            start.linkTo(editor.end)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        },
                        mode = topRightMode,
                        gitViewModel = gitViewModel,
                        fileViewModel = fileViewModel,
                        bufferViewModel = bufferViewModel,
                        currentFilePath = fileViewModel.currentFilePath,
                        onCommitClick = { files ->
                            pendingCommitFiles = files
                            isCommitAndPush = false
                            showCommitDialog = true
                        },
                        onPushClick = { gitViewModel.push() },
                        onFileOpened = onFileOpened,
                        onFolderOpened = onFolderOpened,
                        onSshRemote = { showSshConnectDialog = true }
                )

                // Only show custom keyboards if NOT using landscape IME mode
                if (uiState.isLeftKeyboardVisible && !uiState.useLandscapeIme) {
                    SplitKeyboardHalf(
                            layout = layout,
                            side = KeyboardSide.LEFT,
                            isCapsLockOn = uiState.isCapsLockOn,
                            isShiftOn = uiState.isShiftOn,
                            isCtrlOn = uiState.isCtrlOn,
                            onKeyEvent = keyboardViewModel::onKeyEvent,
                            modifier =
                                    Modifier.constrainAs(leftKb) {
                                                start.linkTo(parent.start)
                                                bottom.linkTo(parent.bottom)
                                                width = Dimension.percent(uiState.leftKeyboardWidth)
                                            }
                                            .background(Color(0xFF212121)),
                            leftPadding = uiState.leftKeyboardPadding,
                            keyboardHeightFraction = uiState.leftKeyboardHeight
                    )
                }

                if (uiState.isRightKeyboardVisible && !uiState.useLandscapeIme) {
                    SplitKeyboardHalf(
                            layout = layout,
                            side = KeyboardSide.RIGHT,
                            isCapsLockOn = uiState.isCapsLockOn,
                            isShiftOn = uiState.isShiftOn,
                            isCtrlOn = uiState.isCtrlOn,
                            onKeyEvent = keyboardViewModel::onKeyEvent,
                            modifier =
                                    Modifier.constrainAs(rightKb) {
                                                end.linkTo(parent.end)
                                                bottom.linkTo(parent.bottom)
                                                width =
                                                        Dimension.percent(
                                                                uiState.rightKeyboardWidth
                                                        )
                                            }
                                            .background(Color(0xFF212121)),
                            keyboardHeightFraction = uiState.rightKeyboardHeight
                    )
                }
            }

            // Check if webView is available before rendering Editor
            // Collect from StateFlow for reactive updates when session changes
            val currentWebView by editorViewModel.webViewFlow.collectAsStateWithLifecycle()
            val webView = currentWebView // Local copy for smart cast

            // Observe isReady state from the session
            val session = webView?.session as? EditorSession
            val isReadyFlow = session?.isReady
            val isReady by isReadyFlow?.collectAsStateWithLifecycle() ?: mutableStateOf(false)

            // Debug logging for session switching
            LaunchedEffect(webView) {
                Log.d(
                        "InitialEditorUI",
                        "webView changed! New webView for session: ${webView?.session?.name}"
                )
                Log.d(
                        "InitialEditorUI",
                        "webView is ${if (webView != null) "NOT NULL" else "NULL"}"
                )
                if (webView != null) {
                    val sess = webView.session
                    when (sess) {
                        is NotebookSession ->
                                Log.d("InitialEditorUI", "Session isReady: ${sess.isReady.value}")
                        is EditorSession ->
                                Log.d("InitialEditorUI", "Session isReady: ${sess.isReady.value}")
                        else -> Log.d("InitialEditorUI", "Unknown session type")
                    }
                }
            }

            // Main Editor Area - Show WebView immediately, with loading overlay if not ready
            if (webView != null) {
                Box(
                        modifier =
                                Modifier.constrainAs(editor) {
                                    top.linkTo(topMiddle.bottom)
                                    bottom.linkTo(bottomBarRef.top)
                                    // If using landscape IME mode, give full width to editor
                                    val showLeftKb =
                                            isLandscape &&
                                                    uiState.isLeftKeyboardVisible &&
                                                    !uiState.useLandscapeIme
                                    val showRightKb =
                                            isLandscape &&
                                                    uiState.isRightKeyboardVisible &&
                                                    !uiState.useLandscapeIme
                                    start.linkTo(if (showLeftKb) leftKb.end else parent.start)
                                    end.linkTo(if (showRightKb) rightKb.start else parent.end)
                                    width = Dimension.fillToConstraints
                                    height = Dimension.fillToConstraints
                                }
                ) {
                    // Show WebView immediately - it will load in the background
                    Editor(Modifier.fillMaxSize(), webView)

                    // AI assistant takes the editor's place while open (top-bar toggle). Drawn
                    // over the still-mounted WebView rather than swapped for it, so the editor
                    // comes back exactly as it was left.
                    if (aiPanelOpen) {
                        com.bsbeetle.bananaspaghetti.ui.components.AiChatPanel(
                                Modifier.fillMaxSize()
                        )
                    }

                    // Loading overlay - fades out when ready
                    if (!isReady && !aiPanelOpen) {
                        Box(
                                modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)),
                                contentAlignment = Alignment.Center
                        ) {
                            Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = Color(0xFF007ACC),
                                        strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                        text = "Loading editor...",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Fallback: Show loading state when WebView doesn't exist yet
                Box(
                        modifier =
                                Modifier.constrainAs(editor) {
                                            top.linkTo(topMiddle.bottom)
                                            bottom.linkTo(bottomBarRef.top)
                                            start.linkTo(parent.start)
                                            end.linkTo(parent.end)
                                            width = Dimension.fillToConstraints
                                            height = Dimension.fillToConstraints
                                        }
                                        .fillMaxSize()
                                        .background(Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center
                ) {
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFF007ACC),
                                strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                                text = "Initializing editor...",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                        )
                    }
                }
            }

            BottomBar(
                    Modifier.constrainAs(bottomBarRef) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(editor.start)
                        end.linkTo(editor.end)
                        width = Dimension.fillToConstraints
                    },
                    color = Color(0xFF1B1B1B),
                    lspViewModel = lspViewModel,
                    gitViewModel = gitViewModel,
                    editorViewModel = editorViewModel,
                    expandToHalf = expandBottomBar,
                    onExpandHandled = { expandBottomBar = false }
            )

            if (!isLandscape) {
                Sidebar(
                        showSidebar,
                        { showSidebar = false },
                        {
                            fileViewModel.selectedUri = Uri.parse(it)
                            fileViewModel.currentFilePath = it
                            showSidebar = false
                        },
                        fileViewModel.currentFilePath,
                        listOf(SidebarTab.FILES, SidebarTab.GIT), // Only Files and Git in sidebar
                        onTabChanged = {
                            if (it == SidebarTab.GIT) {
                                gitViewModel.initializeGit()
                            }
                        },
                        gitViewModel = gitViewModel,
                        // Quick action callbacks (buttons moved from top bar in portrait mode)
                        onFileOptionsClick = { showFileMenu = true },
                        onSearchClick = { showSearchBar = !showSearchBar },
                        onZoomOutClick = {
                            currentSession?.saveContent()
                            navController.navigate(Screen.ZoomOut.route)
                        },
                        onSettingsClick = {
                            currentSession?.saveContent()
                            navController.navigate(Screen.Settings.route)
                        }
                )
            }
        }

        // Extra Keys Row - Shows in portrait mode OR in landscape mode when useLandscapeIme is
        // enabled
        val showExtraKeys = !isLandscape || uiState.useLandscapeIme
        if (showExtraKeys) {
            val imeInsets = WindowInsets.ime
            val density = androidx.compose.ui.platform.LocalDensity.current
            val isKeyboardVisible = imeInsets.getBottom(density) > 0

            if (isKeyboardVisible) {
                com.bsbeetle.bananaspaghetti.ui.components.ExtraKeysRow(
                        keyboardViewModel = keyboardViewModel,
                        mode =
                                com.bsbeetle
                                        .bananaspaghetti
                                        .utils
                                        .sessionHandling
                                        .SessionType
                                        .EDITOR,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(Color(0xFF2E2E2E))
                                        .windowInsetsPadding(imeInsets)
                                        .align(Alignment.BottomCenter)
                )
            }
        }

        // ---------------- SEARCH BAR OVERLAY ----------------
        SearchReplaceBar(
                visible = showSearchBar,
                webView = editorViewModel.webView,
                onClose = { showSearchBar = false },
                modifier =
                        Modifier.align(Alignment.TopCenter)
                                .padding(top = if (isLandscape) 40.dp else 80.dp)
        )

        // ---------------- POPUP OVERLAY ----------------
        SlideLanguagePopup(
                missingLanguageExt = missingLanguageExt,
                show = showLanguagePopup,
                onInstall = {
                    showLanguagePopup = false
                    navController.navigate(Screen.Extensions.route)
                },
                onDismiss = { showLanguagePopup = false }
        )

        // Landscape Commit Dialog
        if (showCommitDialog) {
            LandscapeCommitDialog(
                    isCommitAndPush = isCommitAndPush,
                    selectedFilesCount = pendingCommitFiles.size,
                    onDismiss = { showCommitDialog = false },
                    onCommit = { message ->
                        if (isCommitAndPush) {
                            gitViewModel.commitAndPush(message, pendingCommitFiles)
                        } else {
                            gitViewModel.doCommit(message, pendingCommitFiles)
                        }
                        showCommitDialog = false
                    }
            )
        }

        // File Menu Dialog
        FileMenuDialog(
                show = showFileMenu,
                onDismiss = { showFileMenu = false },
                bufferViewModel = bufferViewModel,
                fileViewModel = fileViewModel,
                onFileOpened = onFileOpened,
                onFolderOpened = onFolderOpened,
                onSshRemote = {
                    showFileMenu = false
                    showSshConnectDialog = true
                }
        )

        // SSH Connect Dialog
        if (showSshConnectDialog) {
            com.bsbeetle.bananaspaghetti.ui.components.SshConnectDialog(
                onDismiss = { showSshConnectDialog = false },
                onConnected = {
                    showSshConnectDialog = false
                    showRemoteFileBrowserDialog = true
                },
                sshViewModel = sshViewModel
            )
        }

        // Remote File Browser Dialog
        if (showRemoteFileBrowserDialog) {
            com.bsbeetle.bananaspaghetti.ui.components.RemoteFileBrowserDialog(
                onDismiss = { showRemoteFileBrowserDialog = false },
                onFolderSelected = { selectedPath ->
                    showRemoteFileBrowserDialog = false
                    val params = sshViewModel.connectionParams
                    val sessionName = if (params != null)
                        "${params.username}@${params.host}:${selectedPath.substringAfterLast("/")}"
                    else "Remote"
                    // Create new editor session for the remote folder
                    val newSession = EditorSession(context.applicationContext, sessionName)
                    // Persist remote state ON the session so it survives session switches
                    newSession.isRemoteMode = true
                    newSession.remoteBasePath = selectedPath
                    editorViewModel.setEditorWebView(newSession.getOrInitView())
                    // Reflect into FileViewModel for the current navigation
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
}

@Composable
fun SlideLanguagePopup(
        missingLanguageExt: String,
        show: Boolean,
        onInstall: () -> Unit,
        onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
                visible = show,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(Modifier.width(340.dp).background(Color(0xFF1E1E1E)).padding(18.dp)) {
                Column {
                    Text(text = "Language support missing", color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                            text =
                                    "Support for .$missingLanguageExt is not installed.\nInstall to enable syntax highlighting, autocomplete & LSP.",
                            color = Color(0xFFCCCCCC),
                            fontSize = 12.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Button(
                                onClick = onInstall,
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2F7BDE)
                                        )
                        ) { Text("Install") }
                        Spacer(Modifier.width(12.dp))
                        Button(
                                onClick = onDismiss,
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3A3A3A)
                                        )
                        ) { Text("Maybe later") }
                    }
                }
            }
        }
    }
}
