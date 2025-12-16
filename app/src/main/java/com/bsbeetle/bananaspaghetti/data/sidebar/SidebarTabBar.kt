package com.bsbeetle.bananaspaghetti.data.sidebar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun SidebarTabBar(
    selectedTab: SidebarTab,
    onTabSelected: (SidebarTab) -> Unit,
    enabledTabs: List<SidebarTab> = SidebarTab.values().toList(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF2D2D30)), // Plain grey like top bar
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Iterate over ALL tabs, but check if each is enabled
        SidebarTab.values().forEach { tab ->
            val isEnabled = enabledTabs.contains(tab)
            SidebarTabItem(
                tab = tab,
                isSelected = selectedTab == tab,
                isEnabled = isEnabled,
                onClick = {
                    if (isEnabled) {
                        onTabSelected(tab)
                    }
                }
            )
        }
    }
}

@Composable
private fun RowScope.SidebarTabItem(
    tab: SidebarTab,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(
                // No special background, just transparent
                if (!isEnabled) Color(0xFF1A1A1A) else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Simple white line indicator at bottom - only show for enabled selected tabs
        if (isSelected && isEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(2.dp)
                    .background(Color(0xFFCCCCCC)) // Simple white/grey line
                    .align(Alignment.BottomCenter)
            )
        }

        // Icon - no scale animation, simpler
        tab.imageRes?.let { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = tab.label,
                modifier = Modifier.size(22.dp),
                alpha = when {
                    !isEnabled -> 0.3f
                    isSelected -> 1f
                    else -> 0.6f
                }
            )
        }

        tab.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = tab.label,
                tint = when {
                    !isEnabled -> Color.Gray.copy(alpha = 0.3f)
                    isSelected -> Color.White
                    else -> Color(0xFF858585)
                },
                modifier = Modifier.size(22.dp)
            )
        }
    }
}