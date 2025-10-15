package com.bsbeetle.bananaspaghetti.ui.home

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.LocalSliderViewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.data.tenebrioObj
import com.bsbeetle.bananaspaghetti.tenebrio.pty.Tenebrio
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.utils.TerminalWebView
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

@Composable
fun InitialTerminalUI() {

    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val layout = if (uiState.toggleKeyboard) KeyboardLayout.developerLayout else KeyboardLayout.qwertyLayout

    tenebrioObj.tenebrio = Tenebrio(context)
    val webView: WebView = TerminalWebView.getWebview(context, tenebrioObj.tenebrio!!)

    Box {

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (topLeftBarRef,
                topMiddleBarRef,
                topRightBarRef,
                leftKeyboardRef,
                codeEditor,
                rightKeyboardRef,
            ) = createRefs()

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
            )


            if (uiState.isLeftKeyboardVisible) {
                key(sliderValue) {
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
                        if (uiState.isLeftKeyboardVisible){start.linkTo(leftKeyboardRef.end)}
                        else {start.linkTo(parent.start)}
                        if (uiState.isRightKeyboardVisible){end.linkTo(rightKeyboardRef.start)}
                        else{end.linkTo(parent.end)}
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                webView = webView
            )

            if (uiState.isRightKeyboardVisible) {
                key(sliderValue) {
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
                            width =
                                Dimension.percent(sliderValue / 100f)
                        }
                    )
                }
            }
        }
    }
}