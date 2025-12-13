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
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.viewmodel.*
import kotlinx.coroutines.delay

@SuppressLint("RememberReturnType")
@Composable
fun InitialEditorUI(navController: NavController) {

    // Log.d("Sessions", "Active Session Index: ${SessionList.activeSessionIndex}")

    // Get current editor session for content preservation
    val currentSession = remember {
        SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession
    }
    
    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current
    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
    val lspViewModel: LspViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    // Sync fileViewModel with current session's projectRoot
    // This ensures each session shows its own folder in the file tree
    LaunchedEffect(SessionList.activeSessionIndex) {
        val activeSession = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession
        activeSession?.projectRoot?.let { projectRoot ->
            fileViewModel.selectedFolderUri = projectRoot.uri
        }
    }
    
    // Restore content when screen appears (after navigation back)
    LaunchedEffect(Unit) {
        delay(200) // Wait for WebView to be ready
        currentSession?.restoreContent()
    }

    BackHandler {
        // Save content before navigating away
        currentSession?.saveContent()
        navController.popBackStack()
    }

    // Pass LspViewModel to active session if it's an EditorSession
    LaunchedEffect(Unit) {
        val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
        if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
            session.setLspViewModel(lspViewModel)
        }
    }

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()


    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layout = when (uiState.keyboardPage) {
        0 -> KeyboardLayout.qwertyLayout    // Alphabets
        1 -> KeyboardLayout.editorLayout    // Symbols
        else -> KeyboardLayout.terminalLayout // Navigation
    }

    // UI States
    var showSidebar by rememberSaveable { mutableStateOf(false) }
    var showLanguagePopup by remember { mutableStateOf(false) }
    var showFileMenu by remember { mutableStateOf(false) }
    var missingLanguageExt by remember { mutableStateOf("") }

    // 🚀 File Load (with LSP check & Performance fix)
    LaunchedEffect(fileViewModel.selectedUri) {
        delay(50) // allows UI to compose first so no freeze
        fileViewModel.selectedUri?.let { uri ->
            DocumentFile.fromSingleUri(context, uri)?.let { documentFile ->

                if (documentFile.isDirectory) {
                    Log.d("Buffer", "Opened directory: ${documentFile.name}, skipping LSP setup")
                    return@let
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
                        if (!LanguageRegistry.isInstalled(slug)) {
                            missingLanguageExt = extType
                            showLanguagePopup = true
                        } else {
                            // Start LSP and get port
                            lspPort = editorViewModel.startLsp(slug)
                            android.widget.Toast.makeText(context, "Initializing $slug support on port $lspPort...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Buffer", "Error in file extension handling $e")
                }

                // Save current content before loading new file
                bufferViewModel.saveCurrentContentToBuffer()

                val content = readFromFile(context, uri)
                bufferViewModel.bufferLine[documentFile] = content
                bufferViewModel.currentFile.value = documentFile // Track current file
                loadContentWhenReady(editorViewModel.webView, content)
                fileViewModel.currentFilePath = uri.toString()
                
                // Re-inject LspViewModel when file changes/session might change
                val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)
                if (session is com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession) {
                    session.setLspViewModel(lspViewModel)
                    // Use a clean filename for LSP to avoid content:// URI issues
                    val cleanName = documentFile.name?.replace(" ", "_") ?: "main.cpp"
                    session.setFilePath("/src/$cleanName")
                    
                    if (slug != "plaintext" && LanguageRegistry.isInstalled(slug)) {
                        session.setLanguage(slug, lspPort)
                    } else {
                        // Clear language support for unsupported files
                        session.clearLanguage()
                    }
                }

                Log.d("Buffer", "Opened: ${documentFile.name}, total: ${bufferViewModel.filesOpenedList.size}")
            }
        }
    }

    Box(Modifier.fillMaxSize()) {

        // ---------------- MAIN UI ----------------
        ConstraintLayout(Modifier.fillMaxSize()) {
            val (topLeft, topMiddle, topRight, leftBox, rightBox, leftKb, editor, rightKb, bottomBarRef) = createRefs()

            // Merged LeftTopBar: Includes Menu toggle (Block 1) AND Save functionality (Block 2)
            LeftTopBar(
                modifier = Modifier.constrainAs(topLeft) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(topMiddle.start)
                    width = Dimension.percent(0.2f)
                },
                onMenuClick = { showSidebar = true },
                showMenuIcon = !isLandscape,
                onSaveClick = { saveFile(fileViewModel, context) },
                onFileClick = { showFileMenu = true },
                onUndoClick = { 
                    editorViewModel.webView?.evaluateJavascript("if(window.performUndo) performUndo();", null)
                },
                onRedoClick = {
                    editorViewModel.webView?.evaluateJavascript("if(window.performRedo) performRedo();", null)
                }
            )

            TopBar(
                modifier = Modifier.constrainAs(topMiddle) {
                    top.linkTo(parent.top)
                    start.linkTo(topLeft.end)
                    end.linkTo(topRight.start)
                    width = Dimension.percent(0.6f)
                },
                context = context
            )

            RightTopBar(
                modifier = Modifier.constrainAs(topRight) {
                    top.linkTo(parent.top)
                    start.linkTo(topMiddle.end)
                    end.linkTo(parent.end)
                    width = Dimension.percent(0.2f)
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
                        start.linkTo(editor.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                )

                if (uiState.isLeftKeyboardVisible) {
                    SplitKeyboardHalf(
                        layout, KeyboardSide.LEFT, uiState.isCapsLockOn,
                        uiState.isShiftOn, uiState.isCtrlOn, keyboardViewModel::onKeyEvent,
                        Modifier.constrainAs(leftKb) {
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.percent(sliderValue / 100f)
                        }.background(Color(0xFF212121))
                    )
                }

                if (uiState.isRightKeyboardVisible) {
                    SplitKeyboardHalf(
                        layout, KeyboardSide.RIGHT, uiState.isCapsLockOn,
                        uiState.isShiftOn, uiState.isCtrlOn, keyboardViewModel::onKeyEvent,
                        Modifier.constrainAs(rightKb) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.percent(sliderValue / 100f)
                        }.background(Color(0xFF212121))
                    )
                }
            }

            Editor(
                Modifier.constrainAs(editor) {
                    top.linkTo(topMiddle.bottom)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(if (isLandscape && uiState.isLeftKeyboardVisible) leftKb.end else parent.start)
                    end.linkTo(if (isLandscape && uiState.isRightKeyboardVisible) rightKb.start else parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                },
                editorViewModel.webView!!
            )

            BottomBar(
                Modifier.constrainAs(bottomBarRef) {
                    bottom.linkTo(editor.bottom)
                    start.linkTo(editor.start)
                    end.linkTo(editor.end)
                    width = Dimension.fillToConstraints
                },
                color = Color(0xFF1B1B1B),
                lspViewModel = lspViewModel
            )

            if (!isLandscape) {
                Sidebar(
                    showSidebar, { showSidebar = false },
                    {
                        fileViewModel.selectedUri = Uri.parse(it)
                        fileViewModel.currentFilePath = it
                        showSidebar = false
                    },
                    fileViewModel.currentFilePath,
                    listOf(SidebarTab.FILES, SidebarTab.SEARCH, SidebarTab.GIT, SidebarTab.EXTENSIONS, SidebarTab.SETTINGS)
                )

            }
        }

        // Extra Keys Row for Portrait Mode (From Block 2)
        if (!isLandscape) {
            val imeInsets = WindowInsets.ime
            val density = androidx.compose.ui.platform.LocalDensity.current
            val isKeyboardVisible = imeInsets.getBottom(density) > 0

            if (isKeyboardVisible) {
                com.bsbeetle.bananaspaghetti.ui.components.ExtraKeysRow(
                    keyboardViewModel = keyboardViewModel,
                    mode = com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType.EDITOR,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E2E2E))
                        .windowInsetsPadding(imeInsets)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        // ---------------- POPUP OVERLAY ----------------
        SlideLanguagePopup(
            missingLanguageExt = missingLanguageExt,
            show = showLanguagePopup,
            onInstall = {
                showLanguagePopup = false
                navController.navigate(Screen.Extensions.route)
            },
            onDismiss = {
                showLanguagePopup = false
            }
        )
        
        // File Menu Dialog
        FileMenuDialog(
            show = showFileMenu,
            onDismiss = { showFileMenu = false },
            bufferViewModel = bufferViewModel,
            fileViewModel = fileViewModel,
            onFileOpened = { documentFile ->
                // Add file to buffer and load it
                if (bufferViewModel.filesOpenedList.none { it.uri == documentFile.uri }) {
                    bufferViewModel.filesOpenedList.add(documentFile)
                }
                fileViewModel.selectedUri = documentFile.uri
                fileViewModel.currentFilePath = documentFile.uri.toString()
                bufferViewModel.currentFile.value = documentFile
                
                val content = readFromFile(context, documentFile.uri)
                bufferViewModel.bufferLine[documentFile] = content
                loadContentWhenReady(editorViewModel.webView, content)
            },
            onFolderOpened = { folder ->
                // Create new editor session for folder
                // Session auto-registers and sets activeSessionIndex in its init{}
                val newSession = EditorSession(context.applicationContext, folder.name ?: "Project")
                newSession.projectRoot = folder
                
                // Update FileViewModel to show new folder in file tree
                fileViewModel.selectedFolderUri = folder.uri
                
                // Set up editor with new session's WebView
                editorViewModel.setEditorWebView(newSession.getOrInitView())
                
                // Navigate to editor with new session
                navController.navigate(Screen.OpenFolder.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            }
        )
    }
}

@Composable
fun SlideLanguagePopup(
    missingLanguageExt: String,
    show: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = show,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                Modifier
                    .width(340.dp)
                    .background(Color(0xFF1E1E1E))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Language support missing",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Support for .$missingLanguageExt is not installed.\nInstall to enable syntax highlighting, autocomplete & LSP.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Button(
                            onClick = onInstall,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F7BDE))
                        ) { Text("Install") }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A))
                        ) { Text("Maybe later") }
                    }
                }
            }
        }
    }
}