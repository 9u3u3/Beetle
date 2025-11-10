//package com.bsbeetle.bananaspaghetti.ui.components
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import com.bsbeetle.bananaspaghetti.R
//
//@Composable
//fun SidebarTabBar(
//    modifier: Modifier = Modifier,
//    onTabSelected: (String) -> Unit
//) {
//    var selectedTab by remember { mutableStateOf("Files") }
//
//    val tabs = listOf(
//        TabItem("Files", R.drawable.computer_folder_open_icon),
//        TabItem("Search", R.drawable.search_icon),
//        TabItem("Git", R.drawable.history_2_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
//        TabItem("Extensions", R.drawable.folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
//        TabItem("Settings", R.drawable.settings_24dp_e3e3e3_fill0_wght400_grad0_opsz24)
//    )
//
//    Surface(
//        color = MaterialTheme.colorScheme.surface,
//        tonalElevation = 3.dp,
//        modifier = modifier.fillMaxWidth()
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(MaterialTheme.colorScheme.surface)
//                .padding(horizontal = 8.dp, vertical = 6.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceEvenly
//        ) {
//            tabs.forEach { tab ->
//                val isSelected = selectedTab == tab.title
//                IconButton(
//                    onClick = {
//                        selectedTab = tab.title
//                        onTabSelected(tab.title)
//                    }
//                ) {
//                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        Icon(
//                            painter = painterResource(id = tab.iconRes),
//                            contentDescription = tab.title,
//                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
//                            modifier = Modifier.size(24.dp)
//                        )
//                        Text(
//                            text = tab.title,
//                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
//                            style = MaterialTheme.typography.labelSmall
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//data class TabItem(val title: String, val iconRes: Int)
