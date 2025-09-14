package com.bsbeetle.bananaspaghetti.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bsbeetle.bananaspaghetti.ui.home.HomeScreen
import com.bsbeetle.bananaspaghetti.ui.home.InitialEditorUI
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@Composable
fun AppNavigation(
    fileViewModel: FileViewModel,
    keyboardViewModel: KeyboardViewModel
) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        // Home screen (Default page (obviously?))
        composable(
            Screen.Home.route
        ) {
            HomeScreen(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                fileViewModel
            )
        }

        // Open an existing file
        composable(
            Screen.OpenFile.route,
        ) {
                InitialEditorUI(
                    fileViewModel,
                    keyboardViewModel
                )
        }

        // Create a new file (Currently it has sample implementation of opening a file)
        composable(
            Screen.NewFile.route
        ) {
            InitialEditorUI(
                fileViewModel,
                keyboardViewModel
            )
        }

    }
}