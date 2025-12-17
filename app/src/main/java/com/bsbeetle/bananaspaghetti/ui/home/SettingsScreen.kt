package com.bsbeetle.bananaspaghetti.ui.home

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

// Windows 11 color palette
private val BackgroundDark = Color(0xFF1A1A1A)
private val SidebarDark = Color(0xFF252525)
private val CardDark = Color(0xFF2D2D2D)
private val AccentBlue = Color(0xFF0078D4)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF9E9E9E)
private val DividerColor = Color(0xFF404040)

@Composable
fun SettingsScreen(navController: NavController) {
    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val authViewModel = com.bsbeetle.bananaspaghetti.LocalAuthViewModel.current
    val userEmail by authViewModel.userEmail.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Handle back navigation - in portrait mode, go back to list first if category is selected
    BackHandler {
        if (!isLandscape && selectedCategory != null) {
            selectedCategory = null
        } else {
            navController.popBackStack()
        }
    }
    
    if (isLandscape) {
        // LANDSCAPE LAYOUT - Sidebar + Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {
            SettingsSidebar(
                selectedCategory = selectedCategory ?: "Keyboard",
                onCategorySelected = { selectedCategory = it },
                userEmail = userEmail,
                onBackClick = { navController.popBackStack() }
            )
            
            SettingsContent(
                selectedCategory = selectedCategory ?: "Keyboard",
                keyboardViewModel = keyboardViewModel
            )
        }
    } else {
        // PORTRAIT LAYOUT - Full screen list or content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {
            if (selectedCategory == null) {
                // Category List View
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SidebarDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Profile Card
                    PortraitProfileCard(userEmail)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Settings categories
                    val categories = listOf("Keyboard", "Appearance")
                    categories.forEach { name ->
                        PortraitSettingsItem(
                            label = name,
                            onClick = { selectedCategory = name }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                // Content View
                // Header with back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SidebarDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedCategory = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedCategory!!,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                SettingsContent(
                    selectedCategory = selectedCategory!!,
                    keyboardViewModel = keyboardViewModel
                )
            }
        }
    }
}

@Composable
private fun PortraitProfileCard(userEmail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(AccentBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                tint = TextPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column {
            Text(
                text = if (userEmail.isNotBlank()) userEmail else "User",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Local Account",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PortraitSettingsItem(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 16.sp
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Go",
            tint = TextSecondary
        )
    }
}

@Composable
private fun SettingsSidebar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    userEmail: String,
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(SidebarDark)
            .padding(16.dp)
    ) {
        // Back button header for landscape
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Profile section
        ProfileCard(userEmail)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search bar
        SearchBar()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Navigation items
        val categories = listOf("Keyboard", "Appearance")
        
        categories.forEach { name ->
            SidebarItem(
                label = name,
                isSelected = selectedCategory == name,
                onClick = { onCategorySelected(name) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ProfileCard(userEmail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .clickable { /* Profile action */ }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AccentBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                tint = TextPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = if (userEmail.isNotBlank()) userEmail else "User",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Local Account",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SearchBar() {
    var searchText by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it },
        placeholder = {
            Text("Find a setting", color = TextSecondary, fontSize = 14.sp)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = CardDark,
            focusedContainerColor = CardDark,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = AccentBlue,
            cursorColor = AccentBlue
        ),
        singleLine = true
    )
}

@Composable
private fun SidebarItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) AccentBlue.copy(alpha = 0.15f) else Color.Transparent
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isSelected) AccentBlue else TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun SettingsContent(
    selectedCategory: String,
    keyboardViewModel: KeyboardViewModel
) {
    val uiState by keyboardViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(32.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = selectedCategory,
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        when (selectedCategory) {
            "Keyboard" -> KeyboardSettingsContent(
                isLeftVisible = uiState.isLeftKeyboardVisible,
                isRightVisible = uiState.isRightKeyboardVisible,
                leftWidth = uiState.leftKeyboardWidth,
                rightWidth = uiState.rightKeyboardWidth,
                isLinked = uiState.linkedKeyboardSliders,
                leftPadding = uiState.leftKeyboardPadding,
                leftHeight = uiState.leftKeyboardHeight,
                rightHeight = uiState.rightKeyboardHeight,
                isHeightLinked = uiState.linkedKeyboardHeight,
                onLeftToggle = { keyboardViewModel.toggleLeftKeyboardVisibility() },
                onRightToggle = { keyboardViewModel.toggleRightKeyboardVisibility() },
                onLeftWidthChange = { keyboardViewModel.updateLeftKeyboardWidth(it) },
                onRightWidthChange = { keyboardViewModel.updateRightKeyboardWidth(it) },
                onLinkToggle = { keyboardViewModel.toggleLinkedKeyboardSliders() },
                onLeftPaddingChange = { keyboardViewModel.updateLeftKeyboardPadding(it) },
                onLeftHeightChange = { keyboardViewModel.updateLeftKeyboardHeight(it) },
                onRightHeightChange = { keyboardViewModel.updateRightKeyboardHeight(it) },
                onHeightLinkToggle = { keyboardViewModel.toggleLinkedKeyboardHeight() }
            )
            "Appearance" -> AppearanceSettingsContent(
                terminalFontSize = uiState.terminalFontSize,
                editorFontSize = uiState.editorFontSize,
                onTerminalFontSizeChange = { keyboardViewModel.updateTerminalFontSize(it) },
                onEditorFontSizeChange = { keyboardViewModel.updateEditorFontSize(it) }
            )
        }
    }
}

