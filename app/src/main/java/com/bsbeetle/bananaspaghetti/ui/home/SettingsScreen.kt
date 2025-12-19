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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
                Row(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
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
                Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
                        if (selectedCategory == null) {
                                // Category List View
                                // Header
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .background(SidebarDark)
                                                        .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 12.dp
                                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        IconButton(
                                                onClick = { navController.popBackStack() },
                                                modifier = Modifier.size(32.dp)
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Filled.ArrowBack,
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
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .verticalScroll(rememberScrollState())
                                                        .padding(16.dp)
                                ) {
                                        // Profile Card
                                        PortraitProfileCard(userEmail)

                                        Spacer(Modifier.height(24.dp))

                                        // Settings categories
                                        val categories =
                                                listOf(
                                                        "Keyboard",
                                                        "Appearance",
                                                        "Editor",
                                                        "Git",
                                                        "General"
                                                )
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
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .background(SidebarDark)
                                                        .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 12.dp
                                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        IconButton(
                                                onClick = { selectedCategory = null },
                                                modifier = Modifier.size(32.dp)
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Filled.ArrowBack,
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
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardDark)
                                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(AccentBlue),
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
                        Text(text = "Local Account", color = TextSecondary, fontSize = 14.sp)
                }
        }
}

@Composable
private fun PortraitSettingsItem(label: String, onClick: () -> Unit) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardDark)
                                .clickable { onClick() }
                                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(text = label, color = TextPrimary, fontSize = 16.sp)
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
                modifier =
                        Modifier.width(260.dp)
                                .fillMaxHeight()
                                .background(SidebarDark)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
        ) {
                // Back button header for landscape
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        IconButton(onClick = onBackClick, modifier = Modifier.size(32.dp)) {
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

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation items
                val categories = listOf("Keyboard", "Appearance", "Editor", "Git", "General")

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
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardDark)
                                .clickable { /* Profile action */}
                                .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(AccentBlue),
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
                        Text(text = "Local Account", color = TextSecondary, fontSize = 12.sp)
                }
        }
}

