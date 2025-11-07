package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.components.GridBackground
import com.bsbeetle.bananaspaghetti.ui.components.LiquidGlassBox
import com.bsbeetle.bananaspaghetti.ui.components.VignetteEffect
import com.bsbeetle.bananaspaghetti.ui.components.beetleAscii
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.ui.theme.KarmaticFont
import com.bsbeetle.bananaspaghetti.ui.theme.born2bsporty
import com.bsbeetle.bananaspaghetti.ui.theme.codaRegular
import com.bsbeetle.bananaspaghetti.utils.fileLauncher
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@SuppressLint("RestrictedApi")
@Composable
fun HomeScreen(navController: NavController) {

    var isRecentsOpen by remember { mutableStateOf(false) }

    // Animated offset for main menu
    val mainMenuOffset by animateDpAsState(
        targetValue = if (isRecentsOpen) -150.dp else 0.dp,
        animationSpec = tween(durationMillis = 400),
        label = "mainMenuOffset"
    )

    // Animated offset for recents panel
    val recentsPanelOffset by animateDpAsState(
        targetValue = if (isRecentsOpen) 0.dp else 400.dp,
        animationSpec = tween(durationMillis = 400),
        label = "recentsPanelOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {

        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (gridLayout, beetleAsciiLeft, beetleAsciiRight, vignette, mainMenu, settings, recents, recentsPanel) = createRefs()

            GridBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .constrainAs(gridLayout) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    })

            beetleAscii(modifier = Modifier
                .size(400.dp)
                .constrainAs(beetleAsciiLeft) {
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                }
                .alpha(0.6f))

            beetleAscii(modifier = Modifier
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

            // Main Menu with animated offset
            MainMenu(
                modifier = Modifier
                    .size(390.dp)
                    .offset(x = mainMenuOffset)
                    .constrainAs(mainMenu) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                navController
            )

            // Recents Panel with animated offset
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
                painter = painterResource(R.drawable.history_2_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = "Recents",
                modifier = Modifier
                    .size(20.dp)
                    .constrainAs(recents) {
                        top.linkTo(settings.bottom, margin = 20.dp)
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
    modifier: Modifier = Modifier, navController: NavController
) {

    val context = LocalContext.current
    val launcher = fileLauncher(context, navController)
    val buttonFontSize = 30.sp

    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column() {
            Row(
                modifier = Modifier
                    .width(400.dp)
                    .height(80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.beetle_logo),
                    contentDescription = "Beetle logo",
                    modifier = Modifier
                        .size(75.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "Beetle",
                    fontFamily = KarmaticFont,
                    color = Color.White,
                    fontSize = 50.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFFFFFFF),
                            offset = Offset(0f, 0f),
                            blurRadius = 15f
                        )
                    )
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "v1.0.0",
                        color = Color(0xFF4E4E4E),
                        fontSize = 20.sp,
                        fontFamily = born2bsporty,
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
                            .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(8.dp) }, effects = {
                                vibrancy()
                                blur(15f.dp.toPx())
                                lens(25f.dp.toPx(), 50f.dp.toPx())
                            }, onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.01f))
                            })
                    )

                    Box(
                        modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center
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
                            .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(8.dp) }, effects = {
                                vibrancy()
                                blur(15f.dp.toPx())
                                lens(25f.dp.toPx(), 50f.dp.toPx())
                            }, onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.01f))
                            })
                    )

                    Box(
                        modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center
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
                        .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(8.dp) }, effects = {
                            vibrancy()
                            blur(15f.dp.toPx())
                            lens(25f.dp.toPx(), 50f.dp.toPx())
                        }, onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.01f))
                        })
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
                            navController.navigate(Screen.Terminal.route)
                        })
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(8.dp) }, effects = {
                            vibrancy()
                            blur(15f.dp.toPx())
                            lens(25f.dp.toPx(), 50f.dp.toPx())
                        }, onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.01f))
                        })
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