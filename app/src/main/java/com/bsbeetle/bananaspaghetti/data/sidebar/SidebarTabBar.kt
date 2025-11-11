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
    enabledTabs: List<SidebarTab> = SidebarTab.values().toList(), // Changed parameter name
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF252525)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Iterate over ALL tabs, but check if each is enabled
        SidebarTab.values().forEach { tab ->
            val isEnabled = enabledTabs.contains(tab)
            SidebarTabItem(
                tab = tab,
                isSelected = selectedTab == tab,
                isEnabled = isEnabled, // Pass enabled state
                onClick = {
                    if (isEnabled) { // Only allow click if enabled
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
    isEnabled: Boolean, // Add this parameter
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected && isEnabled) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(
                // Darker background for disabled tabs
                if (!isEnabled) Color(0xFF1A1A1A) else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled, // Disable click for disabled tabs
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated indicator line - only show for enabled selected tabs
        if (isSelected && isEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(2.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4379FF).copy(alpha = 0.1f),
                                Color(0xFF4379FF),
                                Color(0xFF4379FF).copy(alpha = 0.1f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )
        }

        // Icon with scale animation
        Box(modifier = Modifier.scale(scale)) {
            tab.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = tab.label,
                    tint = when {
                        !isEnabled -> Color.Gray.copy(alpha = 0.3f) // Grayed out for disabled
                        isSelected -> Color(0xFF007ACC)
                        else -> Color(0xFF858585)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            tab.imageRes?.let { imageRes ->
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = tab.label,
                    modifier = Modifier.size(22.dp),
                    alpha = if (!isEnabled) 0.3f else 1f // Reduced opacity for disabled
                )
            }
        }
    }
}