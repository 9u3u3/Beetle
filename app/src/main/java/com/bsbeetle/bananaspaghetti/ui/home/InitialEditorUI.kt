//package com.bsbeetle.bananaspaghetti.ui.home
//
//import android.annotation.SuppressLint
//import android.content.res.Configuration
//import android.net.Uri
//import android.util.Log
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalConfiguration
//import androidx.compose.ui.platform.LocalContext
//import androidx.constraintlayout.compose.ConstraintLayout
//import androidx.constraintlayout.compose.Dimension
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.navigation.NavController
//import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
//import com.bsbeetle.bananaspaghetti.LocalFileViewModel
//import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
//import com.bsbeetle.bananaspaghetti.LocalSliderViewModel
//import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
//import com.bsbeetle.bananaspaghetti.data.TopBarButtons
//import com.bsbeetle.bananaspaghetti.data.sidebar.Sidebar
//import com.bsbeetle.bananaspaghetti.data.sidebar.SidebarTab
//import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
//import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
//import com.bsbeetle.bananaspaghetti.ui.components.TopBar
//import com.bsbeetle.bananaspaghetti.ui.components.TopLeftBox
//import com.bsbeetle.bananaspaghetti.ui.components.TopRightBox
//import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
//import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
//import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
//import com.bsbeetle.bananaspaghetti.utils.readFromFile
//import com.bsbeetle.bananaspaghetti.utils.saveFile
//import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
//import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
//import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
//import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel
//
//@SuppressLint("RememberReturnType")
//@Composable
//fun InitialEditorUI(navController: NavController) {
//
//    val fileViewModel: FileViewModel = LocalFileViewModel.current
//    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
//    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current
//    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
//
//    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
//    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()
//    val context = LocalContext.current
//    val configuration = LocalConfiguration.current
//    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
//    val layout = if (uiState.toggleKeyboard) KeyboardLayout.developerLayout else KeyboardLayout.qwertyLayout
//
//    var showSidebar by rememberSaveable { mutableStateOf(false) }
//
//    editorViewModel.init()
//
//    LaunchedEffect(fileViewModel.selectedUri) {
//        if (fileViewModel.selectedUri != null) {
//            loadContentWhenReady(editorViewModel.webView, readFromFile(context, fileViewModel.selectedUri))
//        }
//    }
//
//    val buttonsForLeftTopBar = listOf(
//        TopBarButtons("Save", onClick = { saveFile(fileViewModel, context, editorViewModel) }),
//        TopBarButtons("Sidebar", onClick = { showSidebar = !showSidebar })
//    )
//
//    val buttonsForRightTopBar = listOf(
//        TopBarButtons("Settings", onClick = { navController.navigate("settings") })
//    )
//
//    Box {
//        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
//            val (
//                topLeftBarRef,
//                topMiddleBarRef,
//                topRightBarRef,
//                topLeftRef,
//                topRightRef,
//                leftKeyboardRef,
//                codeEditor,
//                rightKeyboardRef,
//                errorLogRef
//            ) = createRefs()
//
//            // Left Top Bar
//            TopBar(
//                modifier = Modifier.constrainAs(topLeftBarRef) {
//                    top.linkTo(parent.top)
//                    start.linkTo(parent.start)
//                    end.linkTo(topMiddleBarRef.start)
//                    width = Dimension.percent(0.2f)
//                    height = Dimension.wrapContent
//                },
//                color = Color(0xFF1B1B1B),
//                buttons = buttonsForLeftTopBar,
//            )
//
//            // Middle Top Bar
//            TopBar(
//                modifier = Modifier.constrainAs(topMiddleBarRef) {
//                    top.linkTo(parent.top)
//                    start.linkTo(topLeftBarRef.end)
//                    end.linkTo(topRightBarRef.start)
//                    width = Dimension.percent(0.6f)
//                    height = Dimension.wrapContent
//                },
//                color = Color(0xFF1B1B1B),
//                context = context,
//            )
//
//            // Right Top Bar
//            TopBar(
//                modifier = Modifier.constrainAs(topRightBarRef) {
//                    top.linkTo(parent.top)
//                    start.linkTo(topMiddleBarRef.end)
//                    end.linkTo(parent.end)
//                    width = Dimension.percent(0.2f)
//                    height = Dimension.wrapContent
//                },
//                color = Color(0xFF1B1B1B),
//                buttons = buttonsForRightTopBar,
//            )
//
//            // Optional top boxes in landscape
//            if (isLandscape) {
//                TopLeftBox(
//                    modifier = Modifier.constrainAs(topLeftRef) {
//                        top.linkTo(topLeftBarRef.bottom)
//                        bottom.linkTo(leftKeyboardRef.top)
//                        start.linkTo(parent.start)
//                        end.linkTo(codeEditor.start)
//                        width = Dimension.fillToConstraints
//                        height = Dimension.fillToConstraints
//                    },
//                    editorViewModel.webView!!
//                )
//
//                TopRightBox(
//                    modifier = Modifier.constrainAs(topRightRef) {
//                        top.linkTo(topRightBarRef.bottom)
//                        bottom.linkTo(rightKeyboardRef.top)
//                        start.linkTo(codeEditor.start)
//                        end.linkTo(parent.end)
//                        width = Dimension.fillToConstraints
//                        height = Dimension.fillToConstraints
//                    }
//                )
//
//                if (uiState.isLeftKeyboardVisible) {
//                    SplitKeyboardHalf(
//                        layout = layout,
//                        side = KeyboardSide.LEFT,
//                        isCapsLockOn = uiState.isCapsLockOn,
//                        isShiftOn = uiState.isShiftOn,
//                        isCtrlOn = uiState.isCtrlOn,
//                        onKeyEvent = keyboardViewModel::onKeyEvent,
//                        modifier = Modifier.constrainAs(leftKeyboardRef) {
//                            start.linkTo(parent.start)
//                            bottom.linkTo(parent.bottom)
//                            width = Dimension.percent(sliderValue / 100f)
//                        }
//                    )
//                }
//
//                if (uiState.isRightKeyboardVisible) {
//                    SplitKeyboardHalf(
//                        layout = layout,
//                        side = KeyboardSide.RIGHT,
//                        isCapsLockOn = uiState.isCapsLockOn,
//                        isShiftOn = uiState.isShiftOn,
//                        isCtrlOn = uiState.isCtrlOn,
//                        onKeyEvent = keyboardViewModel::onKeyEvent,
//                        modifier = Modifier.constrainAs(rightKeyboardRef) {
//                            end.linkTo(parent.end)
//                            bottom.linkTo(parent.bottom)
//                            width = Dimension.percent(sliderValue / 100f)
//                        }
//                    )
//                }
//            }
//
//            // Shared Editor composable (works for both orientations)
//            Editor(
//                modifier = Modifier.constrainAs(codeEditor) {
//                    top.linkTo(topMiddleBarRef.bottom)
//                    bottom.linkTo(parent.bottom)
//
//                    if (isLandscape && uiState.isLeftKeyboardVisible) {
//                        start.linkTo(leftKeyboardRef.end)
//                    } else {
//                        start.linkTo(parent.start)
//                    }
//
//                    if (isLandscape && uiState.isRightKeyboardVisible) {
//                        end.linkTo(rightKeyboardRef.start)
//                    } else {
//                        end.linkTo(parent.end)
//                    }
//
//                    width = Dimension.fillToConstraints
//                    height = Dimension.fillToConstraints
//                },
//                editorViewModel.webView!!
//            )
//
//            // Sidebar Overlay (visible in all orientations)
//            // Bottom Bar
//            BottomBar(
//                modifier = Modifier
//                    .background(Color.DarkGray)
//                    .constrainAs(errorLogRef) {
//                        bottom.linkTo(codeEditor.bottom)
//                        start.linkTo(codeEditor.start)
//                        end.linkTo(codeEditor.end)
//                        width = Dimension.fillToConstraints
//                    },
//                color = Color(0xFF245895)
//            )
//
//            if(!isLandscape){
//                Sidebar(
//                    isVisible = showSidebar,
//                    onDismiss = { showSidebar = false },
//                    onFileSelected = { filePath ->
//                        Log.d("Sidebar", "File selected: $filePath")
//                        // ADD THESE:
//                        fileViewModel.selectedUri = Uri.parse(filePath) // Convert path to Uri
//                        showSidebar = false // Auto-dismiss sidebar
//                    },
//                    currentPath = fileViewModel.selectedUri?.path,
//                    enabledTabs = listOf(
//                        SidebarTab.FILES,
//                        SidebarTab.SEARCH,
//                        SidebarTab.GIT,
//                        SidebarTab.EXTENSIONS,
//                        SidebarTab.SETTINGS
//                    )
//                )
//            }
//        }
//    }
//}


