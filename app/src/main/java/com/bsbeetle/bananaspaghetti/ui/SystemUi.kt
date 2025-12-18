package com.bsbeetle.bananaspaghetti.ui

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun DisableDefaultKeyboard(useLandscapeIme: Boolean = false) {
    val context = LocalContext.current
    val window = (context as Activity).window
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isLandscape, useLandscapeIme) {
        window.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)

            if (isLandscape && !useLandscapeIme) {
                // Disable IME keyboard in landscape (use custom split keyboard)
                win.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                )
            } else {
                // Enable IME keyboard (in portrait, or in landscape when useLandscapeIme is true)
                win.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                )
            }
        }
    }
}

@Composable
fun EnterImmersiveMode() {
    val view = LocalView.current

    LaunchedEffect(Unit) {
        val window = (view.context as Activity).window
        val insetsController = WindowInsetsControllerCompat(window, view)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}