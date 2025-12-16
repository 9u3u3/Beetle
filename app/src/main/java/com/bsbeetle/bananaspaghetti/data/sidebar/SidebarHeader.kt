package com.bsbeetle.bananaspaghetti.data.sidebar


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SidebarHeader(selectedTab: SidebarTab) {
    // Simple grey header bar matching the top bar color
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color(0xFF2D2D30))
    )
}