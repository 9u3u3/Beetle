package com.bsbeetle.bananaspaghetti.data.sidebar


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchTab() {
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