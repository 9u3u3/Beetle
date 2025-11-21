package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.bsbeetle.bananaspaghetti.LocalTerminalViewModel
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
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType
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
    var isRecentsOpen by remember { mutableStateOf(false) }
    val mainMenuOffset by animateDpAsState(
        targetValue = if (isRecentsOpen) -150.dp else 0.dp,
        animationSpec = tween(durationMillis = 400),
        label = "mainMenuOffset"
    ) // Animated offset or main menu
    val targetOffset = if (isRecentsOpen) 0.dp else 300.dp
    val recentsPanelOffset by animateDpAsState(
        targetValue = if (isLandscape) targetOffset else targetOffset,
        label = "recentsPanelOffset"
    ) // Animated offset for recent panel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {

        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (gridLayout, beetleAsciiLeft, beetleAsciiRight, vignette, mainMenu, settings, recents, recentsPanel, browser, windowPreview) = createRefs()

            GridBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .constrainAs(gridLayout) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    })

            beetleAscii(
                modifier = Modifier
                    .size(400.dp)
                    .constrainAs(beetleAsciiLeft) {
                        start.linkTo(parent.start)
                        bottom.linkTo(parent.bottom)
                    }
                    .alpha(0.6f))

            beetleAscii(
                modifier = Modifier
                    .size(400.dp)
                    .constrainAs(beetleAsciiRight) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                    }
                    .rotate(180f)
                    .alpha(0.6f))

            VignetteEffect(
                modifier = Modifier
                    .fillMaxSize()
                    .constrainAs(vignette) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }, intensity = 1f
            )

            MainMenu(
                modifier = Modifier
                    .size(390.dp)
                    .then(
                        if (isLandscape)
                            Modifier.offset(x = mainMenuOffset, y = 0.dp)
                        else
                            Modifier.offset(x = 0.dp, y = mainMenuOffset)
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
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .offset(x = recentsPanelOffset)
                        .constrainAs(recentsPanel) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            end.linkTo(parent.end)
                        }
                ) {
                    LiquidGlassBox(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Recent",
                                fontFamily = codaRegular,
                                color = Color(0xFFFFFFFF),
                                fontSize = 35.sp
                            )
                        }
                    }
                }
            }
            else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .offset(y = recentsPanelOffset)
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
                        }
                    }
                }

            }
            Image(
                painter = painterResource(R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = "Settings",
                modifier = Modifier
                    .size(20.dp)
                    .constrainAs(settings) {
                        top.linkTo(parent.top, margin = 20.dp)
                        end.linkTo(parent.end, margin = 20.dp)
                    }
                    .clickable(
                        onClick = {
                            navController.navigate(Screen.Settings.route)
                        }))

            Image(
                painter = painterResource(R.drawable.web_icon),
                contentDescription = "browser",
                modifier = Modifier
                    .size(20.dp)
                    .constrainAs(browser) {
                        top.linkTo(settings.bottom, margin = 20.dp)
                        end.linkTo(parent.end, margin = 20.dp)
                    }
                    .clickable(
                        onClick = {
                            navController.navigate(Screen.Browser.route)
                        }))

            Image(
                painter = painterResource(R.drawable.zoom_in),
                contentDescription = "window preview",
                modifier = Modifier
                    .size(20.dp)
                    .constrainAs(windowPreview) {
                        top.linkTo(browser.bottom, margin = 20.dp)
                        end.linkTo(parent.end, margin = 20.dp)
                    }
                    .clickable(
                        onClick = {
                            // Coming soon
                        }))

            Image(
                painter = painterResource(R.drawable.history_2_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = "Recents",
                modifier = Modifier
                    .size(20.dp)
                    .constrainAs(recents) {
                        top.linkTo(windowPreview.bottom, margin = 20.dp)
                        end.linkTo(parent.end, margin = 20.dp)
                    }
                    .clickable(
                        onClick = {
                            isRecentsOpen = !isRecentsOpen
                        }))
        }
    }
}

@Composable
fun MainMenu(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = LocalContext.current
    val launcher = fileLauncher(context, navController)
    val buttonFontSize = 30.sp
    val terminalViewModel: TerminalViewModel = LocalTerminalViewModel.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column {

            Row(
                modifier = Modifier
                    .width(400.dp)
                    .height(80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Image(
                    painter = painterResource(R.drawable.beetle_logo),
                    contentDescription = "Beetle logo",
                    modifier = Modifier
                        .size(65.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "BEETLE",
                    fontFamily = KarmaticFont,
                    color = Color.White,
                    fontSize = 45.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFFFFFFF),
                            offset = Offset(0f, 0f),
                            blurRadius = 15f
                        )
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                   modifier = Modifier
                       .fillMaxSize()
                ) {
                    Text(
                        text = "v1.0.0",
                        color = Color(0xFF666666),
                        fontSize = 18.sp,
                        fontFamily = born2bsporty,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            val backgroundColor = Color(0xFFFFFFFF)

            val backdrop = rememberLayerBackdrop {
                drawRect(backgroundColor)
                drawContent()
            }

            Row(
                modifier = Modifier.width(400.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable(
                            onClick = {
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                }
                                launcher.launch(intent)
                            })
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(8.dp) },
                                effects = {
                                    vibrancy()
                                    blur(15f.dp.toPx())
                                    lens(25f.dp.toPx(), 50f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.01f))
                                }
                            )
                    )

                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "New file",
                            fontSize = buttonFontSize,
                            fontFamily = born2bsporty,
                            color = Color(0xFF989898),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFF989898),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 15f
                                )
                            ),
                            modifier = Modifier.padding(3.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable(
                            onClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                }
                                launcher.launch(intent)
                            })
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(8.dp) },
                                effects = {
                                    vibrancy()
                                    blur(15f.dp.toPx())
                                    lens(25f.dp.toPx(), 50f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.01f))
                                }
                            )
                    )

                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Open file",
                            fontSize = buttonFontSize,
                            fontFamily = born2bsporty,
                            color = Color(0xFF989898),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFF989898),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 15f
                                )
                            ),
                            modifier = Modifier.padding(3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .width(400.dp)
                    .height(50.dp)
                    .clickable(
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            launcher.launch(intent)
                        })
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(8.dp) },
                            effects = {
                                vibrancy()
                                blur(15f.dp.toPx())
                                lens(25f.dp.toPx(), 50f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.01f))
                            }
                        )
                )

                Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Open folder",
                        fontSize = buttonFontSize,
                        fontFamily = born2bsporty,
                        color = Color(0xFF989898),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF989898),
                                offset = Offset(0f, 0f),
                                blurRadius = 15f
                            )
                        ),
                        modifier = Modifier.padding(3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .width(400.dp)
                    .height(50.dp)
                    .clickable(
                        onClick = {
                            // Terminal Session handler (Will make a function for this later im to lazy to do that currently and this is also in the testing)
                            Session(context.applicationContext, SessionType.TERMINAL)
                            terminalViewModel.setTerminalWebView(SessionList.sessions[SessionList.activeSessionIndex!!].terminalView!!)
                            navController.navigate(Screen.Terminal.route)
                        })
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(8.dp) },
                            effects = {
                                vibrancy()
                                blur(15f.dp.toPx())
                                lens(25f.dp.toPx(), 50f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.01f))
                            }
                        )
                )

                Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Terminal",
                        fontSize = buttonFontSize,
                        fontFamily = born2bsporty,
                        color = Color(0xFF989898),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF989898),
                                offset = Offset(0f, 0f),
                                blurRadius = 15f
                            )
                        ),
                        modifier = Modifier.padding(3.dp)
                    )
                }
            }
        }
    }
}