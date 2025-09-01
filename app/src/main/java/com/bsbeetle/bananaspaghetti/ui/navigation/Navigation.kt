package com.bsbeetle.bananaspaghetti.ui.navigation

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bsbeetle.bananaspaghetti.ui.home.HomeScreen
import com.bsbeetle.bananaspaghetti.ui.home.NewFileScreen
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Default page is home screen
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(modifier = Modifier.fillMaxSize(), navController = navController)
        }

        composable(Screen.OpenFile.route) {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color(0xFF181818)),
                contentAlignment = Alignment.Center) {
                Text(text = "Open File <Now focus on Main Core>",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 30.sp,
                    color = Color.White
                    )
            }
        }
        composable(Screen.NewFile.route) {
            val keyboardViewModel: KeyboardViewModel = viewModel()
            NewFileScreen(viewModel=KeyboardViewModel())
        }
    }
}