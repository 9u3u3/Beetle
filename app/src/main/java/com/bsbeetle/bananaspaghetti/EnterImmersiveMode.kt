package com.bsbeetle.bananaspaghetti

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun EnterImmersiveMode() {
    val view = LocalView.current

    //This effect runs once when the composable enters the composition
    LaunchedEffect(Unit) {
        val window = (view.context as Activity).window
        val insetsController = WindowInsetsControllerCompat(window, view)
        // Hide the system bars (status and navigation)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        // This sets the behaviour or when they reappear
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}































