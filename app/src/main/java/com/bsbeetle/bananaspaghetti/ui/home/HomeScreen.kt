package com.bsbeetle.bananaspaghetti.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController) {
    Box(
        modifier = modifier.background(color = Color(0xFF181818)),
        contentAlignment = Alignment.Center
    ) {
        // Centered content
        Box(
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Column( ) {
                // Heading
                Text(
                    text = "Banana Spaghetti",
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Menu Items
                MenuItem(
                    iconRes = R.drawable.note_add_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                    label = "New File",
                    onClick = { navController.navigate(Screen.NewFile.route)}
                )

                MenuItem(
                    iconRes = R.drawable.file_open_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                    label = "Open File",
                    onClick = { navController.navigate(Screen.OpenFile.route)}
                )

                MenuItem(
                    iconRes = R.drawable.folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                    label = "Open Folder"
                )

                MenuItem(
                    iconRes = R.drawable.terminal_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                    label = "Open Terminal"
                )
            }
        }
    }
}

@Composable
fun MenuItem(iconRes: Int, label: String, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 6.dp)
            .padding(start = 16.dp)
            .clickable{ onClick() }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(15.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}