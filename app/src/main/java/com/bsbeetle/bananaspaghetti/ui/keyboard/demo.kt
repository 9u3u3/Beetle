package com.bsbeetle.bananaspaghetti.ui.keyboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.LocalSliderViewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

@Composable
fun DemoEditorUi(navController: NavController) {

    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current
    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()
    val demoSession: Session = SessionList.sessions[SessionList.activeSessionIndex!!]

    BackHandler {
        navController.popBackStack()
    }

    DisposableEffect(Unit) {
        onDispose {
            demoSession.destroy()
        }
    }

    // for selecting the layout.
    val layout = when (uiState.keyboardPage) {
        0 -> KeyboardLayout.qwertyLayout    // Alphabets
        1 -> KeyboardLayout.editorLayout    // Symbols
        else -> KeyboardLayout.terminalLayout // Navigation
    }

    Box {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (leftKeyboardRef, codeEditor, rightKeyboardRef, bottomBarRef) = createRefs()

            if (uiState.isLeftKeyboardVisible) {
                // Left Keyboard - positioned at bottom left
                // Key is helped to listen to any changes that occur and recompose the UI accordingly
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
                                Dimension.percent(sliderValue / 100f) // 20% width for left keyboard
                        }
                    )
                }
            }

            // Code Editor - positioned in the center, full height
            Editor(
                modifier = Modifier
                    .constrainAs(codeEditor) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        // Dynamically set the start constraint
                        if (uiState.isLeftKeyboardVisible) {
                            start.linkTo(leftKeyboardRef.end)
                        } else {
                            start.linkTo(parent.start)
                        }

                        // Dynamically set the end constraint
                        if (uiState.isRightKeyboardVisible) {
                            end.linkTo(rightKeyboardRef.start)
                        } else {
                            end.linkTo(parent.end)
                        }
                        width = Dimension.fillToConstraints // Takes remaining 60% width
                        height = Dimension.fillToConstraints
                    },
                webView = demoSession.view!!
            )

            if (uiState.isRightKeyboardVisible) {
                // Right Keyboard - positioned at bottom right
                // Key is helped to listen to any changes that occur and recompose the UI accordingly

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
                                Dimension.percent(sliderValue / 100f) // 20% width for right keyboard
                        }
                    )
                }
            }

            BottomBar(
                modifier = Modifier
                    .height(16.dp)
                    .constrainAs(bottomBarRef) {
                        bottom.linkTo(codeEditor.bottom)   // stick to editor’s bottom edge
                        start.linkTo(codeEditor.start)     // align with editor
                        end.linkTo(codeEditor.end)
                        width = Dimension.fillToConstraints
                    },
                color = Color(0xFF2D57A8)

            )
        }
        // Overlay Slider (always on top)
        Box(
            modifier = Modifier // <- Position it
                .background(Color.Transparent)
                .height(200.dp)                // or .heightIn(min = 3.dp)
                .fillMaxSize(fraction = 0.4f) // optional: only take 10% height
                .padding(vertical = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            SmoothSlider(sliderViewModel)
        }
    }
}