@Composable
private fun AppearanceSettingsContent(
    terminalFontSize: Int,
    editorFontSize: Int,
    onTerminalFontSizeChange: (Int) -> Unit,
    onEditorFontSizeChange: (Int) -> Unit
) {
    SettingsCard(title = "Font Size") {
        // Terminal font size
        FontSizeSliderRow(
            title = "Terminal",
            description = "Font size for terminal (xterm.js)",
            value = terminalFontSize,
            onValueChange = onTerminalFontSizeChange
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )
        
        // Editor font size
        FontSizeSliderRow(
            title = "Editor",
            description = "Font size for code editor (CodeMirror)",
            value = editorFontSize,
            onValueChange = onEditorFontSizeChange
        )
    }
}

@Composable
private fun FontSizeSliderRow(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            Text(
                text = "${value}px",
                color = AccentBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 8f..24f,
            steps = 15,
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = DividerColor
            )
        )
    }
}

@Composable
private fun KeyboardSettingsContent(
    isLeftVisible: Boolean,
    isRightVisible: Boolean,
    leftWidth: Float,
    rightWidth: Float,
    isLinked: Boolean,
    leftPadding: Float = 0f,
    leftHeight: Float = 0.5f,
    rightHeight: Float = 0.5f,
    isHeightLinked: Boolean = true,
    onLeftToggle: () -> Unit,
    onRightToggle: () -> Unit,
    onLeftWidthChange: (Float) -> Unit,
    onRightWidthChange: (Float) -> Unit,
    onLinkToggle: () -> Unit,
    onLeftPaddingChange: (Float) -> Unit = {},
    onLeftHeightChange: (Float) -> Unit = {},
    onRightHeightChange: (Float) -> Unit = {},
    onHeightLinkToggle: () -> Unit = {}
) {
    // Visibility section
    SettingsCard(title = "Visibility") {
        SettingRowWithCheckbox(
            title = "Left Keyboard",
            description = "Show the left split keyboard",
            isChecked = isLeftVisible,
            onCheckedChange = { onLeftToggle() }
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )
        
        SettingRowWithCheckbox(
            title = "Right Keyboard",
            description = "Show the right split keyboard",
            isChecked = isRightVisible,
            onCheckedChange = { onRightToggle() }
        )
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Size section
    SettingsCard(title = "Keyboard Width") {
        // Link sliders toggle
        SettingRowWithCheckbox(
            title = "Link Sliders",
            description = "Adjust both keyboards together",
            isChecked = isLinked,
            onCheckedChange = { onLinkToggle() }
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )
        
        // Left keyboard slider
        SliderRow(
            title = "Left Keyboard",
            value = leftWidth,
            onValueChange = onLeftWidthChange,
            enabled = isLeftVisible
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )
        
        // Right keyboard slider
        SliderRow(
            title = "Right Keyboard",
            value = rightWidth,
            onValueChange = onRightWidthChange,
            enabled = isRightVisible
        )
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Left Padding section (for camera avoidance)
    SettingsCard(title = "Left Keyboard Padding") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Camera Offset",
                        color = if (isLeftVisible) TextPrimary else TextSecondary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Add padding to avoid camera cutout",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "${leftPadding.toInt()}dp",
                    color = AccentBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = leftPadding,
                onValueChange = onLeftPaddingChange,
                valueRange = 0f..50f,
                enabled = isLeftVisible,
                colors = SliderDefaults.colors(
                    thumbColor = AccentBlue,
                    activeTrackColor = AccentBlue,
                    inactiveTrackColor = DividerColor,
                    disabledThumbColor = TextSecondary,
                    disabledActiveTrackColor = TextSecondary,
                    disabledInactiveTrackColor = DividerColor
                )
            )
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Keyboard Height section
    SettingsCard(title = "Keyboard Height (Landscape)") {
        // Link height sliders toggle
        SettingRowWithCheckbox(
            title = "Link Height Sliders",
            description = "Adjust both keyboard heights together",
            isChecked = isHeightLinked,
            onCheckedChange = { onHeightLinkToggle() }
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )
        
        // Left keyboard height slider
        HeightSliderRow(
            title = "Left Keyboard",
            value = leftHeight,
            onValueChange = onLeftHeightChange,
            enabled = isLeftVisible
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        )
        
        // Right keyboard height slider
        HeightSliderRow(
            title = "Right Keyboard",
            value = rightHeight,
            onValueChange = onRightHeightChange,
            enabled = isRightVisible
        )
    }
}

@Composable
private fun HeightSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextSecondary,
                fontSize = 16.sp
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = AccentBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.3f..0.7f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = DividerColor,
                disabledThumbColor = TextSecondary,
                disabledActiveTrackColor = TextSecondary,
                disabledInactiveTrackColor = DividerColor
            )
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextSecondary,
                fontSize = 16.sp
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = AccentBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.0f..0.4f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = DividerColor,
                disabledThumbColor = TextSecondary,
                disabledActiveTrackColor = TextSecondary,
                disabledInactiveTrackColor = DividerColor
            )
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardDark)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRowWithCheckbox(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = AccentBlue,
                uncheckedColor = TextSecondary,
                checkmarkColor = TextPrimary
            )
        )
    }
}
