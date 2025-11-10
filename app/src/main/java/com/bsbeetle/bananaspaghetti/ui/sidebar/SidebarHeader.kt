package com.bsbeetle.bananaspaghetti.ui.sidebar

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier

@Composable
fun SidebarHeader(selectedTab: SidebarTab) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2D2D30), Color(0xFF252526)))
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedContent(targetState = selectedTab) { tab ->
                tab.icon?.let {
                    Icon(imageVector = it, contentDescription = null, tint = Color(0xFF007ACC), modifier = Modifier.size(20.dp))
                }
                tab.imageRes?.let {
                    Image(painter = painterResource(id = it), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            AnimatedContent(targetState = selectedTab.label) { label ->
                Text(
                    text = label.uppercase(),
                    color = Color(0xFFE8E8E8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
