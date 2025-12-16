package com.bsbeetle.bananaspaghetti.ui.home

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
    var selectedCategory by remember { mutableStateOf("Keyboard") }
    
    BackHandler {
        navController.popBackStack()
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Left sidebar
        SettingsSidebar(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            userEmail = userEmail
        )
        
        // Main content area
        SettingsContent(
            selectedCategory = selectedCategory,
            keyboardViewModel = keyboardViewModel
        )
    }
}

@Composable
private fun SettingsSidebar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    userEmail: String
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(SidebarDark)
            .padding(16.dp)
    ) {
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
                onLeftToggle = { keyboardViewModel.toggleLeftKeyboardVisibility() },
                onRightToggle = { keyboardViewModel.toggleRightKeyboardVisibility() },
                onLeftWidthChange = { keyboardViewModel.updateLeftKeyboardWidth(it) },
                onRightWidthChange = { keyboardViewModel.updateRightKeyboardWidth(it) },
                onLinkToggle = { keyboardViewModel.toggleLinkedKeyboardSliders() }
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
    onLeftToggle: () -> Unit,
    onRightToggle: () -> Unit,
    onLeftWidthChange: (Float) -> Unit,
    onRightWidthChange: (Float) -> Unit,
    onLinkToggle: () -> Unit
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
    SettingsCard(title = "Keyboard Size") {
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
