package com.bsbeetle.bananaspaghetti.ui.navigation

sealed class Screen(val route: String) {
    object Home: Screen("homeScreen")
    object OpenFile: Screen("openFile")
}