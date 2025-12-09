package com.bsbeetle.bananaspaghetti.ui.home

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
    var selectedCategory by remember { mutableStateOf("Keyboard") }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Left sidebar
        SettingsSidebar(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
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
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(SidebarDark)
            .padding(16.dp)
    ) {
        // Profile section
        ProfileCard()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search bar
        SearchBar()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Navigation items
        val categories = listOf("Keyboard")
        
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
private fun ProfileCard() {
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
                text = "User",
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
                onLeftToggle = { keyboardViewModel.toggleLeftKeyboardVisibility() },
                onRightToggle = { keyboardViewModel.toggleRightKeyboardVisibility() }
            )
        }
    }
}

@Composable
private fun KeyboardSettingsContent(
    isLeftVisible: Boolean,
    isRightVisible: Boolean,
    onLeftToggle: () -> Unit,
    onRightToggle: () -> Unit
) {
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