@Composable
private fun SidebarItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
        val backgroundColor = if (isSelected) AccentBlue.copy(alpha = 0.15f) else Color.Transparent

        Row(
                modifier =
                        Modifier.fillMaxWidth()
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
                useLandscapeIme = uiState.useLandscapeIme,
                onLeftToggle = { keyboardViewModel.toggleLeftKeyboardVisibility() },
                onRightToggle = { keyboardViewModel.toggleRightKeyboardVisibility() },
                onLeftWidthChange = { keyboardViewModel.updateLeftKeyboardWidth(it) },
                onRightWidthChange = { keyboardViewModel.updateRightKeyboardWidth(it) },
                onLinkToggle = { keyboardViewModel.toggleLinkedKeyboardSliders() },
                onLeftPaddingChange = { keyboardViewModel.updateLeftKeyboardPadding(it) },
                onLeftHeightChange = { keyboardViewModel.updateLeftKeyboardHeight(it) },
                onRightHeightChange = { keyboardViewModel.updateRightKeyboardHeight(it) },
                onHeightLinkToggle = { keyboardViewModel.toggleLinkedKeyboardHeight() },
                onUseLandscapeImeToggle = { keyboardViewModel.toggleUseLandscapeIme() }
            )
            "Appearance" -> AppearanceSettingsContent(
                terminalFontSize = uiState.terminalFontSize,
                editorFontSize = uiState.editorFontSize,
                onTerminalFontSizeChange = { keyboardViewModel.updateTerminalFontSize(it) },
                onEditorFontSizeChange = { keyboardViewModel.updateEditorFontSize(it) }
            )
            "Licenses" -> LicensesContent()
            "Editor" -> EditorSettingsContent()
            "Git" -> GitSettingsContent()
            "General" -> GeneralSettingsContent()
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

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, color = TextPrimary, fontSize = 16.sp)
                                Text(text = description, color = TextSecondary, fontSize = 13.sp)
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
                        colors =
                                SliderDefaults.colors(
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
        useLandscapeIme: Boolean = false,
    onLeftToggle: () -> Unit,
    onRightToggle: () -> Unit,
    onLeftWidthChange: (Float) -> Unit,
    onRightWidthChange: (Float) -> Unit,
    onLinkToggle: () -> Unit,
    onLeftPaddingChange: (Float) -> Unit = {},
    onLeftHeightChange: (Float) -> Unit = {},
    onRightHeightChange: (Float) -> Unit = {},
    onHeightLinkToggle: () -> Unit = {},
    onUseLandscapeImeToggle: () -> Unit = {}
) {
        // Landscape IME Mode section (at the top)
    SettingsCard(title = "Input Mode (Landscape)") {
        SettingRowWithCheckbox(
            title = "Use System Keyboard",
            description = "Use the system IME keyboard instead of custom split keyboard in landscape mode",
            isChecked = useLandscapeIme,
            onCheckedChange = { onUseLandscapeImeToggle() }
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Visibility section
        SettingsCard(title = "Visibility") {
                SettingRowWithCheckbox(
                        title = "Left Keyboard",
                        description = "Show the left split keyboard",
                        isChecked = isLeftVisible,
                        onCheckedChange = { onLeftToggle() }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

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

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                // Left keyboard slider
                SliderRow(
                        title = "Left Keyboard",
                        value = leftWidth,
                        onValueChange = onLeftWidthChange,
                        enabled = isLeftVisible
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

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
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = "Camera Offset",
                                                color =
                                                        if (isLeftVisible) TextPrimary
                                                        else TextSecondary,
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
                                colors =
                                        SliderDefaults.colors(
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

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                // Left keyboard height slider
                HeightSliderRow(
                        title = "Left Keyboard",
                        value = leftHeight,
                        onValueChange = onLeftHeightChange,
                        enabled = isLeftVisible
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                        colors =
                                SliderDefaults.colors(
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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                        colors =
                                SliderDefaults.colors(
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
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
        Column {
                Text(
                        text = title,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CardDark)
                ) { content() }
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
                modifier =
                        Modifier.fillMaxWidth()
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

                        Text(text = description, color = TextSecondary, fontSize = 13.sp)
                }

                Checkbox(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange,
                        colors =
                                CheckboxDefaults.colors(
                                        checkedColor = AccentBlue,
                                        uncheckedColor = TextSecondary,
                                        checkmarkColor = TextPrimary
                                )
                )
        }
}

@Composable
private fun LicensesContent() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Open Source Licenses",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // PRoot - GPL License
        LicenseSection(
            libraryName = "PRoot",
            license = "GPLv2",
            description = "PRoot is used to provide Linux environment emulation.",
            sourceCodeUrl = "https://github.com/proot-me/proot",
            licenseText = """
                GNU GENERAL PUBLIC LICENSE
                Version 2, June 1991
                
                Copyright (C) 1989, 1991 Free Software Foundation, Inc.
                51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
                
                Everyone is permitted to copy and distribute verbatim copies of this license document, but changing it is not allowed.
                
                This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
                
                This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
                
                You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
                
                Full license: https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
            """.trimIndent()
        )

        Spacer(Modifier.height(24.dp))

        // CodeMirror - MIT License
        LicenseSection(
            libraryName = "CodeMirror",
            license = "MIT License",
            description = "CodeMirror is used for the code editor component.",
            sourceCodeUrl = "https://github.com/codemirror/codemirror",
            licenseText = """
                MIT License
                
                Copyright (C) 2017 by Marijn Haverbeke <marijn@haverbeke.berlin> and others
                
                Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
                
                The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
                
                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
            """.trimIndent()
        )

        Spacer(Modifier.height(24.dp))

        // xterm.js - MIT License
        LicenseSection(
            libraryName = "xterm.js",
            license = "MIT License",
            description = "xterm.js is used for the terminal emulator.",
            sourceCodeUrl = "https://github.com/xtermjs/xterm.js",
            licenseText = """
                MIT License
                
                Copyright (c) 2017-present, The xterm.js authors
                
                Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
                
                The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
                
                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
            """.trimIndent()
        )

        Spacer(Modifier.height(24.dp))

        // Additional Libraries Summary
        Text(
            text = "Additional Libraries",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AdditionalLibraryItem("JGit", "EDL (Eclipse Distribution License)")
        AdditionalLibraryItem("Ktor", "Apache License 2.0")
        AdditionalLibraryItem("NanoHTTPD", "BSD License")
        AdditionalLibraryItem("Android Libraries", "Apache License 2.0")
    }
}

@Composable
private fun LicenseSection(
    libraryName: String,
    license: String,
    description: String,
    sourceCodeUrl: String,
    licenseText: String
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = libraryName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = license,
                    color = AccentBlue,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = description,
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Source Code:",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = sourceCodeUrl,
            color = AccentBlue,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Expand/Collapse License Text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp)
        ) {
            Text(
                text = if (isExpanded) "Hide License Text" else "Show License Text",
                color = AccentBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (isExpanded) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = licenseText,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun AdditionalLibraryItem(name: String, license: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            color = TextPrimary,
            fontSize = 14.sp
        )
        Text(
            text = license,
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EditorSettingsContent() {
        val context = androidx.compose.ui.platform.LocalContext.current
        val editorViewModel = com.bsbeetle.bananaspaghetti.LocalEditorViewModel.current
        val prefs = remember {
                com.bsbeetle.bananaspaghetti.utils.preferences.EditorPreferences(context)
        }

        var tabSize by remember { mutableStateOf(prefs.tabSize) }
        var indentWithTabs by remember { mutableStateOf(prefs.indentWithTabs) }
        var wordWrap by remember { mutableStateOf(prefs.wordWrap) }
        var showLineNumbers by remember { mutableStateOf(prefs.lineNumbers) }
        var relativeLineNumbers by remember { mutableStateOf(prefs.relativeLineNumbers) }
        var showIndentGuides by remember { mutableStateOf(prefs.indentGuides) }
        var autoSave by remember { mutableStateOf(prefs.autoSave) }
        var autoSaveDelay by remember { mutableStateOf(prefs.autoSaveDelay) }
        var selectedTheme by remember { mutableStateOf(prefs.theme) }
        var themeDropdownExpanded by remember { mutableStateOf(false) }
        var pythonReplEnabled by remember { mutableStateOf(prefs.pythonRepl) }

        // Theme Selection Card
        SettingsCard(title = "Editor Theme") {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Color Theme", color = TextPrimary, fontSize = 16.sp)
                        Text(
                                "Choose the editor color scheme",
                                color = TextSecondary,
                                fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Theme Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF3C3C3C))
                                                        .clickable { themeDropdownExpanded = true }
                                                        .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 14.dp
                                                        ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                prefs.getThemeDisplayName(selectedTheme),
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                        )
                                        Icon(
                                                imageVector =
                                                        Icons.AutoMirrored.Filled
                                                                .KeyboardArrowRight,
                                                contentDescription = "Expand",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                        )
                                }

                                DropdownMenu(
                                        expanded = themeDropdownExpanded,
                                        onDismissRequest = { themeDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF2D2D2D))
                                ) {
                                        prefs.availableThemes.forEach { (themeId, themeName) ->
                                                DropdownMenuItem(
                                                        text = {
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Text(
                                                                                themeName,
                                                                                color = TextPrimary
                                                                        )
                                                                        if (selectedTheme == themeId
                                                                        ) {
                                                                                Text(
                                                                                        "✓",
                                                                                        color =
                                                                                                AccentBlue,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Bold
                                                                                )
                                                                        }
                                                                }
                                                        },
                                                        onClick = {
                                                                selectedTheme = themeId
                                                                prefs.theme = themeId
                                                                themeDropdownExpanded = false
                                                                // Apply immediately
                                                                editorViewModel.webView
                                                                        ?.evaluateJavascript(
                                                                                "if(window.setTheme) window.setTheme('$themeId');",
                                                                                null
                                                                        )
                                                        }
                                                )
                                        }
                                }
                        }
                }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCard(title = "Indentation") {
                // Tab Size
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text("Tab Size", color = TextPrimary, fontSize = 16.sp)
                                        Text(
                                                "Number of spaces per indentation level",
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                        )
                                }
                                Text(
                                        "$tabSize",
                                        color = AccentBlue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                                value = tabSize.toFloat(),
                                onValueChange = {
                                        tabSize = it.toInt()
                                        prefs.tabSize = tabSize
                                        editorViewModel.webView?.evaluateJavascript(
                                                "if(window.setTabSize) window.setTabSize($tabSize);",
                                                null
                                        )
                                },
                                valueRange = 2f..8f,
                                steps = 5,
                                colors =
                                        SliderDefaults.colors(
                                                thumbColor = AccentBlue,
                                                activeTrackColor = AccentBlue,
                                                inactiveTrackColor = DividerColor
                                        )
                        )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                SettingRowWithCheckbox(
                        title = "Indent with Tabs",
                        description = "Use tabs instead of spaces for indentation",
                        isChecked = indentWithTabs,
                        onCheckedChange = {
                                indentWithTabs = it
                                prefs.indentWithTabs = it
                        }
                )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCard(title = "Display") {
                SettingRowWithCheckbox(
                        title = "Word Wrap",
                        description = "Wrap long lines to fit the viewport",
                        isChecked = wordWrap,
                        onCheckedChange = {
                                wordWrap = it
                                prefs.wordWrap = it
                                editorViewModel.webView?.evaluateJavascript(
                                        "if(window.setWordWrap) window.setWordWrap($it);",
                                        null
                                )
                        }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                SettingRowWithCheckbox(
                        title = "Line Numbers",
                        description = "Show line numbers in the gutter",
                        isChecked = showLineNumbers,
                        onCheckedChange = {
                                showLineNumbers = it
                                prefs.lineNumbers = it
                                editorViewModel.webView?.evaluateJavascript(
                                        "if(window.setLineNumbers) window.setLineNumbers($it);",
                                        null
                                )
                        }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                SettingRowWithCheckbox(
                        title = "Relative Line Numbers",
                        description = "Show line numbers relative to cursor position",
                        isChecked = relativeLineNumbers,
                        onCheckedChange = {
                                relativeLineNumbers = it
                                prefs.relativeLineNumbers = it
                                editorViewModel.webView?.evaluateJavascript(
                                        "if(window.setRelativeLineNumbers) window.setRelativeLineNumbers($it);",
                                        null
                                )
                        }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                SettingRowWithCheckbox(
                        title = "Indent Guides",
                        description = "Show vertical lines at each indent level",
                        isChecked = showIndentGuides,
                        onCheckedChange = {
                                showIndentGuides = it
                                prefs.indentGuides = it
                                editorViewModel.webView?.evaluateJavascript(
                                        "if(window.setIndentGuides) window.setIndentGuides($it);",
                                        null
                                )
                        }
                )

        }


        SettingsCard(title = "Auto Save") {
                SettingRowWithCheckbox(
                        title = "Enable Auto Save",
                        description = "Automatically save files after changes",
                        isChecked = autoSave,
                        onCheckedChange = {
                                autoSave = it
                                prefs.autoSave = it
                        }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                "Auto Save Delay",
                                                color =
                                                        if (autoSave) TextPrimary
                                                        else TextSecondary,
                                                fontSize = 16.sp
                                        )
                                        Text(
                                                "Seconds to wait after last keystroke",
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                        )
                                }
                                Text(
                                        "${autoSaveDelay}s",
                                        color = AccentBlue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                                value = autoSaveDelay.toFloat(),
                                onValueChange = {
                                        autoSaveDelay = it.toInt()
                                        prefs.autoSaveDelay = autoSaveDelay
                                },
                                valueRange = 1f..30f,
                                steps = 28,
                                enabled = autoSave,
                                colors =
                                        SliderDefaults.colors(
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

        // Python REPL Settings Card
        SettingsCard(title = "Python Notebook") {
                SettingRowWithCheckbox(
                        title = "Python REPL Mode",
                        description = "Enable notebook-like editing for .py files with cell execution (requires Python installed)",
                        isChecked = pythonReplEnabled,
                        onCheckedChange = {
                                pythonReplEnabled = it
                                prefs.pythonRepl = it
                        }
                )
        }

        Spacer(modifier = Modifier.height(16.dp))
}
@Composable
private fun GitSettingsContent() {
        val context = androidx.compose.ui.platform.LocalContext.current
        var userName by remember { mutableStateOf("") }
        var userEmail by remember { mutableStateOf("") }
        var showPublicKey by remember { mutableStateOf(false) }
        var publicKey by remember { mutableStateOf("") }

        // Load public key
        androidx.compose.runtime.LaunchedEffect(showPublicKey) {
                if (showPublicKey) {
                        try {
                                val alpinePath =
                                        "${context.filesDir.absolutePath}/alpine/home/.ssh/id_ed25519.pub"
                                val file = java.io.File(alpinePath)
                                publicKey =
                                        if (file.exists()) file.readText() else "No SSH key found"
                        } catch (e: Exception) {
                                publicKey = "Error reading key: ${e.message}"
                        }
                }
        }

        SettingsCard(title = "Git User Configuration") {
                // User Name
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("User Name", color = TextPrimary, fontSize = 16.sp)
                        Text("Used for commit author name", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                                value = userName,
                                onValueChange = { userName = it },
                                placeholder = { Text("Your Name", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = CardDark,
                                                focusedContainerColor = CardDark,
                                                unfocusedBorderColor = DividerColor,
                                                focusedBorderColor = AccentBlue,
                                                cursorColor = AccentBlue,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                        ),
                                singleLine = true
                        )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                // User Email
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("User Email", color = TextPrimary, fontSize = 16.sp)
                        Text(
                                "Used for commit author email",
                                color = TextSecondary,
                                fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                                value = userEmail,
                                onValueChange = { userEmail = it },
                                placeholder = { Text("you@example.com", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = CardDark,
                                                focusedContainerColor = CardDark,
                                                unfocusedBorderColor = DividerColor,
                                                focusedBorderColor = AccentBlue,
                                                cursorColor = AccentBlue,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                        ),
                                singleLine = true
                        )
                }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCard(title = "SSH Key") {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                "View Public Key",
                                                color = TextPrimary,
                                                fontSize = 16.sp
                                        )
                                        Text(
                                                "Copy to add to GitHub/GitLab",
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                        )
                                }
                                Box(
                                        modifier =
                                                Modifier.clip(RoundedCornerShape(8.dp))
                                                        .background(AccentBlue)
                                                        .clickable {
                                                                showPublicKey = !showPublicKey
                                                        }
                                                        .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 8.dp
                                                        )
                                ) {
                                        Text(
                                                if (showPublicKey) "Hide" else "Show",
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                        )
                                }
                        }

                        if (showPublicKey) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                        publicKey,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .background(
                                                                Color(0xFF1E1E1E),
                                                                RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(8.dp)
                                )
                        }
                }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
                "Note: Git user config will be applied when saved in a future update.",
                color = TextSecondary,
                fontSize = 12.sp
        )
}

@Composable
private fun GeneralSettingsContent() {
        val context = androidx.compose.ui.platform.LocalContext.current
        var showClearCacheConfirm by remember { mutableStateOf(false) }

        SettingsCard(title = "About") {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("App Name", color = TextPrimary, fontSize = 16.sp)
                                Text("BananaSpaghetti", color = TextSecondary, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("Version", color = TextPrimary, fontSize = 16.sp)
                                Text("1.0.0", color = TextSecondary, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Text("Package", color = TextPrimary, fontSize = 16.sp)
                                Text(
                                        "com.bsbeetle.bananaspaghetti",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                )
                        }
                }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCard(title = "Storage") {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text("Clear Cache", color = TextPrimary, fontSize = 16.sp)
                                        Text(
                                                "Remove temporary files and cached data",
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                        )
                                }
                                Box(
                                        modifier =
                                                Modifier.clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFDC3545))
                                                        .clickable { showClearCacheConfirm = true }
                                                        .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 8.dp
                                                        )
                                ) { Text("Clear", color = TextPrimary, fontSize = 14.sp) }
                        }
                }
        }

        if (showClearCacheConfirm) {
                androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showClearCacheConfirm = false },
                        title = { Text("Clear Cache", color = TextPrimary) },
                        text = {
                                Text(
                                        "This will clear all cached data. Continue?",
                                        color = TextSecondary
                                )
                        },
                        confirmButton = {
                                androidx.compose.material3.TextButton(
                                        onClick = {
                                                try {
                                                        context.cacheDir.deleteRecursively()
                                                        android.widget.Toast.makeText(
                                                                        context,
                                                                        "Cache cleared",
                                                                        android.widget.Toast
                                                                                .LENGTH_SHORT
                                                                )
                                                                .show()
                                                } catch (e: Exception) {
                                                        android.widget.Toast.makeText(
                                                                        context,
                                                                        "Error: ${e.message}",
                                                                        android.widget.Toast
                                                                                .LENGTH_SHORT
                                                                )
                                                                .show()
                                                }
                                                showClearCacheConfirm = false
                                        }
                                ) { Text("Clear", color = Color(0xFFDC3545)) }
                        },
                        dismissButton = {
                                androidx.compose.material3.TextButton(
                                        onClick = { showClearCacheConfirm = false }
                                ) { Text("Cancel", color = TextSecondary) }
                        },
                        containerColor = CardDark
                )
        }
}
