// Author : Harsh Gaonker
// Project started on : 18 - August - 2025

package com.bsbeetle.bananaspaghetti

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.ui.components.BottomBar
import com.bsbeetle.bananaspaghetti.ui.components.TopBar
import com.bsbeetle.bananaspaghetti.ui.home.HomeScreen
import com.bsbeetle.bananaspaghetti.ui.navigation.AppNavigation
import com.bsbeetle.bananaspaghetti.ui.theme.BananaSpaghettiTheme
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import android.net.Uri
import com.bsbeetle.bananaspaghetti.data.OpenedFiles


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Window Settings
        // Hides the status bar and blocks the IME
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode( // Disables soft keyboard popup
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )

        setContent {

            // Created the view model in the parent composable so its passed down throughout
            //...the children
            val keyboardViewModel: KeyboardViewModel = viewModel()

            BananaSpaghettiTheme {
                EnterImmersiveMode()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF181818))
                ) {
                    AppNavigation()
                    //TopBar(modifier = Modifier .height(15.dp))
                    //BottomBar(modifier = Modifier .height(10.dp) .align(Alignment.BottomCenter))
                }
            }
        }
    }
}