package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalBufferViewModel
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalExpandViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.LocalSliderViewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
import com.bsbeetle.bananaspaghetti.data.sidebar.Sidebar
import com.bsbeetle.bananaspaghetti.data.sidebar.SidebarTab
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
import com.bsbeetle.bananaspaghetti.ui.components.TopLeftBox
import com.bsbeetle.bananaspaghetti.ui.components.TopRightBox
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.utils.readFromFile
import com.bsbeetle.bananaspaghetti.utils.saveFile
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.ExpandViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

//@SuppressLint("RememberReturnType")
//@Composable
//fun InitialEditorUI(navController: NavController) {
//
//    val fileViewModel: FileViewModel = LocalFileViewModel.current
//    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
//    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current
//    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
//    val expandViewModel: ExpandViewModel = LocalExpandViewModel.current
//    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current
//
//
//    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
//    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()
//    val context = LocalContext.current
//    val configuration = LocalConfiguration.current
//    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
//    val layout = if (uiState.toggleKeyboard) KeyboardLayout.developerLayout else KeyboardLayout.qwertyLayout
//
//    var showSidebar by rememberSaveable { mutableStateOf(false) }
//
//    editorViewModel.init()
//
////    LaunchedEffect(fileViewModel.selectedUri) {
////        if (fileViewModel.selectedUri != null) {
////            loadContentWhenReady(editorViewModel.webView, readFromFile(context, fileViewModel.selectedUri))
////            fileViewModel.currentFilePath = fileViewModel.selectedUri.toString()
////        }
////    }
//
//    LaunchedEffect(fileViewModel.selectedUri) {
//        if (fileViewModel.selectedUri != null) {
//            val content = readFromFile(context, fileViewModel.selectedUri)
//            loadContentWhenReady(editorViewModel.webView, content)
//            fileViewModel.currentFilePath = fileViewModel.selectedUri.toString()
//
//            // ADD TO BUFFER
//            val documentFile = DocumentFile.fromSingleUri(context, fileViewModel.selectedUri!!)
//            if (documentFile != null) {
//                // Check if file is already in buffer
//                val existingFile = bufferViewModel.filesOpenedList.find {
//                    it.uri == documentFile.uri
//                }
//                if (existingFile == null) {
//                    bufferViewModel.filesOpenedList.add(documentFile)
//                    bufferViewModel.bufferLine[documentFile] = content
//                } else {
//                    // Update existing buffer content
//                    bufferViewModel.bufferLine[existingFile] = content
//                }
//            }
//        }
//    }
//
//
//    val buttonsForLeftTopBar = listOf(
//        TopBarButtons("Save", onClick = { saveFile(fileViewModel, context, editorViewModel) }),
//        TopBarButtons("Sidebar", onClick = { showSidebar = !showSidebar })
//    )
//
//    val buttonsForRightTopBar = listOf(
//        TopBarButtons("Settings", onClick = { navController.navigate("settings") })
//    )
//
//    Box {
//        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
//            val (
//                topLeftBarRef,
//                topMiddleBarRef,
//                topRightBarRef,
//                topLeftRef,
//                topRightRef,
//                leftKeyboardRef,
//                codeEditor,
//                rightKeyboardRef,
//                errorLogRef
//            ) = createRefs()
//
//            // Left Top Bar
//            TopBar(
//                modifier = Modifier.constrainAs(topLeftBarRef) {
//                    top.linkTo(parent.top)
//                    start.linkTo(parent.start)
//                    end.linkTo(topMiddleBarRef.start)
//                    width = Dimension.percent(0.2f)
//                    height = Dimension.wrapContent
//                },
//                color = Color(0xFF1B1B1B),
//                buttons = buttonsForLeftTopBar,
//            )
//
//            // Middle Top Bar
//            TopBar(
//                modifier = Modifier.constrainAs(topMiddleBarRef) {
//                    top.linkTo(parent.top)
//                    start.linkTo(topLeftBarRef.end)
//                    end.linkTo(topRightBarRef.start)
//                    width = Dimension.percent(0.6f)
//                    height = Dimension.wrapContent
//                },
//                color = Color(0xFF1B1B1B),
//                context = context,
//            )
//
//            // Right Top Bar
//            TopBar(
//                modifier = Modifier.constrainAs(topRightBarRef) {
//                    top.linkTo(parent.top)
//                    start.linkTo(topMiddleBarRef.end)
//                    end.linkTo(parent.end)
//                    width = Dimension.percent(0.2f)
//                    height = Dimension.wrapContent
//                },
//                color = Color(0xFF1B1B1B),
//                buttons = buttonsForRightTopBar,
//            )
//
//            // Optional top boxes in landscape
//            if (isLandscape) {
//                TopLeftBox(
//                    modifier = Modifier.constrainAs(topLeftRef) {
//                        top.linkTo(topLeftBarRef.bottom)
//                        bottom.linkTo(leftKeyboardRef.top)
//                        start.linkTo(parent.start)
//                        end.linkTo(codeEditor.start)
//                        width = Dimension.fillToConstraints
//                        height = Dimension.fillToConstraints
//                    },
//                    editorViewModel.webView!!
//                )
//
//                TopRightBox(
//                    modifier = Modifier.constrainAs(topRightRef) {
//                        top.linkTo(topRightBarRef.bottom)
//                        bottom.linkTo(rightKeyboardRef.top)
//                        start.linkTo(codeEditor.start)
//                        end.linkTo(parent.end)
//                        width = Dimension.fillToConstraints
//                        height = Dimension.fillToConstraints
//                    }
//                )
//
//                if (uiState.isLeftKeyboardVisible) {
//                    SplitKeyboardHalf(
//                        layout = layout,
//                        side = KeyboardSide.LEFT,
//                        isCapsLockOn = uiState.isCapsLockOn,
//                        isShiftOn = uiState.isShiftOn,
//                        isCtrlOn = uiState.isCtrlOn,
//                        onKeyEvent = keyboardViewModel::onKeyEvent,
//                        modifier = Modifier.constrainAs(leftKeyboardRef) {
//                            start.linkTo(parent.start)
//                            bottom.linkTo(parent.bottom)
//                            width = Dimension.percent(sliderValue / 100f)
//                        }
//                    )
//                }
//
//                if (uiState.isRightKeyboardVisible) {
//                    SplitKeyboardHalf(
//                        layout = layout,
//                        side = KeyboardSide.RIGHT,
//                        isCapsLockOn = uiState.isCapsLockOn,
//                        isShiftOn = uiState.isShiftOn,
//                        isCtrlOn = uiState.isCtrlOn,
//                        onKeyEvent = keyboardViewModel::onKeyEvent,
//                        modifier = Modifier.constrainAs(rightKeyboardRef) {
//                            end.linkTo(parent.end)
//                            bottom.linkTo(parent.bottom)
//                            width = Dimension.percent(sliderValue / 100f)
//                        }
//                    )
//                }
//            }
//
//            // Shared Editor composable (works for both orientations)
//            Editor(
//                modifier = Modifier.constrainAs(codeEditor) {
//                    top.linkTo(topMiddleBarRef.bottom)
//                    bottom.linkTo(parent.bottom)
//
//                    if (isLandscape && uiState.isLeftKeyboardVisible) {
//                        start.linkTo(leftKeyboardRef.end)
//                    } else {
//                        start.linkTo(parent.start)
//                    }
//
//                    if (isLandscape && uiState.isRightKeyboardVisible) {
//                        end.linkTo(rightKeyboardRef.start)
//                    } else {
//                        end.linkTo(parent.end)
//                    }
//
//                    width = Dimension.fillToConstraints
//                    height = Dimension.fillToConstraints
//                },
//                editorViewModel.webView!!
//            )
//
//            // Bottom Bar
//            BottomBar(
//                modifier = Modifier
//                    .background(Color.DarkGray)
//                    .constrainAs(errorLogRef) {
//                        bottom.linkTo(codeEditor.bottom)
//                        start.linkTo(codeEditor.start)
//                        end.linkTo(codeEditor.end)
//                        width = Dimension.fillToConstraints
//                    },
//                color = Color(0xFF245895)
//            )
//
//            // Sidebar Overlay (portrait only)
//            if (!isLandscape) {
//                Sidebar(
//                    isVisible = showSidebar,
//                    onDismiss = { showSidebar = false },
//                    onFileSelected = { filePath ->
//                        Log.d("Sidebar", "File selected: $filePath")
//                        fileViewModel.selectedUri = Uri.parse(filePath)
//                        fileViewModel.currentFilePath = filePath
//                        showSidebar = false
//                    },
//                    currentPath = fileViewModel.currentFilePath,
//                    enabledTabs = listOf(
//                        SidebarTab.FILES,
//                        SidebarTab.SEARCH,
//                        SidebarTab.GIT,
//                        SidebarTab.EXTENSIONS,
//                        SidebarTab.SETTINGS
//                    )
//                )
//            }
//        }
//    }
//}



