package com.bsbeetle.bananaspaghetti.ui.home

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.LocalSliderViewModel
import com.bsbeetle.bananaspaghetti.LocalBrowserViewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.cmeditor.Editor
import com.bsbeetle.bananaspaghetti.ui.components.LeftTermTopBar
import com.bsbeetle.bananaspaghetti.ui.components.RightTopBar
import com.bsbeetle.bananaspaghetti.ui.components.TerminalPortraitTopBar
import com.bsbeetle.bananaspaghetti.ui.components.TerminalSidebar
import com.bsbeetle.bananaspaghetti.ui.components.TopLeftTermBox
import com.bsbeetle.bananaspaghetti.ui.components.TopRightBox
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

@Composable
fun InitialTerminalUI(navController: NavController) {

    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showSidebar by rememberSaveable { mutableStateOf(false) }

    val layout =
            when (uiState.keyboardPage) {
                0 -> KeyboardLayout.qwertyLayout // Alphabets
                1 -> KeyboardLayout.editorLayout // Symbols
                else -> KeyboardLayout.terminalLayout // Navigation
            }

    BackHandler {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    // Apply terminal font size when it changes
    val terminalSession = remember {
        SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
    }
    
    androidx.compose.runtime.LaunchedEffect(uiState.terminalFontSize) {
        terminalSession?.getOrInitView()?.evaluateJavascript(
            "if(window.setFontSize) window.setFontSize(${uiState.terminalFontSize});",
            null
        )
    }

    // Apply bottom padding when IME keyboard is visible (portrait mode)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeHeight = imeInsets.getBottom(density)

    LaunchedEffect(imeHeight) {
        val paddingPx = imeHeight
        if (paddingPx > 0) {
            terminalSession?.getOrInitView()?.evaluateJavascript(
                "if(window.setBottomPadding) window.setBottomPadding($paddingPx);",
                null
            )
        } else {
            terminalSession?.getOrInitView()?.evaluateJavascript(
                "if(window.setBottomPadding) window.setBottomPadding(0);",
                null
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1B1B1B))) {
        ConstraintLayout(
                modifier = Modifier
                        .fillMaxSize()
                        .then(if (!isLandscape) Modifier.padding(top = 40.dp) else Modifier)
        ) {
            val (topLeft, topRight, leftBox, rightBox, leftKb, editor, rightKb) = createRefs()

            if (isLandscape) {
                LeftTermTopBar(
                        modifier =
                                Modifier.constrainAs(topLeft) {
                                    top.linkTo(parent.top)
                                    start.linkTo(parent.start)
                                    end.linkTo(editor.start)
                                    width = Dimension.fillToConstraints
                                    height = Dimension.wrapContent
                                }
                )

                RightTopBar(
                        modifier =
                                Modifier.constrainAs(topRight) {
                                    top.linkTo(parent.top)
                                    start.linkTo(editor.end)
                                    end.linkTo(parent.end)
                                    width = Dimension.fillToConstraints
                                    height = Dimension.wrapContent
                                },
                        navController = navController
                )
            } else {
                // Portrait Top Bar - Custom terminal top bar with menu and exit buttons
                TerminalPortraitTopBar(
                        modifier =
                                Modifier.constrainAs(topLeft) {
                                    top.linkTo(parent.top)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                    width = Dimension.fillToConstraints
                                    height = Dimension.wrapContent
                                },
                        onMenuClick = { showSidebar = !showSidebar },
                        navController = navController
                )
            }

            if (isLandscape) {
                TopLeftTermBox(
                        Modifier.constrainAs(leftBox) {
                            top.linkTo(topRight.bottom)
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

                // Only show custom keyboards if NOT using landscape IME mode
                if (uiState.isLeftKeyboardVisible && !uiState.useLandscapeIme) {
                    SplitKeyboardHalf(
                            layout = layout,
                            side = KeyboardSide.LEFT,
                            isCapsLockOn = uiState.isCapsLockOn,
                            isShiftOn = uiState.isShiftOn,
                            isCtrlOn = uiState.isCtrlOn,
                            onKeyEvent = keyboardViewModel::onKeyEvent,
                            modifier = Modifier.constrainAs(leftKb) {
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
                            modifier = Modifier.constrainAs(rightKb) {
                                        end.linkTo(parent.end)
                                        bottom.linkTo(parent.bottom)
                                        width = Dimension.percent(uiState.rightKeyboardWidth)
                                    }
                                    .background(Color(0xFF212121)),
                            keyboardHeightFraction = uiState.rightKeyboardHeight
                    )
                }
            }

            // Editor extended to the top
            Editor(
                    modifier =
                            Modifier.constrainAs(editor) {
                                if (isLandscape) {
                                    top.linkTo(parent.top)
                                } else {
                                    top.linkTo(topLeft.bottom)
                                }
                                bottom.linkTo(parent.bottom)

                                // adapt constraints depending on keyboard visibility & orientation
                                val showLeftKb = isLandscape && uiState.isLeftKeyboardVisible && !uiState.useLandscapeIme
                                val showRightKb = isLandscape && uiState.isRightKeyboardVisible && !uiState.useLandscapeIme
                                
                                if (showLeftKb) {
                                    start.linkTo(leftKb.end)
                                } else {
                                    start.linkTo(parent.start)
                                }

                                if (showRightKb) {
                                    end.linkTo(rightKb.start)
                                } else {
                                    end.linkTo(parent.end)
                                }

                                width = Dimension.fillToConstraints
                                height = Dimension.fillToConstraints
                            },
                    // Change the webview used here
                    webView = SessionList.sessions[SessionList.activeSessionIndex!!].getOrInitView()
            )
        }

        // Terminal Sidebar for portrait mode (simplified with only quick actions)
        if (!isLandscape) {
            TerminalSidebar(
                    isVisible = showSidebar,
                    onDismiss = { showSidebar = false },
                    onZoomOutClick = {
                        navController.navigate(Screen.ZoomOut.route)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
            )
        }
        // Extra Keys Row - Shows in portrait mode OR in landscape mode when useLandscapeIme is enabled
        val showExtraKeys = !isLandscape || uiState.useLandscapeIme
        if (showExtraKeys) {
            val imeInsets = androidx.compose.foundation.layout.WindowInsets.ime
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
                                        .TERMINAL,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(Color(0xFF2E2E2E))
                                        .windowInsetsPadding(
                                                imeInsets
                                        ) // Push up by keyboard height
                                        .align(Alignment.BottomCenter) // Align to bottom of Box
                )
            }
        }
    }
}
