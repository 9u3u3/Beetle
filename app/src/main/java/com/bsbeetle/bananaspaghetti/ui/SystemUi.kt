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
fun DisableDefaultKeyboard() {
    val context = LocalContext.current
    val window = (context as Activity).window
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isLandscape) {  // React to orientation changes
        window.let { win ->
            if (isLandscape) {
                WindowCompat.setDecorFitsSystemWindows(win, false)
                win.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                )
            } else {
                // Enable keyboard in portrait
                win.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                )
            }
        }
    }
}@Composable
fun EnterImmersiveMode() {
    val view = LocalView.current

    //This effect runs once when the composable enters the composition
    LaunchedEffect(
        Unit
    ) {
        val window = (view.context as Activity).window
        val insetsController = WindowInsetsControllerCompat(window, view)
        // Hide the system bars (status and navigation)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        // This sets the behaviour or when they reappear
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}