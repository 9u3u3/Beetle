// Authors : Harsh Gaonker & Johniton Mascarenhas
// Project started on : 18 - August - 2025

package com.bsbeetle.bananaspaghetti

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.bsbeetle.bananaspaghetti.ui.DisableDefaultKeyboard
import com.bsbeetle.bananaspaghetti.ui.EnterImmersiveMode
import com.bsbeetle.bananaspaghetti.ui.navigation.AppNavigation
import com.bsbeetle.bananaspaghetti.ui.theme.BeetleTheme
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.DevWebViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.ExpandViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.TerminalViewModel

val LocalFileViewModel = compositionLocalOf<FileViewModel> { error("No file view model provided") }
val LocalKeyboardViewModel =
        compositionLocalOf<KeyboardViewModel> { error("No key board view model provided") }
val LocalSliderViewModel =
        compositionLocalOf<SliderViewModel> { error("No slider view model provided") }
val LocalBufferViewModel =
        compositionLocalOf<BufferViewModel> { error("No buffer view model provided") }
val LocalEditorViewModel =
        compositionLocalOf<EditorViewModel> { error("No editor view model provided") }
val LocalExpandViewModel =
        compositionLocalOf<ExpandViewModel> { error("No expand view model provided") }
val LocalTerminalViewModel =
        compositionLocalOf<TerminalViewModel> { error("No terminal view model provided") }
val LocalBrowserViewModel =
        compositionLocalOf<DevWebViewModel> { error("No browser view model provided") }
val LocalAuthViewModel =
        compositionLocalOf<com.bsbeetle.bananaspaghetti.viewmodel.AuthViewModel> { error("No auth view model provided") }

class MainActivity : ComponentActivity() {
    
        // Notification permission launcher for Android 13+
        private val notificationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
        ) { isGranted ->
                // Permission result - service will work regardless, but notification may not show
                android.util.Log.d("MainActivity", "Notification permission granted: $isGranted")
        }
        
        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()
                
                // Request notification permission for Android 13+ (TIRAMISU)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                }

                setContent {
                        BeetleTheme(dynamicColor = false) {

                                // System UI settings (Global settings)
                                EnterImmersiveMode()

                                val navController = rememberNavController()

                                // View Models used throughout the application
                                val fileViewModel: FileViewModel = viewModel()
                                val keyboardViewModel: KeyboardViewModel = viewModel()
                                val sliderViewModel: SliderViewModel = viewModel()
                                val bufferViewModel: BufferViewModel = viewModel()
                                val editorViewModel: EditorViewModel = viewModel()
                                val expandViewModel: ExpandViewModel = viewModel()
                                val terminalViewModel: TerminalViewModel = viewModel()
                                val browserViewModel: DevWebViewModel = viewModel()
                                val authViewModel: com.bsbeetle.bananaspaghetti.viewmodel.AuthViewModel = viewModel()
                                
                                // Collect keyboard UI state for IME mode
                                val keyboardUiState = keyboardViewModel.uiState.collectAsState()
                                
                                // Sync CustomWebView's static IME flag with the setting
                                // And notify all active WebViews to update their inputmode
                                androidx.compose.runtime.LaunchedEffect(keyboardUiState.value.useLandscapeIme) {
                                    val useLandscapeIme = keyboardUiState.value.useLandscapeIme
                                    
                                    // Update static flag
                                    com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView.useLandscapeIme = useLandscapeIme
                                    
                                    // Update all active session WebViews to reflect the new setting
                                    com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList.sessions.forEach { session ->
                                        session.view?.evaluateJavascript("""
                                            window.useLandscapeIme = $useLandscapeIme;
                                            if (window.updateInputMode) window.updateInputMode();
                                        """.trimIndent(), null)
                                    }
                                }
                                
                                DisableDefaultKeyboard(useLandscapeIme = keyboardUiState.value.useLandscapeIme)

                                val startDestination = if (authViewModel.isAuthorized.value) {
                                    com.bsbeetle.bananaspaghetti.ui.navigation.Screen.Home.route
                                } else {
                                    com.bsbeetle.bananaspaghetti.ui.navigation.Screen.Auth.route
                                }

                                CompositionLocalProvider(
                                        LocalFileViewModel provides fileViewModel,
                                        LocalKeyboardViewModel provides keyboardViewModel,
                                        LocalSliderViewModel provides sliderViewModel,
                                        LocalBufferViewModel provides bufferViewModel,
                                        LocalEditorViewModel provides editorViewModel,
                                        LocalExpandViewModel provides expandViewModel,
                                        LocalTerminalViewModel provides terminalViewModel,
                                        LocalBrowserViewModel provides browserViewModel,
                                        LocalAuthViewModel provides authViewModel
                                ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                                AppNavigation(startDestination = startDestination)
                                                //
                                                // BrowserScreen(navController)
                                        }
                                }
                        }
                }
        }
}

