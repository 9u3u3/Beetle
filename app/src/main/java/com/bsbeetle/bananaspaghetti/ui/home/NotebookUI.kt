package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.*
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.data.sidebar.Sidebar
import com.bsbeetle.bananaspaghetti.data.sidebar.SidebarTab
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import com.bsbeetle.bananaspaghetti.ui.components.*
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.notebook.CellDelimiterRegistry
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.NotebookSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType
import com.bsbeetle.bananaspaghetti.viewmodel.*

/**
 * NotebookUI - UI for Notebook/REPL sessions Similar structure to InitialEditorUI but uses
 * NotebookSession's WebView and does NOT route files based on extension.
 */
@SuppressLint("RememberReturnType")
@Composable
fun NotebookUI(navController: NavController) {

    // Use mutableStateOf to track the session and trigger recomposition when it becomes available
    var currentSession by remember { mutableStateOf<NotebookSession?>(null) }

    // Find the NotebookSession - either at activeIndex or the last one in the list
    LaunchedEffect(Unit) {
        Log.d("NotebookUI", "NotebookUI launched")
        // Keep trying to get the session for up to 2 seconds
        repeat(20) { attempt ->
            // First try the active session
            val activeSession = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
            if (activeSession is NotebookSession) {
                Log.d("NotebookUI", "Found NotebookSession at activeIndex on attempt $attempt")
                currentSession = activeSession
                return@LaunchedEffect
            }

            // If active session is not a NotebookSession, find the last NotebookSession in the list
            val lastNotebook = SessionList.sessions.filterIsInstance<NotebookSession>().lastOrNull()
            if (lastNotebook != null) {
                Log.d("NotebookUI", "Found last NotebookSession on attempt $attempt")
                // Update active index to point to this notebook
                SessionList.activeSessionIndex = SessionList.sessions.indexOf(lastNotebook)
                currentSession = lastNotebook
                return@LaunchedEffect
            }

            Log.d(
                    "NotebookUI",
                    "Session not found, attempt $attempt, activeIndex: ${SessionList.activeSessionIndex}, sessions: ${SessionList.sessions.size}, types: ${SessionList.sessions.map { it.type }}"
            )
            kotlinx.coroutines.delay(100)
        }
        Log.e("NotebookUI", "Failed to find NotebookSession after retries")
    }

    // Debug logging
    LaunchedEffect(currentSession) {
        Log.d("NotebookUI", "currentSession changed: $currentSession")
        Log.d("NotebookUI", "currentSession view: ${currentSession?.view}")
    }

    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current
    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
    val gitViewModel: com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel = viewModel()
    val lspViewModel: LspViewModel = viewModel()

    // Sync fileViewModel with current session's projectRoot
    LaunchedEffect(SessionList.activeSessionIndex) {
        val activeSession =
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as?
                        NotebookSession
        activeSession?.projectRoot?.let { projectRoot ->
            fileViewModel.selectedFolderUri = projectRoot.uri
        }
    }

    BackHandler {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    // Pass BufferViewModel to active notebook session
    LaunchedEffect(Unit) {
        val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
        if (session is NotebookSession) {
            session.setBufferViewModel(bufferViewModel)
            session.id?.let { id -> bufferViewModel.setSessionId(id) }
        }
    }

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layout =
            when (uiState.keyboardPage) {
                0 -> KeyboardLayout.qwertyLayout
                1 -> KeyboardLayout.editorLayout
                else -> KeyboardLayout.terminalLayout
            }

    // Monitor IME keyboard and apply bottom padding to notebook
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeHeight = imeInsets.getBottom(density)

    // Apply bottom padding when IME keyboard is visible
    LaunchedEffect(imeHeight) {
        val paddingPx = imeHeight
        Log.d("NotebookUI", "IME height changed: $paddingPx px")
        if (paddingPx > 0) {
            currentSession?.view?.let { webView ->
                val js =
                        "if(window.setBottomPadding) { window.setBottomPadding($paddingPx); console.log('[Notebook] IME padding set to $paddingPx'); }"
                webView.evaluateJavascript(js, null)
                Log.d("NotebookUI", "Applied IME bottom padding: $paddingPx")
            }
        }
    }

    // UI States
    var showSidebar by rememberSaveable { mutableStateOf(false) }
    var showFileMenu by remember { mutableStateOf(false) }
    var topRightMode by remember { mutableStateOf(TopRightMode.NONE) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showUnsupportedFileDialog by remember { mutableStateOf(false) }
    var unsupportedFileName by remember { mutableStateOf("") }

    // Get the notebook WebView
    val webView = currentSession?.view

    // Observe ready state - need to handle case where session might not exist immediately
    val isReadyFlow = currentSession?.isReady
    val isReady by isReadyFlow?.collectAsStateWithLifecycle() ?: mutableStateOf(false)

    // Additional debug logging for ready state
    LaunchedEffect(currentSession, webView, isReady) {
        Log.d(
                "NotebookUI",
                "State update - currentSession: $currentSession, webView: $webView, isReady: $isReady"
        )
    }

    // Shared File Opening Logic for Notebook
    val onFileOpened: (DocumentFile) -> Unit = { documentFile ->
        // Check if file extension is supported by notebook
        val extension = documentFile.name?.substringAfterLast(".", "") ?: ""
        if (!CellDelimiterRegistry.isExtensionSupported(extension)) {
            // Show unsupported file dialog
            unsupportedFileName = documentFile.name ?: "Unknown file"
            showUnsupportedFileDialog = true
            Log.d("NotebookUI", "Unsupported file type: .$extension")
        } else {
            // Add file to buffer and load it
            if (bufferViewModel.filesOpenedList.none { it.uri == documentFile.uri }) {
                bufferViewModel.filesOpenedList.add(documentFile)
            }
            fileViewModel.selectedUri = documentFile.uri
            fileViewModel.currentFilePath = documentFile.uri.toString()
            bufferViewModel.currentFile.value = documentFile

            // For notebook, set the file path which triggers content loading
            currentSession?.setFilePath(documentFile.uri.toString())
            Log.d("NotebookUI", "Set file path in notebook: ${documentFile.uri}")
        }
    }

    val onFolderOpened: (DocumentFile) -> Unit = { folder ->
        // Update projectRoot on current notebook session
        currentSession?.projectRoot = folder
        fileViewModel.selectedFolderUri = folder.uri
    }

    // When the file is selected, load it into the notebook
    LaunchedEffect(fileViewModel.selectedUri) {
        fileViewModel.selectedUri?.let { uri ->
            DocumentFile.fromSingleUri(context, uri)?.let documentFile@{ documentFile ->
                if (documentFile.isDirectory) {
                    Log.d("NotebookUI", "Opened directory: ${documentFile.name}, skipping")
                    return@documentFile
                }

                // Check if file extension is supported by notebook
                val extension = documentFile.name?.substringAfterLast(".", "") ?: ""
                if (!CellDelimiterRegistry.isExtensionSupported(extension)) {
                    // Show unsupported file dialog
                    unsupportedFileName = documentFile.name ?: "Unknown file"
                    showUnsupportedFileDialog = true
                    Log.d("NotebookUI", "Unsupported file type in LaunchedEffect: .$extension")
                    return@documentFile
                }

                // Track file in buffer
                if (bufferViewModel.filesOpenedList.none { it.uri.toString() == uri.toString() }) {
                    bufferViewModel.filesOpenedList.add(documentFile)
                    Log.d("NotebookUI", "Added to buffer: ${documentFile.name}")
                }

                bufferViewModel.currentFile.value = documentFile
                fileViewModel.currentFilePath = uri.toString()

                // Load file into notebook
                currentSession?.setFilePath(uri.toString())
                Log.d("NotebookUI", "Set file path in notebook: $uri")
            }
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

            // Left top bar with menu, save, file options
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
                        // Save notebook content
                        currentSession?.view?.evaluateJavascript(
                                "if(window.saveContent) window.saveContent();",
                                null
                        )
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
                        currentSession?.view?.evaluateJavascript(
                                "if(window.performUndo) performUndo();",
                                null
                        )
                    },
                    onRedoClick = {
                        currentSession?.view?.evaluateJavascript(
                                "if(window.performRedo) performRedo();",
                                null
                        )
                    },
                    onSearchClick = { showSearchBar = !showSearchBar }
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
                        onCommitClick = { /* No commit dialog for notebook yet */},
                        onPushClick = { gitViewModel.push() },
                        onFileOpened = onFileOpened,
                        onFolderOpened = onFolderOpened
                )

                // Left Keyboard (Landscape) - Use SplitKeyboardHalf like InitialEditorUI
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

                // Right Keyboard (Landscape) - Use SplitKeyboardHalf like InitialEditorUI
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

            // Main Editor/Notebook Area
            if (webView != null && isReady) {
                Editor(
                        Modifier.constrainAs(editor) {
                            top.linkTo(topMiddle.bottom)
                            bottom.linkTo(parent.bottom)
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
                        },
                        webView
                )
            } else {
                // Fallback: Show loading state when notebook is not ready
                Box(
                        modifier =
                                Modifier.constrainAs(editor) {
                                            top.linkTo(topMiddle.bottom)
                                            bottom.linkTo(parent.bottom)
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
                        CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFF007ACC),
                                strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                                text = "Loading notebook...",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                        )
                    }
                }
            }

            // Bottom bar
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
                    editorViewModel = editorViewModel
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
                        listOf(SidebarTab.FILES, SidebarTab.GIT),
                        onTabChanged = {
                            if (it == SidebarTab.GIT) {
                                gitViewModel.initializeGit()
                            }
                        },
                        gitViewModel = gitViewModel,
                        onFileOptionsClick = { showFileMenu = true },
                        onSearchClick = { showSearchBar = !showSearchBar },
                        onZoomOutClick = { navController.navigate(Screen.ZoomOut.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }
        }

        // Extra Keys Row - Shows in portrait mode OR in landscape mode when useLandscapeIme is
        // enabled
        val showExtraKeys = !isLandscape || uiState.useLandscapeIme
        if (showExtraKeys) {
            val isKeyboardVisible = imeHeight > 0

            if (isKeyboardVisible) {
                ExtraKeysRow(
                        keyboardViewModel = keyboardViewModel,
                        mode = SessionType.NOTEBOOK,
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
                webView = currentSession?.view,
                onClose = { showSearchBar = false },
                modifier =
                        Modifier.align(Alignment.TopCenter)
                                .padding(top = if (isLandscape) 40.dp else 80.dp)
        )

        // File Menu Dialog
        FileMenuDialog(
                show = showFileMenu,
                onDismiss = { showFileMenu = false },
                bufferViewModel = bufferViewModel,
                fileViewModel = fileViewModel,
                onFileOpened = onFileOpened,
                onFolderOpened = onFolderOpened
        )

        // Unsupported file type dialog
        if (showUnsupportedFileDialog) {
            androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showUnsupportedFileDialog = false },
                    title = { Text("Unsupported File Type", color = Color.White) },
                    text = {
                        androidx.compose.foundation.layout.Column {
                            Text(
                                    "The file \"$unsupportedFileName\" is not supported in Notebook mode.",
                                    color = Color(0xFFCCCCCC)
                            )
                            androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.height(8.dp)
                            )
                            Text(
                                    "Currently only Python files (.py) are supported.",
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp
                            )
                            androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.height(8.dp)
                            )
                            Text(
                                    "Use the Editor for other file types.",
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                                onClick = { showUnsupportedFileDialog = false }
                        ) { Text("OK", color = Color(0xFF4CAF50)) }
                    },
                    containerColor = Color(0xFF2A2A2A)
            )
        }
    }
}
