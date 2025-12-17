package com.bsbeetle.bananaspaghetti.data.sidebar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTab
import com.bsbeetle.bananaspaghetti.directGit.viewmodel.GitViewModel
import com.bsbeetle.bananaspaghetti.ui.components.MultiSelectActionsPopup

@Composable
fun Sidebar(
        isVisible: Boolean,
        onDismiss: () -> Unit,
        onFileSelected: (String?) -> Unit = {},
        currentPath: String? = null,
        enabledTabs: List<SidebarTab> = SidebarTab.values().toList(),
        modifier: Modifier = Modifier,
        onTabChanged: (SidebarTab) -> Unit = {},
        gitViewModel: GitViewModel? = null
) {
        // Get FileViewModel for multi-select state
        val fileViewModel = LocalFileViewModel.current
        
        // Track if user has confirmed their selection (to show popup)
        var showActionsPopup by remember { mutableStateOf(false) }
        
        // Reset popup state when multi-select mode exits
        LaunchedEffect(fileViewModel.isMultiSelectMode) {
                if (!fileViewModel.isMultiSelectMode) {
                        showActionsPopup = false
                }
        }
        
        // Initialize selectedTab with first enabled tab
        var selectedTab by
                remember(enabledTabs) {
                        mutableStateOf(enabledTabs.firstOrNull() ?: SidebarTab.FILES)
                }

        // Auto-switch to first available tab if current tab becomes unavailable
        LaunchedEffect(enabledTabs) {
                if (!enabledTabs.contains(selectedTab)) {
                        val newTab = enabledTabs.firstOrNull() ?: SidebarTab.FILES
                        selectedTab = newTab
                        onTabChanged(newTab)
                }
        }

        // Animated offset for peek effect
        val offsetX by
                animateFloatAsState(
                        targetValue = if (isVisible) 0f else -1f,
                        animationSpec =
                                spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                ),
                        label = "sidebarOffset"
                )

        Box(modifier = modifier.fillMaxSize()) {
                // Backdrop scrim - dismisses sidebar when tapped
                AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200))
                ) {
                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f))
                                                .clickable(
                                                        interactionSource =
                                                                remember {
                                                                        MutableInteractionSource()
                                                                },
                                                        indication = null,
                                                        onClick = onDismiss
                                                )
                                                .zIndex(1f)
                        )
                }

                // Sidebar content with sliding peek effect
                Surface(
                        modifier =
                                Modifier.width(300.dp)
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                                // Slide from left to right with peek
                                                translationX = offsetX * size.width
                                        }
                                        .zIndex(2f)
                                        .clickable(
                                                interactionSource =
                                                        remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { /* Prevent dismiss when clicking sidebar */
                                                }
                                        ),
                        color = Color(0xFF1E1E1E),
                        shadowElevation = 16.dp,
                        tonalElevation = 8.dp
                ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                        // Enhanced Header with gradient
                                        SidebarHeader(selectedTab = selectedTab)

                                        // Sleek Tab Bar - show all tabs but disable some
                                        SidebarTabBar(
                                                selectedTab = selectedTab,
                                                onTabSelected = { tab ->
                                                        // Only allow selection of enabled tabs
                                                        if (enabledTabs.contains(tab)) {
                                                                selectedTab = tab
                                                                onTabChanged(tab)
                                                        }
                                                },
                                                enabledTabs = enabledTabs
                                        )

                                        HorizontalDivider(thickness = 1.dp, color = Color(0xFF2D2D2D))

                                        // Content Area with transition
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxSize().background(Color(0xFF121212)) // Black like landscape boxes
                                        ) {
                                                Crossfade(
                                                        targetState = selectedTab,
                                                        animationSpec =
                                                                tween(300, easing = FastOutSlowInEasing),
                                                        label = "tabContent"
                                                ) { tab ->
                                                        when (tab) {
                                                                SidebarTab.FILES ->
                                                                        FilesTab(
                                                                                currentPath = currentPath,
                                                                                onFileSelected =
                                                                                        onFileSelected
                                                                        )
                                                                SidebarTab.GIT -> {
                                                                        if (gitViewModel != null) {
                                                                                GitTab(
                                                                                        viewModel =
                                                                                                gitViewModel
                                                                                )
                                                                        } else {
                                                                                GitTab()
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                                
                                // Floating action bar when in multi-select mode
                                AnimatedVisibility(
                                        visible = fileViewModel.isMultiSelectMode && selectedTab == SidebarTab.FILES,
                                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                        Row(
                                                modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF2D2D2D))
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                // Cancel button
                                                IconButton(
                                                        onClick = { fileViewModel.exitMultiSelectMode() },
                                                        modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF454545))
                                                ) {
                                                        Icon(
                                                                Icons.Rounded.Close,
                                                                contentDescription = "Cancel",
                                                                tint = Color(0xFFCCCCCC),
                                                                modifier = Modifier.size(20.dp)
                                                        )
                                                }
                                                
                                                // Selected count
                                                Text(
                                                        text = "${fileViewModel.selectedFilePaths.size} selected",
                                                        color = Color.White,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                )
                                                
                                                // Confirm button
                                                IconButton(
                                                        onClick = { showActionsPopup = true },
                                                        modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White)
                                                ) {
                                                        Icon(
                                                                Icons.Rounded.Check,
                                                                contentDescription = "Confirm selection",
                                                                tint = Color(0xFF1E1E1E),
                                                                modifier = Modifier.size(22.dp)
                                                        )
                                                }
                                        }
                                }
                        }
                }
                
                // Multi-select popup for portrait mode - only show after user confirms selection
                Box(modifier = Modifier.zIndex(3f)) {
                        MultiSelectActionsPopup(
                                show = showActionsPopup && isVisible,
                                fileViewModel = fileViewModel,
                                selectedFilePaths = fileViewModel.selectedFilePaths,
                                onDismiss = { showActionsPopup = false }
                        )
                }
        }
}


