package com.bsbeetle.bananaspaghetti.ui.home

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.LocalSliderViewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.ui.browser.DevBrowserScreen
import com.bsbeetle.bananaspaghetti.ui.components.ExtraKeysRow
import com.bsbeetle.bananaspaghetti.ui.components.LeftTermTopBar
import com.bsbeetle.bananaspaghetti.ui.components.RightTopBar
import com.bsbeetle.bananaspaghetti.ui.components.TopLeftTermBox
import com.bsbeetle.bananaspaghetti.ui.components.TopRightBox
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.DevWebViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

@Composable
fun BrowserScreen(navController: NavController) {

    // Safely check if session exists before accessing
    val sessionIndex = SessionList.activeSessionIndex
    val sessions = SessionList.sessions

    // If index is invalid, navigate back to home
    if (sessionIndex == null || sessionIndex < 0 || sessionIndex >= sessions.size) {
        Log.w(
                "BrowserScreen",
                "Invalid session index: $sessionIndex, sessions size: ${sessions.size}"
        )
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
        return
    }

    val currentSession = sessions.getOrNull(sessionIndex)
    if (currentSession == null || currentSession.view == null) {
        Log.w("BrowserScreen", "Session or view is null, navigating back")
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
        return
    }

    Log.d("BrowserScreen", "Loading the Browser screen, Session type: ${currentSession.type}")

    BackHandler {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current

    // MERGE NOTE: Added DevWebViewModel from Block 1
    val devWebViewModel: DevWebViewModel = viewModel()

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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1B1B1B))) {
        ConstraintLayout(
                modifier =
                        Modifier.fillMaxSize()
                                .then(if (!isLandscape) Modifier.padding(top = 40.dp) else Modifier)
        ) {
            val (topLeft, topRight, leftBox, rightBox, leftKb, editor, rightKb) = createRefs()

            // MERGE NOTE: Kept Block 2's logic for Portrait/Landscape TopBars
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
                // Portrait Top Bar
                RightTopBar(
                        modifier =
                                Modifier.constrainAs(topLeft) {
                                    top.linkTo(parent.top)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                    width = Dimension.fillToConstraints
                                    height = Dimension.wrapContent
                                },
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

            DevBrowserScreen(
                    modifier =
                            Modifier.constrainAs(editor) {
                                // MERGE NOTE: Kept Block 2's smart constraints for Portrait mode
                                if (isLandscape) {
                                    top.linkTo(parent.top)
                                } else {
                                    top.linkTo(topLeft.bottom)
                                }
                                bottom.linkTo(parent.bottom)

                                // If using landscape IME mode, give full width to browser
                                val showLeftKb =
                                        isLandscape &&
                                                uiState.isLeftKeyboardVisible &&
                                                !uiState.useLandscapeIme
                                val showRightKb =
                                        isLandscape &&
                                                uiState.isRightKeyboardVisible &&
                                                !uiState.useLandscapeIme

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
                    // Use the validated currentSession from earlier
                    webView = currentSession.view!!,
                    viewModel = devWebViewModel
            )
        }

        // Extra Keys Row - Shows in portrait mode OR in landscape mode when useLandscapeIme is
        // enabled
        val showExtraKeys = !isLandscape || uiState.useLandscapeIme
        if (showExtraKeys) {
            val imeInsets = WindowInsets.ime
            val density = LocalDensity.current
            val isKeyboardVisible = imeInsets.getBottom(density) > 0

            if (isKeyboardVisible) {
                ExtraKeysRow(
                        keyboardViewModel = keyboardViewModel,
                        mode =
                                com.bsbeetle
                                        .bananaspaghetti
                                        .utils
                                        .sessionHandling
                                        .SessionType
                                        .BROWSER,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(Color(0xFF2E2E2E))
                                        .windowInsetsPadding(imeInsets)
                                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}
