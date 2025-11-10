//package com.bsbeetle.bananaspaghetti.ui.sidebar
//
//import androidx.compose.animation.*
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Surface
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.zIndex
//
//@Composable
//fun Sidebar(
//    isVisible: Boolean,
//    onDismiss: () -> Unit,
//    onFileSelected: (String) -> Unit,
//    currentPath: String? = null,
//    modifier: Modifier = Modifier
//) {
//    var selectedTab by remember { mutableStateOf(SidebarTab.FILES) }
//
//    Box(modifier = modifier.fillMaxSize()) {
//        // Scrim background
//        AnimatedVisibility(
//            visible = isVisible,
//            enter = fadeIn(),
//            exit = fadeOut()
//        ) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Black.copy(alpha = 0.4f))
//                    .clickable(
//                        interactionSource = remember { MutableInteractionSource() },
//                        indication = null,
//                        onClick = onDismiss
//                    )
//                    .zIndex(1f)
//            )
//        }
//
//        // Sidebar content
//        AnimatedVisibility(
//            visible = isVisible,
//            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
//            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
//            modifier = Modifier.zIndex(2f)
//        ) {
//            Surface(
//                modifier = Modifier.width(300.dp).fillMaxHeight(),
//                color = Color(0xFF1E1E1E),
//                tonalElevation = 8.dp
//            ) {
//                Column {
//                    SidebarHeader(selectedTab)
//                    SidebarTabBar(selectedTab) { selectedTab = it }
//                    SidebarContent(selectedTab, currentPath, onFileSelected)
//                }
//            }
//        }
//    }
//}
