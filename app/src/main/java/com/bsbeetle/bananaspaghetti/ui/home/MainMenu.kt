package com.bsbeetle.bananaspaghetti.ui.home

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.LocalTerminalViewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.ui.theme.KarmaticFont
import com.bsbeetle.bananaspaghetti.ui.theme.born2bsporty
import com.bsbeetle.bananaspaghetti.utils.fileHandling.fileLauncher
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.TerminalSession
import com.bsbeetle.bananaspaghetti.viewmodel.TerminalViewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Main menu component for home screen.
 * Contains Beetle branding and primary action buttons.
 */
@Composable
fun MainMenu(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    val launcher = fileLauncher(context, navController)
    val buttonFontSize = 30.sp
    val terminalViewModel: TerminalViewModel = LocalTerminalViewModel.current
    val fileViewModel = LocalFileViewModel.current

    Box(modifier = modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        Column {
            // Header with logo and title
            Row(
                modifier = Modifier.width(400.dp).height(80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Image(
                    painter = painterResource(R.drawable.beetle_logo),
                    contentDescription = "Beetle logo",
                    modifier = Modifier.size(65.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Beetle",
                    fontFamily = KarmaticFont,
                    color = Color.White,
                    fontSize = 45.sp,
                    style = TextStyle(shadow = Shadow(color = Color(0xFFFFFFFF), offset = Offset(0f, 0f), blurRadius = 15f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "v1.0.0",
                        color = Color(0xFF666666),
                        fontSize = 18.sp,
                        fontFamily = born2bsporty,
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            val backgroundColor = Color(0xFFFFFFFF)

            val backdrop = rememberLayerBackdrop {
                drawRect(backgroundColor)
                drawContent()
            }

            // New file / Open file row
            Row(
                modifier = Modifier.width(400.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    text = "New file",
                    backdrop = backdrop,
                    fontSize = buttonFontSize,
                    modifier = Modifier.weight(1f).height(50.dp),
                    onClick = {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        launcher.launch(intent)
                    }
                )

                GlassButton(
                    text = "Open file",
                    backdrop = backdrop,
                    fontSize = buttonFontSize,
                    modifier = Modifier.weight(1f).height(50.dp),
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        launcher.launch(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Open folder button
            GlassButton(
                text = "Open folder",
                backdrop = backdrop,
                fontSize = buttonFontSize,
                modifier = Modifier.width(400.dp).height(50.dp),
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    launcher.launch(intent)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Terminal button
            GlassButton(
                text = "Terminal",
                backdrop = backdrop,
                fontSize = buttonFontSize,
                modifier = Modifier.width(400.dp).height(50.dp),
                onClick = {
                    val currentPath = fileViewModel.currentFilePath ?:
                        (SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession)?.projectRoot?.uri?.toString()

                    TerminalSession(context.applicationContext, initialDir = currentPath)
                    terminalViewModel.setTerminalWebView(SessionList.sessions[SessionList.activeSessionIndex!!].getOrInitView())
                    navController.navigate(Screen.Terminal.route)
                }
            )
        }
    }
}

/**
 * Glass-effect button component used in MainMenu.
 */
@Composable
private fun GlassButton(
    text: String,
    backdrop: com.kyant.backdrop.Backdrop,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
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
                text = text,
                fontSize = fontSize,
                fontFamily = born2bsporty,
                color = Color(0xFF989898),
                style = TextStyle(shadow = Shadow(color = Color(0xFF989898), offset = Offset(0f, 0f), blurRadius = 15f)),
                modifier = Modifier.padding(3.dp)
            )
        }
    }
}
