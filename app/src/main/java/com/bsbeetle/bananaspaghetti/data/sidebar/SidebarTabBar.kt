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
    onTabSelected: (SidebarTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF252526)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SidebarTab.entries.forEach { tab ->
            SidebarTabItem(
                tab = tab,
                isSelected = selectedTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun RowScope.SidebarTabItem(
    tab: SidebarTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated indicator line - OUTSIDE the Box with scale
        if (isSelected) {
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

// Icon with scale animation (keep this below)

        // Icon with scale animation
        Box(modifier = Modifier.scale(scale)) {
            tab.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = tab.label,
                    tint = if (isSelected) Color(0xFF007ACC) else Color(0xFF858585),
                    modifier = Modifier.size(22.dp)
                )
            }

            tab.imageRes?.let { imageRes ->
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = tab.label,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}