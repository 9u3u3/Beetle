package com.bsbeetle.bananaspaghetti.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bsbeetle.bananaspaghetti.ui.home.HomeScreen
import com.bsbeetle.bananaspaghetti.ui.home.InitialEditorUI
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import androidx.core.net.toUri
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

@Composable
fun AppNavigation(fileViewModel: FileViewModel) {
    val navController = rememberNavController()
    // Default page is home screen
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(modifier = Modifier.fillMaxSize(), navController = navController, fileViewModel)
        }

        composable(
            Screen.OpenFile.route,
        ) {
                InitialEditorUI(fileViewModel)
        }

        composable(Screen.NewFile.route) {
            // InitialEditorUI()
        }
    }
}