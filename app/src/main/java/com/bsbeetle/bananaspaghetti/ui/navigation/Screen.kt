package com.bsbeetle.bananaspaghetti.ui.navigation

sealed class Screen(val route: String) {
    object Home: Screen("homeScreen")
    object NewFile: Screen("newFile")
    object OpenFile: Screen("openFile")
    object OpenFolder: Screen("openFolder")
    object Terminal: Screen(route = "terminal")
    object Settings: Screen("settings")

    object Demo: Screen("demo")
}