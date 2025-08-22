// Author : Harsh Gaonker
// Project started on : 18 - 08 - 2025

package com.bsbeetle.bananaspaghetti

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
import com.bsbeetle.bananaspaghetti.ui.home.HomeScreen
import com.bsbeetle.bananaspaghetti.ui.navigation.AppNavigation
import com.bsbeetle.bananaspaghetti.ui.theme.BananaSpaghettiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Forced to landscape mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Hide the status bar
        // This allows app to draw behind the status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            BananaSpaghettiTheme {
                EnterImmersiveMode()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF181818))
                ) {
                    AppNavigation()
                    TopBar(modifier = Modifier .height(15.dp))
                    BottomBar(modifier = Modifier .height(10.dp) .align(Alignment.BottomCenter))
                }
            }
        }
    }
}































