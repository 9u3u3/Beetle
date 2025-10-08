package com.bsbeetle.bananaspaghetti.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bsbeetle.bananaspaghetti.ui.home.HomeScreen
import com.bsbeetle.bananaspaghetti.ui.home.InitialEditorUI
import com.bsbeetle.bananaspaghetti.ui.home.InitialTerminalUI
import com.bsbeetle.bananaspaghetti.ui.home.SettingsScreen
import com.bsbeetle.bananaspaghetti.ui.keyboard.DemoEditorUi
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.SliderViewModel

@Composable
fun AppNavigation(
    fileViewModel: FileViewModel,
    keyboardViewModel: KeyboardViewModel,
    sliderViewModel: SliderViewModel,
    bufferViewModel: BufferViewModel,
    editorViewModel: EditorViewModel
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
                    keyboardViewModel,
                    sliderViewModel,
                    navController,
                    bufferViewModel,
                    editorViewModel
                )
        }

        // Create a new file (Currently it has sample implementation of opening a file)
        composable(
            Screen.NewFile.route
        ) {
            InitialEditorUI(
                fileViewModel,
                keyboardViewModel,
                sliderViewModel,
                navController,
                bufferViewModel,
                editorViewModel
            )
        }

        // Open a folder (Also add a New Folder if this doesn't provide creation of new folder)
        composable(
            Screen.OpenFolder.route
        ) {
            InitialEditorUI(
                fileViewModel,
                keyboardViewModel,
                sliderViewModel,
                navController,
                bufferViewModel,
                editorViewModel
            )
        }

        // Open a terminal
        composable(
            Screen.Terminal.route
        ) {
            InitialTerminalUI(
                keyboardViewModel,
                sliderViewModel
            )
        }

        composable(route=Screen.Settings.route){
            SettingsScreen(
                keyboardViewModel,
                navController,
            )
        }

        composable(route=Screen.Demo.route){
            DemoEditorUi(
                keyboardViewModel,
                sliderViewModel,
            )
        }

    }
}

