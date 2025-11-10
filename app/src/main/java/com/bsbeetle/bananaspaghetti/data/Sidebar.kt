package com.bsbeetle.bananaspaghetti.data

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bsbeetle.bananaspaghetti.R

enum class SidebarTab(
    val icon: ImageVector? = null,
    val imageRes: Int? = null,
    val label: String
) {
    FILES(imageRes = R.drawable.computer_folder_open_icon, label = "Files"),
    SEARCH(imageRes = R.drawable.search_image, label = "Search"),
    GIT(icon = Icons.Default.AccountTree, label = "Source Control"),
    EXTENSIONS(icon = Icons.Default.Extension, label = "Extensions"),
    SETTINGS(icon = Icons.Default.Settings, label = "Settings")
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isExpanded: Boolean = false,
    val level: Int = 0,
    val children: List<FileItem> = emptyList()
)

@Composable
fun Sidebar(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onFileSelected: (String) -> Unit,
    currentPath: String? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SidebarTab.FILES) }

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

                    // Sleek Tab Bar
                    SidebarTabBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
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

@Composable
private fun SidebarHeader(selectedTab: SidebarTab) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D2D30),
                        Color(0xFF252526)
                    )
                )
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated icon based on selected tab
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "headerIcon"
            ) { tab ->
                tab.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF007ACC),
                        modifier = Modifier.size(20.dp)
                    )
                }
                tab.imageRes?.let { imageRes ->
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab.label,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                },
                label = "headerText"
            ) { label ->
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

@Composable
private fun SidebarTabBar(
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
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = imageRes),
                    contentDescription = tab.label,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun FilesTab(
    currentPath: String?,
    onFileSelected: (String) -> Unit
) {
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Modern search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = {
                Text("Search files...", color = Color(0xFF6A6A6A), fontSize = 13.sp)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF858585),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF858585),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007ACC),
                unfocusedBorderColor = Color(0xFF3C3C3C),
                focusedTextColor = Color(0xFFE8E8E8),
                unfocusedTextColor = Color(0xFFCCCCCC),
                cursorColor = Color(0xFF007ACC),
                focusedContainerColor = Color(0xFF2D2D2D),
                unfocusedContainerColor = Color(0xFF252526)
            ),
            singleLine = true,
            shape = RoundedCornerShape(6.dp)
        )

        // File tree with smooth animations
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val sampleFiles = listOf(
                FileItem("src", "/src", true, level = 0),
                FileItem("middleware", "/src/middleware", true, level = 1),
                FileItem("authRoutes", "/src/authRoutes", true, level = 1),
                FileItem("auth.js", "/src/authRoutes/auth.js", false, level = 2),
                FileItem("delete.js", "/src/authRoutes/delete.js", false, level = 2),
                FileItem("Server.js", "/src/Server.js", false, level = 1),
                FileItem(".env", "/.env", false, level = 0),
                FileItem("package.json", "/package.json", false, level = 0),
                FileItem("README.md", "/README.md", false, level = 0)
            ).filter {
                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
            }

            items(sampleFiles, key = { it.path }) { file ->
                FileTreeItem(
                    file = file,
                    isExpanded = expandedFolders.contains(file.path),
                    isSelected = currentPath == file.path,
                    onToggleExpand = {
                        expandedFolders = if (expandedFolders.contains(file.path)) {
                            expandedFolders - file.path
                        } else {
                            expandedFolders + file.path
                        }
                    },
                    onFileClick = { onFileSelected(file.path) }
                )
            }
        }
    }
}

@Composable
private fun FileTreeItem(
    file: FileItem,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggleExpand: () -> Unit,
    onFileClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF2A2D2E)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chevronRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable {
                if (file.isDirectory) {
                    onToggleExpand()
                } else {
                    onFileClick()
                }
            }
            .padding(start = (8 + file.level * 16).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (file.isDirectory) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF858585),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = rotationAngle }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFDCB67A),
                modifier = Modifier.size(18.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(22.dp))
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = getFileIconColor(file.name),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = file.name,
            color = if (isSelected) Color(0xFFE8E8E8) else Color(0xFFCCCCCC),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var replaceQuery by remember { mutableStateOf("") }
        var caseSensitive by remember { mutableStateOf(false) }
        var wholeWord by remember { mutableStateOf(false) }
        var useRegex by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search", fontSize = 13.sp, color = Color(0xFF6A6A6A)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007ACC),
                unfocusedBorderColor = Color(0xFF3C3C3C),
                focusedTextColor = Color(0xFFE8E8E8),
                unfocusedTextColor = Color(0xFFCCCCCC),
                focusedContainerColor = Color(0xFF2D2D2D),
                unfocusedContainerColor = Color(0xFF252526)
            ),
            singleLine = true,
            shape = RoundedCornerShape(6.dp)
        )

        OutlinedTextField(
            value = replaceQuery,
            onValueChange = { replaceQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Replace", fontSize = 13.sp, color = Color(0xFF6A6A6A)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007ACC),
                unfocusedBorderColor = Color(0xFF3C3C3C),
                focusedTextColor = Color(0xFFE8E8E8),
                unfocusedTextColor = Color(0xFFCCCCCC),
                focusedContainerColor = Color(0xFF2D2D2D),
                unfocusedContainerColor = Color(0xFF252526)
            ),
            singleLine = true,
            shape = RoundedCornerShape(6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchOption("Aa", "Match Case", caseSensitive) { caseSensitive = it }
            SearchOption("Ab", "Whole Word", wholeWord) { wholeWord = it }
            SearchOption(".*", "Use Regex", useRegex) { useRegex = it }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { /* Search */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007ACC),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Search All Files", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RowScope.SearchOption(
    label: String,
    tooltip: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF007ACC) else Color(0xFF3C3C3C),
        animationSpec = tween(200),
        label = "optionColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "optionScale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable { onToggle(!isSelected) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GitTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GitStatusSection("Changes", 5)
        GitStatusSection("Staged Changes", 2)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* Commit */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007ACC)
            ),
            shape = RoundedCornerShape(6.dp),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Commit", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun GitStatusSection(title: String, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF252526))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = Color(0xFFE8E8E8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF007ACC).copy(alpha = 0.2f)
            ) {
                Text(
                    text = count.toString(),
                    color = Color(0xFF007ACC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ExtensionsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Recommended extensions for your workspace",
            color = Color(0xFF858585),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SettingsTab() {
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

private fun getFileIconColor(fileName: String): Color {
    return when {
        fileName.endsWith(".js") -> Color(0xFFF7DF1E)
        fileName.endsWith(".kt") -> Color(0xFF7F52FF)
        fileName.endsWith(".json") -> Color(0xFFFCA311)
        fileName.endsWith(".md") -> Color(0xFF42A5F5)
        fileName.endsWith(".env") -> Color(0xFFEDD652)
        fileName.endsWith(".css") -> Color(0xFF2965F1)
        fileName.endsWith(".html") -> Color(0xFFE34C26)
        fileName.endsWith(".py") -> Color(0xFF3776AB)
        else -> Color(0xFF858585)
    }
}