@SuppressLint("RememberReturnType")
@Composable
fun InitialEditorUI(navController: NavController) {

    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current
    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
    val expandViewModel: ExpandViewModel = LocalExpandViewModel.current
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layout = if (uiState.toggleKeyboard) KeyboardLayout.developerLayout else KeyboardLayout.qwertyLayout

    var showSidebar by rememberSaveable { mutableStateOf(false) }

    editorViewModel.init()

    LaunchedEffect(fileViewModel.selectedUri) {
        if (fileViewModel.selectedUri != null) {
            val uri = fileViewModel.selectedUri!!
            val documentFile = DocumentFile.fromSingleUri(context, uri)

            if (documentFile != null) {
                // Check if file is already in buffer
                val existingFile = bufferViewModel.filesOpenedList.find {
                    it.uri.toString() == uri.toString()
                }

                if (existingFile == null) {
                    // New file - add to buffer FIRST
                    bufferViewModel.filesOpenedList.add(documentFile)
                    Log.d("Buffer", "Added to buffer: ${documentFile.name}")
                }

                // Read content
                val content = readFromFile(context, uri)

                // Store content in buffer
                bufferViewModel.bufferLine[documentFile] = content

                // Load content in editor
                loadContentWhenReady(editorViewModel.webView, content)

                // Update current file path
                fileViewModel.currentFilePath = uri.toString()

                Log.d("Buffer", "File opened: ${documentFile.name}, buffer size: ${bufferViewModel.filesOpenedList.size}")
            }
        }
    }

    val buttonsForLeftTopBar = listOf(
        TopBarButtons("Save", onClick = { saveFile(fileViewModel, context, editorViewModel) }),
        TopBarButtons("Sidebar", onClick = { showSidebar = !showSidebar })
    )

    val buttonsForRightTopBar = listOf(
        TopBarButtons("Settings", onClick = { navController.navigate("settings") })
    )

    Box {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (
                topLeftBarRef,
                topMiddleBarRef,
                topRightBarRef,
                topLeftRef,
                topRightRef,
                leftKeyboardRef,
                codeEditor,
                rightKeyboardRef,
                errorLogRef
            ) = createRefs()

            // Left Top Bar
            TopBar(
                modifier = Modifier.constrainAs(topLeftBarRef) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(topMiddleBarRef.start)
                    width = Dimension.percent(0.2f)
                    height = Dimension.wrapContent
                },
                color = Color(0xFF1B1B1B),
                buttons = buttonsForLeftTopBar,
            )

            // Middle Top Bar - Buffer Tabs
            TopBar(
                modifier = Modifier.constrainAs(topMiddleBarRef) {
                    top.linkTo(parent.top)
                    start.linkTo(topLeftBarRef.end)
                    end.linkTo(topRightBarRef.start)
                    width = Dimension.percent(0.6f)
                    height = Dimension.wrapContent
                },
                color = Color(0xFF1B1B1B),
                context = context,
                showBufferTabs = true
            )

            // Right Top Bar
            TopBar(
                modifier = Modifier.constrainAs(topRightBarRef) {
                    top.linkTo(parent.top)
                    start.linkTo(topMiddleBarRef.end)
                    end.linkTo(parent.end)
                    width = Dimension.percent(0.2f)
                    height = Dimension.wrapContent
                },
                color = Color(0xFF1B1B1B),
                buttons = buttonsForRightTopBar,
            )

            // Optional top boxes in landscape
            if (isLandscape) {
                TopLeftBox(
                    modifier = Modifier.constrainAs(topLeftRef) {
                        top.linkTo(topLeftBarRef.bottom)
                        bottom.linkTo(leftKeyboardRef.top)
                        start.linkTo(parent.start)
                        end.linkTo(codeEditor.start)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    editorViewModel.webView!!
                )

                TopRightBox(
                    modifier = Modifier.constrainAs(topRightRef) {
                        top.linkTo(topRightBarRef.bottom)
                        bottom.linkTo(rightKeyboardRef.top)
                        start.linkTo(codeEditor.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                )

                if (uiState.isLeftKeyboardVisible) {
                    SplitKeyboardHalf(
                        layout = layout,
                        side = KeyboardSide.LEFT,
                        isCapsLockOn = uiState.isCapsLockOn,
                        isShiftOn = uiState.isShiftOn,
                        isCtrlOn = uiState.isCtrlOn,
                        onKeyEvent = keyboardViewModel::onKeyEvent,
                        modifier = Modifier.constrainAs(leftKeyboardRef) {
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.percent(sliderValue / 100f)
                        }
                    )
                }

                if (uiState.isRightKeyboardVisible) {
                    SplitKeyboardHalf(
                        layout = layout,
                        side = KeyboardSide.RIGHT,
                        isCapsLockOn = uiState.isCapsLockOn,
                        isShiftOn = uiState.isShiftOn,
                        isCtrlOn = uiState.isCtrlOn,
                        onKeyEvent = keyboardViewModel::onKeyEvent,
                        modifier = Modifier.constrainAs(rightKeyboardRef) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.percent(sliderValue / 100f)
                        }
                    )
                }
            }

            // Shared Editor composable (works for both orientations)
            Editor(
                modifier = Modifier.constrainAs(codeEditor) {
                    top.linkTo(topMiddleBarRef.bottom)
                    bottom.linkTo(parent.bottom)

                    if (isLandscape && uiState.isLeftKeyboardVisible) {
                        start.linkTo(leftKeyboardRef.end)
                    } else {
                        start.linkTo(parent.start)
                    }

                    if (isLandscape && uiState.isRightKeyboardVisible) {
                        end.linkTo(rightKeyboardRef.start)
                    } else {
                        end.linkTo(parent.end)
                    }

                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                },
                editorViewModel.webView!!
            )

            // Bottom Bar
            BottomBar(
                modifier = Modifier
                    .background(Color.DarkGray)
                    .constrainAs(errorLogRef) {
                        bottom.linkTo(codeEditor.bottom)
                        start.linkTo(codeEditor.start)
                        end.linkTo(codeEditor.end)
                        width = Dimension.fillToConstraints
                    },
                color = Color(0xFF245895)
            )

            // Sidebar Overlay (portrait only)
            if (!isLandscape) {
                Sidebar(
                    isVisible = showSidebar,
                    onDismiss = { showSidebar = false },
                    onFileSelected = { filePath ->
                        Log.d("Sidebar", "File selected: $filePath")
                        fileViewModel.selectedUri = Uri.parse(filePath)
                        fileViewModel.currentFilePath = filePath
                        showSidebar = false
                    },
                    currentPath = fileViewModel.currentFilePath,
                    enabledTabs = listOf(
                        SidebarTab.FILES,
                        SidebarTab.SEARCH,
                        SidebarTab.GIT,
                        SidebarTab.EXTENSIONS,
                        SidebarTab.SETTINGS
                    )
                )
            }
        }
    }
}