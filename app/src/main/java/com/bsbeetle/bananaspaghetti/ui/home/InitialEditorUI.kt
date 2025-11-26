package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
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
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.LocalSliderViewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.data.sidebar.Sidebar
import com.bsbeetle.bananaspaghetti.data.sidebar.SidebarTab
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.components.LeftTopBar
import com.bsbeetle.bananaspaghetti.ui.components.RightTopBar
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
import com.bsbeetle.bananaspaghetti.ui.components.TopLeftBox
import com.bsbeetle.bananaspaghetti.ui.components.TopRightBox
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.utils.fileHandling.readFromFile
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel


@SuppressLint("RememberReturnType")
@Composable
fun InitialEditorUI(navController: NavController) {

    Log.e("Sessions", SessionList.sessions.toString())


    // This can be buggy
    BackHandler {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.OpenFolder.route) { inclusive = true }
        }
    }

    // View models
    val fileViewModel: FileViewModel = LocalFileViewModel.current
    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current
    val editorViewModel: EditorViewModel = LocalEditorViewModel.current
    val bufferViewModel: BufferViewModel = LocalBufferViewModel.current

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layout = if (uiState.toggleKeyboard) KeyboardLayout.developerLayout else KeyboardLayout.qwertyLayout
    var showSidebar by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(fileViewModel.selectedUri) {
        fileViewModel.selectedUri?.let { uri ->
            DocumentFile.fromSingleUri(context, uri)?.let { documentFile ->
                if (bufferViewModel.filesOpenedList.none { it.uri.toString() == uri.toString() }) {
                    bufferViewModel.filesOpenedList.add(documentFile)
                    Log.d("Buffer", "Added: ${documentFile.name}")
                }

                val content = readFromFile(context, uri)
                bufferViewModel.bufferLine[documentFile] = content
                loadContentWhenReady(editorViewModel.webView, content)
                fileViewModel.currentFilePath = uri.toString()
                Log.d("Buffer", "Opened: ${documentFile.name}, total: ${bufferViewModel.filesOpenedList.size}")
            }
        }
    }

    Box {
        ConstraintLayout(Modifier.fillMaxSize()) {
            val (topLeft, topMiddle, topRight, leftBox, rightBox, leftKb, editor, rightKb, bottomBarRef) = createRefs()

            LeftTopBar(
                modifier = Modifier.constrainAs(topLeft) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(topMiddle.start)
                    width = Dimension.percent(0.2f)
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
                        uiState.isShiftOn, keyboardViewModel::onKeyEvent,
                        Modifier.constrainAs(leftKb) {
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.percent(sliderValue / 100f)
                        }
                            .background(Color(0xFF212121))
                    )
                }

                if (uiState.isRightKeyboardVisible) {
                    SplitKeyboardHalf(
                        layout, KeyboardSide.RIGHT, uiState.isCapsLockOn,
                        uiState.isShiftOn, keyboardViewModel::onKeyEvent,
                        Modifier.constrainAs(rightKb) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.percent(sliderValue / 100f)
                        }
                            .background(Color(0xFF212121))
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
                color = Color(0xFF1B1B1B)
            )

            if (!isLandscape) {
                Sidebar(
                    showSidebar, { showSidebar = false },
                    { fileViewModel.selectedUri = Uri.parse(it); fileViewModel.currentFilePath = it; showSidebar = false },
                    fileViewModel.currentFilePath,
                    listOf(SidebarTab.FILES, SidebarTab.SEARCH, SidebarTab.GIT, SidebarTab.EXTENSIONS, SidebarTab.SETTINGS)
                )
            }
        }
    }
}