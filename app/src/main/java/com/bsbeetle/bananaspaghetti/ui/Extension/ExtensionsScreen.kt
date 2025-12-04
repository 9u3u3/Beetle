package com.bsbeetle.bananaspaghetti.ui.Extension

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExtensionsScreen() {
    var selectedExtension by remember { mutableStateOf<ExtensionModel?>(null) }

    Row(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {

        // SIDEBAR
        LazyColumn(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(Color(0xFF252526))
                .padding(top = 12.dp)
        ) {
            items(ExtensionsRepo.extensions) { ext ->
                SidebarExtensionItem(
                    extension = ext,
                    isSelected = ext.id == selectedExtension?.id,
                    onClick = { selectedExtension = ext }
                )
            }
        }

        // DETAILS PANEL
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(Color(0xFF1E1E1E))
                .padding(20.dp)
        ) {
            if (selectedExtension == null) {
                Text(
                    text = "Select an extension to view details",
                    color = Color(0xFFBBBBBB),
                    fontSize = 18.sp,
                )
            } else {
                ExtensionDetails(extension = selectedExtension!!)
            }
        }
    }
}

@Composable
fun SidebarExtensionItem(
    extension: ExtensionModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF37373D) else Color.Transparent

    Column(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(extension.name, color = Color.White, fontSize = 13.sp)
        Text(extension.author, color = Color(0xFF9C9C9C), fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            extension.description.take(80) + "...",
            color = Color(0xFFB6B6B6),
            fontSize = 10.sp
        )
    }
}

@Composable
fun ExtensionDetails(extension: ExtensionModel) {
    Column(
        Modifier
            .fillMaxSize()
    ) {
        Text(extension.name, color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text("By ${extension.author}", color = Color(0xFFB6B6B6), fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        Text(extension.description, color = Color(0xFFDDDDDD), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        Text("Version: ${extension.version}", color = Color(0xFFB6B6B6), fontSize = 12.sp)
        Text("Size: ${extension.size}", color = Color(0xFFB6B6B6), fontSize = 12.sp)
        Text("Rating: ⭐ ${extension.rating}", color = Color(0xFFB6B6B6), fontSize = 12.sp)

        Spacer(Modifier.height(26.dp))

        if (!extension.installed) {
            Button(
                onClick = { /* TODO install */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F7BDE))
            ) {
                Text("Install")
            }
        } else {
            Button(
                onClick = { /* TODO uninstall */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404040))
            ) {
                Text("Uninstall")
            }
        }
    }
}