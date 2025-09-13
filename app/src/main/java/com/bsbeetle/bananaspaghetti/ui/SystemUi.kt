package com.bsbeetle.bananaspaghetti.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun DisableDefaultKeyboard() {
    val context = LocalContext.current
    val window = (context as Activity).window

    LaunchedEffect(
        Unit
    ) {
        window.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            win.setSoftInputMode( // Disables soft keyboard popup
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )
        }
    }

}

@Composable
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