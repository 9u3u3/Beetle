package com.bsbeetle.bananaspaghetti.ui.navigation

sealed class Screen(val route: String) {
    object Home: Screen("homeScreen")
    object NewFile: Screen("newFile")
    object OpenFile: Screen("openFile")
    object OpenFolder: Screen("openFolder")
    object Terminal: Screen(route = "terminal")
    object Settings: Screen("settings")
    object Demo: Screen("demo")
    object Browser: Screen("browser")
    object Notebook: Screen("notebook")

    // zoom out preview testing
    object ZoomOut: Screen("zoom_out")

    object SessionOverview : Screen("session_overview")
    object Extensions: Screen("extensions")
    object Auth: Screen("auth")
    object Licenses: Screen("licenses")
    object Notifications: Screen("notifications")
}