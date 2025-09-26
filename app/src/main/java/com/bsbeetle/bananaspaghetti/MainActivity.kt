// Authors : Harsh Gaonker & Johniton Mascarenhas
// Project started on : 18 - August - 2025

package com.bsbeetle.bananaspaghetti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.ui.DisableDefaultKeyboard
import com.bsbeetle.bananaspaghetti.ui.EnterImmersiveMode
import com.bsbeetle.bananaspaghetti.ui.navigation.AppNavigation
import com.bsbeetle.bananaspaghetti.ui.theme.BananaSpaghettiTheme
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            // V I E W  M O D E L S
            // Keyboard view model
            val keyboardViewModel: KeyboardViewModel = viewModel()
            // File view model
            val fileViewModel: FileViewModel = viewModel()
            // Slider view model
            val sliderViewModel: SliderViewModel = viewModel()

            BananaSpaghettiTheme {

                // System UI settings (Global settings)
                EnterImmersiveMode()
                DisableDefaultKeyboard()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0C0C))
                ) {
                    AppNavigation(
                        fileViewModel,
                        keyboardViewModel,
                        sliderViewModel
                    )
                }
            }

        }
    }
}































