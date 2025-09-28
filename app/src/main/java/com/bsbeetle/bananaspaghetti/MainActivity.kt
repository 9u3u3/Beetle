// Authors : Harsh Gaonker & Johniton Mascarenhas
// Project started on : 18 - August - 2025

package com.bsbeetle.bananaspaghetti

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.tenebrio.data.ShellExecutor
import com.bsbeetle.bananaspaghetti.tenebrio.pty.Tenebrio
import com.bsbeetle.bananaspaghetti.tenebrio.terminal.Terminal
import com.bsbeetle.bananaspaghetti.ui.DisableDefaultKeyboard
import com.bsbeetle.bananaspaghetti.ui.EnterImmersiveMode
import com.bsbeetle.bananaspaghetti.ui.navigation.AppNavigation
import com.bsbeetle.bananaspaghetti.ui.theme.BananaSpaghettiTheme
import com.bsbeetle.bananaspaghetti.utils.EditorWebView
import com.bsbeetle.bananaspaghetti.utils.TerminalWebView
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel
import java.io.File
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val keyboardViewModel: KeyboardViewModel = viewModel()
            val fileViewModel: FileViewModel = viewModel()
            val sliderViewModel: SliderViewModel = viewModel()
            val webView: WebView = EditorWebView.getWebview(this)
            val termWebView: WebView = TerminalWebView.getWebview(this)
            val bufferViewModel: BufferViewModel = viewModel()
            val dotDownloadedFile = File(filesDir.absolutePath, ".downloaded")
            val terminal = Terminal(this)
            ShellExecutor.startShell(this)

            BananaSpaghettiTheme {

                // System UI settings (Global settings)
                EnterImmersiveMode()
                DisableDefaultKeyboard()

                // testing the terminal
//                thread {
//                    if(dotDownloadedFile.exists()) {
//                        terminal.startAxs()
//                    } else {
//                        terminal.installRootfs() // Install root fs and extract it also download the some extra dependencies
//                    }
//                }

                val pty = Tenebrio()
                pty.testShell()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0C0C))
                ) {
                    AppNavigation(
                        fileViewModel,
                        keyboardViewModel,
                        sliderViewModel,
                        bufferViewModel
                    )
                }
            }

        }
    }

    override fun onDestroy() {
        EditorWebView.destroy()
        super.onDestroy()
    }

}































