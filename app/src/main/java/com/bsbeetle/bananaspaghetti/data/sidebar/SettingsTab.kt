package com.bsbeetle.bananaspaghetti.data.sidebar


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            SettingItem(Icons.Default.ColorLens, "Theme", "Dark (Visual Studio)")
        }
        item {
            SettingItem(Icons.Default.TextFields, "Font Size", "14px")
        }
        item {
            SettingItem(Icons.Default.Code, "Tab Size", "4 spaces")
        }
    }
}

@Composable
private fun SettingItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { /* Handle */ }
            .background(Color(0xFF252526))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF007ACC),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = Color(0xFFE8E8E8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color(0xFF858585), fontSize = 12.sp)
        }
    }
}