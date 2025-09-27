package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.data.TopBarButtons
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
import com.bsbeetle.bananaspaghetti.ui.components.TopLeftBox
import com.bsbeetle.bananaspaghetti.ui.components.TopRightBox
import com.bsbeetle.bananaspaghetti.utils.EditorWebView
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.utils.readFromFile
import com.bsbeetle.bananaspaghetti.utils.saveFile
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel
import java.io.FileNotFoundException
import java.io.OutputStreamWriter


@SuppressLint("RememberReturnType")
@Composable
fun InitialEditorUI(
    fileViewModel: FileViewModel,
    keyboardViewModel: KeyboardViewModel,
    sliderViewModel: SliderViewModel,
    navController: NavController,
    bufferViewModel: BufferViewModel
) {

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webView: WebView = EditorWebView.getWebview(context)
    val layout = if (uiState.toggleKeyboard) KeyboardLayout.developerLayout else KeyboardLayout.qwertyLayout

    if(fileViewModel.selectedUri != null) {
        loadContentWhenReady(webView, readFromFile(context, fileViewModel.selectedUri))
    }

    val buttonsForLeftTopBar = listOf(
        TopBarButtons("Save", { saveFile(fileViewModel, context) })
    )

    val buttonsForRightTopBar = listOf(TopBarButtons("Settings", onClick = {
        navController.navigate("settings")
    }))


    Box {

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (topLeftBarRef,
                topMiddleBarRef,
                topRightBarRef,
                topLeftRef,
                topRightRef,
                leftKeyboardRef,
                codeEditor,
                rightKeyboardRef,
                errorLogRef) = createRefs()

            TopLeftBox(
                modifier = Modifier
                    .constrainAs(topLeftRef) {
                        top.linkTo(topLeftBarRef.bottom)
                        bottom.linkTo(leftKeyboardRef.top)
                        start.linkTo(parent.start)
                        end.linkTo(codeEditor.start)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                fileViewModel,
                bufferViewModel
            )

            TopRightBox(
                modifier = Modifier
                    .constrainAs(topRightRef) {
                        top.linkTo(topRightBarRef.bottom)
                        bottom.linkTo(rightKeyboardRef.top)
                        start.linkTo(codeEditor.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
            )

            //Left Top Bar
            TopBar(
                modifier = Modifier
                    .constrainAs(topLeftBarRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(topMiddleBarRef.start)
                        width = Dimension.percent(0.2f)
                        height = Dimension.wrapContent
                    },
                Color(0xFF212121),
                buttonsForLeftTopBar
            )

            //Middle Top Bar
            TopBar(
                modifier = Modifier
                    .constrainAs(topMiddleBarRef) {
                        top.linkTo(parent.top)
                        start.linkTo(topLeftBarRef.end)
                        end.linkTo(topRightBarRef.start)
                        width = Dimension.percent(0.6f)
                        height = Dimension.wrapContent
                    },
                color = Color.Gray,
                bufferViewModel = bufferViewModel,
                context = context
            )

            //Right Top Bar
            TopBar(
                modifier = Modifier
                    .constrainAs(topRightBarRef) {
                        top.linkTo(parent.top)
                        start.linkTo(topMiddleBarRef.end)
                        end.linkTo(parent.end)
                        width = Dimension.percent(0.2f)
                        height = Dimension.wrapContent
                    },
                color = Color(0xFF212121),
                buttonsForRightTopBar
            )


            if (uiState.isLeftKeyboardVisible) {
                key(sliderValue) {
                    SplitKeyboardHalf(
                        layout = layout,
                        side = KeyboardSide.LEFT,
                        isCapsLockOn = uiState.isCapsLockOn,
                        isShiftOn = uiState.isShiftOn,
                        onKeyEvent = keyboardViewModel::onKeyEvent,
                        modifier = Modifier.constrainAs(leftKeyboardRef) {
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                            width =
                                Dimension.percent(sliderValue / 100f)
                        }
                    )
                }
            }

            Editor(
                modifier = Modifier
                    .constrainAs(codeEditor) {
                        top.linkTo(topMiddleBarRef.bottom)
                        bottom.linkTo(parent.bottom)
                        if (uiState.isLeftKeyboardVisible) {
                            start.linkTo(leftKeyboardRef.end)
                        } else {
                            start.linkTo(parent.start)
                        }
                        if (uiState.isRightKeyboardVisible) {
                            end.linkTo(rightKeyboardRef.start)
                        } else {
                            end.linkTo(parent.end)
                        }
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                keyboardViewModel,
                webView = webView
            )

            if (uiState.isRightKeyboardVisible) {
                key(sliderValue) {
                    SplitKeyboardHalf(
                        layout = layout,
                        side = KeyboardSide.RIGHT,
                        isCapsLockOn = uiState.isCapsLockOn,
                        isShiftOn = uiState.isShiftOn,
                        onKeyEvent = keyboardViewModel::onKeyEvent,
                        modifier = Modifier.constrainAs(rightKeyboardRef) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width =
                                Dimension.percent(sliderValue / 100f)
                        }
                    )
                }
            }

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

        }
    }
}