package com.bsbeetle.bananaspaghetti.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bsbeetle.bananaspaghetti.ui.Extension.ExtensionsScreen
import com.bsbeetle.bananaspaghetti.ui.home.BrowserScreen
import com.bsbeetle.bananaspaghetti.ui.home.HomeScreen
import com.bsbeetle.bananaspaghetti.ui.home.InitialEditorUI
import com.bsbeetle.bananaspaghetti.ui.home.InitialTerminalUI
import com.bsbeetle.bananaspaghetti.ui.home.NotebookUI
import com.bsbeetle.bananaspaghetti.ui.home.NotificationScreen
import com.bsbeetle.bananaspaghetti.ui.home.SettingsScreen
import com.bsbeetle.bananaspaghetti.ui.keyboard.DemoEditorUi
import com.bsbeetle.bananaspaghetti.ui.screens.sessions.ZoomOut

import com.bsbeetle.bananaspaghetti.ui.home.AuthScreen

@Composable
fun AppNavigation(startDestination: String = Screen.Home.route) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Auth.route){AuthScreen(navController)} // Auth Screen
        composable(Screen.Home.route){HomeScreen(navController)} // Home Screen (Default)
        composable(Screen.OpenFile.route){InitialEditorUI(navController)} // Open File
        composable(Screen.NewFile.route){InitialEditorUI(navController)} // New File
        composable(Screen.OpenFolder.route){InitialEditorUI(navController)} // Open or Create Folder
        composable(Screen.Terminal.route){InitialTerminalUI(navController)} // Open Terminal
        composable(Screen.Settings.route){SettingsScreen(navController)} // Open Settings
        composable(Screen.Demo.route){DemoEditorUi(navController)} // Demo editor used settings
        composable(Screen.Browser.route){BrowserScreen(navController)} // Developer browser UI
        composable(Screen.Notebook.route){NotebookUI(navController)} // Notebook/REPL UI
        composable(Screen.ZoomOut.route){ZoomOut(navController)} // Zoom out preview (Session switching)
        composable(Screen.Extensions.route){ExtensionsScreen(navController)}
        composable(Screen.Notifications.route){NotificationScreen(navController)} // Updates/Notifications
    }
}