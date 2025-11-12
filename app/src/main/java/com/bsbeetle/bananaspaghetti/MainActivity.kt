// Authors : Harsh Gaonker & Johniton Mascarenhas
// Project started on : 18 - August - 2025

package com.bsbeetle.bananaspaghetti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.ui.DisableDefaultKeyboard
import com.bsbeetle.bananaspaghetti.ui.EnterImmersiveMode
import com.bsbeetle.bananaspaghetti.ui.navigation.AppNavigation
import com.bsbeetle.bananaspaghetti.ui.theme.BananaSpaghettiTheme
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.ExpandViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

val LocalFileViewModel = compositionLocalOf<FileViewModel>{error("No file view model provided")}
val LocalKeyboardViewModel = compositionLocalOf<KeyboardViewModel>{error("No key board view model provided")}
val LocalSliderViewModel = compositionLocalOf<SliderViewModel>{error("No slider view model provided")}
val LocalBufferViewModel = compositionLocalOf<BufferViewModel>{error("No buffer view model provided")}
val LocalEditorViewModel = compositionLocalOf<EditorViewModel>{error("No editor view model provided")}
val LocalExpandViewModel = compositionLocalOf<ExpandViewModel>{error("No expand view model provided")}



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BananaSpaghettiTheme(dynamicColor = false) {

                // System UI settings (Global settings)
                EnterImmersiveMode()
                DisableDefaultKeyboard()

                // View Models used throughout the application
                val fileViewModel: FileViewModel = viewModel()
                val keyboardViewModel: KeyboardViewModel = viewModel()
                val sliderViewModel: SliderViewModel = viewModel()
                val bufferViewModel: BufferViewModel = viewModel()
                val editorViewModel: EditorViewModel = viewModel()
                val expandViewModel: ExpandViewModel = viewModel()

                CompositionLocalProvider(
                    LocalFileViewModel provides fileViewModel,
                    LocalKeyboardViewModel provides keyboardViewModel,
                    LocalSliderViewModel provides sliderViewModel,
                    LocalBufferViewModel provides bufferViewModel,
                    LocalEditorViewModel provides editorViewModel,
                    LocalExpandViewModel provides expandViewModel,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        AppNavigation()
                    }
                }

            }
        }
    }
}