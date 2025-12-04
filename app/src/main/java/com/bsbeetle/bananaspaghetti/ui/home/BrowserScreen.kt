package com.bsbeetle.bananaspaghetti.ui.home

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.LocalSliderViewModel
import com.bsbeetle.bananaspaghetti.data.KeyboardLayout
import com.bsbeetle.bananaspaghetti.data.sidebar.Sidebar
import com.bsbeetle.bananaspaghetti.data.sidebar.SidebarTab
import com.bsbeetle.bananaspaghetti.ui.browser.DevBrowserScreen
import com.bsbeetle.bananaspaghetti.ui.components.LeftTermTopBar
import com.bsbeetle.bananaspaghetti.ui.components.RightTopBar
import com.bsbeetle.bananaspaghetti.ui.components.TopLeftTermBox
import com.bsbeetle.bananaspaghetti.ui.components.TopRightBox
import com.bsbeetle.bananaspaghetti.ui.keyboard.KeyboardSide
import com.bsbeetle.bananaspaghetti.ui.keyboard.SplitKeyboardHalf
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

@Composable
fun BrowserScreen(navController: NavController) {

    Log.d("BrowserScreen", "Loading the Browser screen, Session type: ${SessionList.sessions[SessionList.activeSessionIndex!!].type}")

    BackHandler {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Browser.route) { inclusive = true }
        }
    }

    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val sliderViewModel: SliderViewModel = LocalSliderViewModel.current

    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val sliderValue by sliderViewModel.sliderValue.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showSidebar by rememberSaveable { mutableStateOf(false) }

    val layout = if (uiState.toggleKeyboard) {
        KeyboardLayout.developerLayout
    } else {
        KeyboardLayout.qwertyLayout
    }

    Box {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {

            val (topLeft, topRight, leftBox, rightBox, leftKb, editor, rightKb) = createRefs()

            if (isLandscape) {
                LeftTermTopBar(
                    modifier = Modifier.constrainAs(topLeft) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(editor.start)
                        width = Dimension.fillToConstraints
                        height = Dimension.wrapContent
                    }
                )

                RightTopBar(
                    modifier = Modifier.constrainAs(topRight) {
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
                    modifier = Modifier.constrainAs(topLeft) {
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

            DevBrowserScreen(
                modifier = Modifier.constrainAs(editor) {
                    if (isLandscape) {
                        top.linkTo(parent.top)
                    } else {
                        top.linkTo(topLeft.bottom)
                    }
                    bottom.linkTo(parent.bottom)

                    // adapt constraints depending on keyboard visibility & orientation
                    if (isLandscape && uiState.isLeftKeyboardVisible) {
                        start.linkTo(leftKb.end)
                    } else {
                        start.linkTo(parent.start)
                    }

                    if (isLandscape && uiState.isRightKeyboardVisible) {
                        end.linkTo(rightKb.start)
                    } else {
                        end.linkTo(parent.end)
                    }

                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                },
                webView = SessionList.sessions[SessionList.activeSessionIndex!!].view!!
            )

            if(!isLandscape){
                Sidebar(
                    isVisible = showSidebar,
                    onDismiss = { showSidebar = false },
                    enabledTabs = listOf(
                        SidebarTab.EXTENSIONS,
                        SidebarTab.SETTINGS
                    )
                )

            }
        }

        if (!isLandscape) {
            // Extra Keys Row for Portrait Mode
            val imeInsets = androidx.compose.foundation.layout.WindowInsets.ime
            val density = androidx.compose.ui.platform.LocalDensity.current
            val isKeyboardVisible = imeInsets.getBottom(density) > 0

            if (isKeyboardVisible) {
                com.bsbeetle.bananaspaghetti.ui.components.ExtraKeysRow(
                    keyboardViewModel = keyboardViewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E2E2E))
                        .windowInsetsPadding(imeInsets)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }

}