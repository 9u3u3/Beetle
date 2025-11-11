package com.bsbeetle.bananaspaghetti.data.sidebar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun Sidebar(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onFileSelected: (String?) -> Unit = {},
    currentPath: String? = null,
    enabledTabs: List<SidebarTab> = SidebarTab.values().toList(),
    modifier: Modifier = Modifier
) {
    // Initialize selectedTab with first enabled tab
    var selectedTab by remember(enabledTabs) {
        mutableStateOf(enabledTabs.firstOrNull() ?: SidebarTab.SETTINGS)
    }

    // Auto-switch to first available tab if current tab becomes unavailable
    LaunchedEffect(enabledTabs) {
        if (!enabledTabs.contains(selectedTab)) {
            selectedTab = enabledTabs.firstOrNull() ?: SidebarTab.SETTINGS
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop scrim - dismisses sidebar when tapped
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .zIndex(1f)
            )
        }

        // Sidebar content
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier.zIndex(2f)
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Prevent dismiss when clicking sidebar */ }
                    ),
                color = Color(0xFF1E1E1E),
                shadowElevation = 16.dp,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Enhanced Header with gradient
                    SidebarHeader(selectedTab = selectedTab)

                    // Sleek Tab Bar - show all tabs but disable some
                    SidebarTabBar(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            // Only allow selection of enabled tabs
                            if (enabledTabs.contains(tab)) {
                                selectedTab = tab
                            }
                        },
                        enabledTabs = enabledTabs
                    )

                    HorizontalDivider(thickness = 1.dp, color = Color(0xFF2D2D2D))

                    // Content Area with transition
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                    ) {
                        Crossfade(
                            targetState = selectedTab,
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            label = "tabContent"
                        ) { tab ->
                            when (tab) {
                                SidebarTab.FILES -> FilesTab(
                                    currentPath = currentPath,
                                    onFileSelected = onFileSelected
                                )
                                SidebarTab.SEARCH -> SearchTab()
                                SidebarTab.GIT -> GitTab()
                                SidebarTab.EXTENSIONS -> ExtensionsTab()
                                SidebarTab.SETTINGS -> SettingsTab()
                            }
                        }
                    }
                }
            }
        }
    }
}
