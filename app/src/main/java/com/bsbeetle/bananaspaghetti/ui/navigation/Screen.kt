package com.bsbeetle.bananaspaghetti.ui.navigation

sealed class Screen(val route: String) {
    object Home: Screen("homeScreen")
    object NewFile: Screen("newFile")
    object OpenFile: Screen("openFile")

    object Settings: Screen("settings")

    object Demo: Screen("demo")
}