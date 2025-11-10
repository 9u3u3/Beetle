package com.bsbeetle.bananaspaghetti.data

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.R

enum class SidebarTab(
    val icon: ImageVector? = null,
    val imageRes: Int? = null
) {
    FILES(imageRes = R.drawable.computer_folder_open_icon),
    SEARCH(imageRes = R.drawable.search_image),  // Your XML drawable
    // Your XML drawable
    GIT(icon = Icons.Default.AccountTree),
    EXTENSIONS(icon = Icons.Default.Extension),
    SETTINGS(icon = Icons.Default.Settings)
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isExpanded: Boolean = false,
    val level: Int = 0
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
    val sidebarWidth by animateDpAsState(
        targetValue = if (isVisible) 280.dp else 0.dp,
        label = "sidebarWidth"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Surface(
            modifier = modifier
                .width(sidebarWidth)
                .fillMaxHeight(),
            color = Color(0xFF1E1E1E),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                SidebarHeader(
                    onClose = onDismiss
                )

                // Tab Bar
                SidebarTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                HorizontalDivider(thickness = 1.dp, color = Color(0xFF2D2D2D))

                // Content Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                ) {
                    when (selectedTab) {
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

@Composable
private fun SidebarHeader(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF252526))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "EXPLORER",
            color = Color(0xFFCCCCCC),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Sidebar",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(18.dp)
            )
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
        SidebarTabItem(
            tab = SidebarTab.FILES,  // ✅ Changed
            isSelected = selectedTab == SidebarTab.FILES,
            onClick = { onTabSelected(SidebarTab.FILES) }
        )
        SidebarTabItem(
            tab = SidebarTab.SEARCH,  // ✅ Changed
            isSelected = selectedTab == SidebarTab.SEARCH,
            onClick = { onTabSelected(SidebarTab.SEARCH) }
        )
        SidebarTabItem(
            tab = SidebarTab.GIT,  // ✅ Changed
            isSelected = selectedTab == SidebarTab.GIT,
            onClick = { onTabSelected(SidebarTab.GIT) }
        )
        SidebarTabItem(
            tab = SidebarTab.EXTENSIONS,  // ✅ Changed
            isSelected = selectedTab == SidebarTab.EXTENSIONS,
            onClick = { onTabSelected(SidebarTab.EXTENSIONS) }
        )
        SidebarTabItem(
            tab = SidebarTab.SETTINGS,  // ✅ Changed
            isSelected = selectedTab == SidebarTab.SETTINGS,
            onClick = { onTabSelected(SidebarTab.SETTINGS) }
        )
    }
}

@Composable
private fun RowScope.SidebarTabItem(
    tab: SidebarTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .background(Color.Transparent),  // No background change
        contentAlignment = Alignment.Center
    ) {
        // Bottom blue line indicator - ONLY THIS
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF007ACC))
                    .align(Alignment.BottomCenter)
            )
        }

        // ✅ Show vector icon if available
        tab.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF007ACC) else Color(0xFF858585),
                modifier = Modifier.size(20.dp)
            )
        }

        // ✅ Show image if available
        tab.imageRes?.let { imageRes ->
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
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
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            placeholder = {
                Text("Search files...", color = Color(0xFF858585), fontSize = 13.sp)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF858585),
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007ACC),
                unfocusedBorderColor = Color(0xFF3C3C3C),
                focusedTextColor = Color(0xFFCCCCCC),
                unfocusedTextColor = Color(0xFFCCCCCC),
                cursorColor = Color(0xFF007ACC)
            ),
            singleLine = true,
            shape = RoundedCornerShape(4.dp)
        )

        // File Tree
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            // Sample file structure - replace with actual file system
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
            )

            items(sampleFiles) { file ->
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF2A2D2E) else Color.Transparent)
            .clickable {
                if (file.isDirectory) {
                    onToggleExpand()
                } else {
                    onFileClick()
                }
            }
            .padding(start = (file.level * 16).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (file.isDirectory) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF858585),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFDCB67A),
                modifier = Modifier.size(16.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
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
            color = Color(0xFFCCCCCC),
            fontSize = 13.sp,
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
            .padding(16.dp)
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var replaceQuery by remember { mutableStateOf("") }
        var caseSensitive by remember { mutableStateOf(false) }
        var wholeWord by remember { mutableStateOf(false) }
        var useRegex by remember { mutableStateOf(false) }

        Text(
            text = "Search",
            color = Color(0xFFCCCCCC),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search", fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007ACC),
                unfocusedBorderColor = Color(0xFF3C3C3C),
                focusedTextColor = Color(0xFFCCCCCC),
                unfocusedTextColor = Color(0xFFCCCCCC)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = replaceQuery,
            onValueChange = { replaceQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Replace", fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007ACC),
                unfocusedBorderColor = Color(0xFF3C3C3C),
                focusedTextColor = Color(0xFFCCCCCC),
                unfocusedTextColor = Color(0xFFCCCCCC)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SearchOption("Aa", caseSensitive) { caseSensitive = it }
            SearchOption("Ab", wholeWord) { wholeWord = it }
            SearchOption(".*", useRegex) { useRegex = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /* Implement search */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007ACC)
            )
        ) {
            Text("Search All Files")
        }
    }
}

@Composable
private fun SearchOption(
    label: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF007ACC) else Color(0xFF3C3C3C))
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
            .padding(16.dp)
    ) {
        Text(
            text = "Source Control",
            color = Color(0xFFCCCCCC),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Git status items
        GitStatusSection("Changes", 5)
        Spacer(modifier = Modifier.height(16.dp))
        GitStatusSection("Staged Changes", 2)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* Implement commit */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007ACC)
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Commit")
        }
    }
}

@Composable
private fun GitStatusSection(title: String, count: Int) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = Color(0xFF858585),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = count.toString(),
                color = Color(0xFF858585),
                fontSize = 11.sp
            )
        }
        // Add file items here
    }
}

@Composable
private fun ExtensionsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Extensions",
            color = Color(0xFFCCCCCC),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Recommended extensions for your workspace",
            color = Color(0xFF858585),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Add extension list here
    }
}

@Composable
private fun SettingsTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                color = Color(0xFFCCCCCC),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.ColorLens,
                title = "Theme",
                subtitle = "Dark (Visual Studio)"
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.TextFields,
                title = "Font Size",
                subtitle = "14px"
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Code,
                title = "Tab Size",
                subtitle = "4 spaces"
            )
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF007ACC),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF858585),
                fontSize = 12.sp
            )
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
        else -> Color(0xFF858585)
    }
}