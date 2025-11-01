// Authors : Harsh Gaonker (Founder and only owner) & Johniton Mascarenhas (Co-founder if the thing works out)
// Project started on : 18 - August - 2025

package com.bsbeetle.bananaspaghetti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.tenebrio.LSP.cLsp
import com.bsbeetle.bananaspaghetti.ui.DisableDefaultKeyboard
import com.bsbeetle.bananaspaghetti.ui.EnterImmersiveMode
import com.bsbeetle.bananaspaghetti.ui.navigation.AppNavigation
import com.bsbeetle.bananaspaghetti.ui.theme.BananaSpaghettiTheme
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

val LocalFileViewModel = compositionLocalOf<FileViewModel>{error("No file view model provided")}
val LocalKeyboardViewModel = compositionLocalOf<KeyboardViewModel>{error("No key board view model provided")}
val LocalSliderViewModel = compositionLocalOf<SliderViewModel>{error("No slider view model provided")}
val LocalBufferViewModel = compositionLocalOf<BufferViewModel>{error("No buffer view model provided")}
val LocalEditorViewModel = compositionLocalOf<EditorViewModel>{error("No editor view model provided")}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // testing
        lifecycleScope.launch(Dispatchers.IO) {
            val clsp = cLsp(applicationContext)
            clsp.init()
        }

        setContent {
            BananaSpaghettiTheme {

                // System UI settings (Global settings)
                EnterImmersiveMode()
                DisableDefaultKeyboard()

                val fileViewModel: FileViewModel = viewModel()
                val keyboardViewModel: KeyboardViewModel = viewModel()
                val sliderViewModel: SliderViewModel = viewModel()
                val bufferViewModel: BufferViewModel = viewModel()
                val editorViewModel: EditorViewModel = viewModel()


                CompositionLocalProvider(
                    LocalFileViewModel provides fileViewModel,
                    LocalKeyboardViewModel provides keyboardViewModel,
                    LocalSliderViewModel provides sliderViewModel,
                    LocalBufferViewModel provides bufferViewModel,
                    LocalEditorViewModel provides editorViewModel
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0C0C0C))
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}