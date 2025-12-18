package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.ImageLoader
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalBrowserViewModel
import com.bsbeetle.bananaspaghetti.LocalTerminalViewModel
import androidx.compose.ui.zIndex
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.components.GridBackground
import com.bsbeetle.bananaspaghetti.ui.components.LiquidGlassBox
import com.bsbeetle.bananaspaghetti.ui.components.VignetteEffect
import com.bsbeetle.bananaspaghetti.ui.components.beetleAscii
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.ui.theme.KarmaticFont
import com.bsbeetle.bananaspaghetti.ui.theme.born2bsporty
import com.bsbeetle.bananaspaghetti.ui.theme.codaRegular
import com.bsbeetle.bananaspaghetti.utils.fileHandling.fileLauncher
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.BrowserSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import com.bsbeetle.bananaspaghetti.viewmodel.TerminalViewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@SuppressLint("RestrictedApi")
@Composable
fun HomeScreen(navController: NavController) {

    Log.e("Sessions", SessionList.sessions.toString())

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isRecentsOpen by rememberSaveable { mutableStateOf(false) }
    val mainMenuOffset by
            animateDpAsState(
                    targetValue = if (isRecentsOpen) {
            if (isLandscape) 150.dp else (-150).dp
        } else {
            0.dp
                    },
        animationSpec = tween(durationMillis = 400),
        label = "mainMenuOffset"
    ) // Animated offset or main menu
    val recentsPanelOffset by animateDpAsState(
        targetValue = if (isLandscape) {
            if (isRecentsOpen) 0.dp else (-300).dp
        } else {
            if (isRecentsOpen) 0.dp else 350.dp
        },
                    label = "recentsPanelOffset"
            ) // Animated offset for recent panel
    val context: Context = LocalContext.current
    val browserViewModel = LocalBrowserViewModel.current

    // Terminal install check
    var showTerminalDialog by rememberSaveable { mutableStateOf(false) }
    var hasCheckedTerminal by rememberSaveable { mutableStateOf(false) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasCheckedTerminal) {
            hasCheckedTerminal = true
            val isInstalled = com.bsbeetle.bananaspaghetti.ui.components.isTerminalInstalled(context.filesDir)
            if (!isInstalled) {
                showTerminalDialog = true
            }
        }
    }
    
    // Terminal install dialog
    if (showTerminalDialog) {
        com.bsbeetle.bananaspaghetti.ui.components.TerminalInstallDialog(
            onInstallClick = {
                showTerminalDialog = false
                // Navigate to terminal - it will auto-install
                com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession(context.applicationContext, initialDir = null)
                navController.navigate(com.bsbeetle.bananaspaghetti.ui.navigation.Screen.Terminal.route)
            }
        )
    }

    Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (
                    gridLayout,
                    beetleAsciiLeft,
                    beetleAsciiRight,
                    vignette,
                    mainMenu,
                    settings,
                    recents,
                    recentsPanel,
                    browser,
                    windowPreview) =
                    createRefs()

            GridBackground(
                    modifier =
                            Modifier.fillMaxSize().constrainAs(gridLayout) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            }
            )

            beetleAscii(
                    modifier =
                            Modifier.size(400.dp)
                                    .constrainAs(beetleAsciiLeft) {
                                        start.linkTo(parent.start)
                                        bottom.linkTo(parent.bottom)
                                    }
                                    .alpha(0.6f)
            )

            beetleAscii(
                    modifier =
                            Modifier.size(400.dp)
                                    .constrainAs(beetleAsciiRight) {
                                        end.linkTo(parent.end)
                                        top.linkTo(parent.top)
                                    }
                                    .rotate(180f)
                                    .alpha(0.6f)
            )

            VignetteEffect(
                    modifier =
                            Modifier.fillMaxSize().constrainAs(vignette) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            },
                    intensity = 1f
            )

            MainMenu(
                    modifier =
                            Modifier.size(390.dp)
                                    .then(
                                            if (isLandscape)
                                                    Modifier.offset(x = mainMenuOffset, y = 0.dp)
                                            else Modifier.offset(x = 0.dp, y = mainMenuOffset)
                                    )
                                    .constrainAs(mainMenu) {
                                        top.linkTo(parent.top)
                                        bottom.linkTo(parent.bottom)
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                    },
                    navController
            )

            if (isLandscape) {
                Box(
                        modifier =
                                Modifier.width(300.dp)
                                        .fillMaxHeight()
                                        .offset(x = recentsPanelOffset)
                                        .zIndex(10f) // Ensure panel is on top of icons
                        .constrainAs(recentsPanel) {
                                            top.linkTo(parent.top)
                                            bottom.linkTo(parent.bottom)
                                            start.linkTo(parent.start) // Move to left
                                        }
                ) {
                    LiquidGlassBox(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Column(modifier = Modifier.fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Use FileViewModel for recent files state
                            val fileViewModel = com.bsbeetle.bananaspaghetti.LocalFileViewModel.current
                            val recentFiles = fileViewModel.recentFiles

                            android.util.Log.d("HomeScreen", "Recomposing Recent Files (Landscape). Size: ${recentFiles.size}")

                            androidx.compose.runtime.LaunchedEffect(isRecentsOpen) {
                                if (isRecentsOpen) {
                                    fileViewModel.loadRecentFiles(context)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent",
                                    fontFamily = codaRegular,
                                    color = Color(0xFFFFFFFF),
                                    fontSize = 35.sp
                                )

                                Text(
                                    text = "Clear",
                                    fontFamily = born2bsporty,
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .padding(16.dp) // Add padding to increase touch target
                                        .clickable {
                                            android.util.Log.d("HomeScreen", "Clear button clicked! (Landscape)")
                                            fileViewModel.clearRecentFiles(context)
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            androidx.compose.foundation.lazy.LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = recentFiles,
                                    key = { it.uri }
                                ) { file ->
                                    RecentFileItem(
                                        file = file,
                                        navController = navController,
                                        fileViewModel = fileViewModel
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(350.dp) // Increased height for mobile
                                        .offset(y = recentsPanelOffset)
                        .zIndex(10f) // Ensure panel is on top of icons
                                        .constrainAs(recentsPanel) {
                                            bottom.linkTo(parent.bottom)
                                            start.linkTo(parent.start)
                                            end.linkTo(parent.end)
                                        }
                ) {
                    LiquidGlassBox(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Use FileViewModel for recent files state
                            val fileViewModel = com.bsbeetle.bananaspaghetti.LocalFileViewModel.current
                            val recentFiles = fileViewModel.recentFiles

                            android.util.Log.d("HomeScreen", "Recomposing Recent Files. Size: ${recentFiles.size}")

                            androidx.compose.runtime.LaunchedEffect(isRecentsOpen) {
                                if (isRecentsOpen) {
                                    fileViewModel.loadRecentFiles(context)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Recent",
                                    fontFamily = codaRegular,
                                    color = Color(0xFFFFFFFF),
                                    fontSize = 30.sp
                                )

                                Text(
                                    text = "Clear",
                                    fontFamily = born2bsporty,
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(16.dp) // Add padding to increase touch target
                                        .clickable {
                                            android.util.Log.d("HomeScreen", "Clear button clicked!")
                                            fileViewModel.clearRecentFiles(context)
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            androidx.compose.foundation.lazy.LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = recentFiles,
                                    key = { it.uri }
                                ) { file ->
                                    RecentFileItem(
                                        file = file,
                                        navController = navController,
                                        fileViewModel = com.bsbeetle.bananaspaghetti.LocalFileViewModel.current
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Image(
                    painter =
                            painterResource(
                                    R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            ),
                    contentDescription = "Settings",
                    modifier =
                            Modifier.size(20.dp)
                                    .constrainAs(settings) {
                                        top.linkTo(parent.top, margin = 20.dp)
                                        end.linkTo(parent.end, margin = 20.dp)
                                    }
                                    .clickable(
                                            onClick = {
                                                BrowserSession(context)
                                                browserViewModel.setDevWebView(
                                                        SessionList.sessions[
                                                                        SessionList
                                                                                .activeSessionIndex!!]
                                                                .getOrInitView()
                                                )
                                                navController.navigate(Screen.Settings.route)
                                            }
                                    )
            )

            Image(
                    painter = painterResource(R.drawable.web_icon),
                    contentDescription = "browser",
                    modifier =
                            Modifier.size(20.dp)
                                    .constrainAs(browser) {
                                        top.linkTo(settings.bottom, margin = 20.dp)
                                        end.linkTo(parent.end, margin = 20.dp)
                                    }
                                    .clickable(
                                            onClick = {
                                                BrowserSession(context)
                                                browserViewModel.setDevWebView(
                                                        SessionList.sessions[
                                                                        SessionList
                                                                                .activeSessionIndex!!]
                                                                .getOrInitView()
                                                )
                                                navController.navigate(Screen.Browser.route)
                                            }
                                    )
            )

            Image(
                    painter = painterResource(R.drawable.zoom_in),
                    contentDescription = "window preview",
                    modifier =
                            Modifier.size(20.dp)
                                    .constrainAs(windowPreview) {
                                        top.linkTo(browser.bottom, margin = 20.dp)
                                        end.linkTo(parent.end, margin = 20.dp)
                                    }
                                    .clickable(
                                            onClick = {
                                                navController.navigate(Screen.ZoomOut.route)
                                            }
                                    )
            )

            Image(
                    painter =
                            painterResource(
                                    R.drawable.history_2_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            ),
                    contentDescription = "Recents",
                    modifier =
                            Modifier.size(20.dp)
                                    .constrainAs(recents) {
                                        top.linkTo(windowPreview.bottom, margin = 20.dp)
                                        end.linkTo(parent.end, margin = 20.dp)
                                    }
                                    .clickable(onClick = { isRecentsOpen = !isRecentsOpen })
            )

            // Extensions button
            Image(
                    painter = painterResource(R.drawable.extension),
                    contentDescription = "Extensions",
                    modifier =
                            Modifier.size(20.dp)
                                    .constrainAs(createRef()) {
                                        top.linkTo(recents.bottom, margin = 20.dp)
                                        end.linkTo(parent.end, margin = 20.dp)
                                    }
                                    .clickable(
                                            onClick = {
                                                navController.navigate(Screen.Extensions.route)
                                            }
                                    )
            )
            
            // Community/Telegram button
            Image(
                    painter = painterResource(R.drawable.groups),
                    contentDescription = "Join Community",
                    modifier =
                            Modifier.size(20.dp)
                                    .constrainAs(createRef()) {
                                        top.linkTo(recents.bottom, margin = 60.dp)
                                        end.linkTo(parent.end, margin = 20.dp)
                                    }
                                    .clickable(
                                            onClick = {
                                                val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+1mcwtY2aNc1jMjdl"))
                                                context.startActivity(telegramIntent)
                                            }
                                    )
            )
            
            // Alpha version text - bottom left
            Text(
                    text = "Alpha version",
                    color = Color(0xFF666666),
                    fontSize = 10.sp,
                    modifier = Modifier.constrainAs(createRef()) {
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                    }
            )
        }
    }
}

// MainMenu component is now in MainMenu.kt
