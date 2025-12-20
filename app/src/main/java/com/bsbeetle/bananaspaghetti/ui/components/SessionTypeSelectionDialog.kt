package com.bsbeetle.bananaspaghetti.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bsbeetle.bananaspaghetti.R

/**
 * Dialog for selecting session type when opening files/folders.
 * Allows user to choose between Editor and Notebook session types.
 */
@Composable
fun SessionTypeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectEditor: () -> Unit,
    onSelectNotebook: () -> Unit,
    title: String = "Open in..."
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Options row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Editor option
                    SessionTypeOption(
                        iconRes = R.drawable.article_icon,
                        label = "Editor",
                        description = "Code editing with syntax highlighting",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSelectEditor()
                            onDismiss()
                        }
                    )
                    
                    // Notebook option
                    SessionTypeOption(
                        iconRes = R.drawable.note_add_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                        label = "Notebook",
                        description = "Python cells with execution",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSelectNotebook()
                            onDismiss()
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF888888),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Individual session type option card.
 */
@Composable
private fun SessionTypeOption(
    iconRes: Int,
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2D2D2D))
            .border(1.dp, Color(0xFF3D3D3D), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color(0xFF007ACC),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Label
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Description
        Text(
            text = description,
            color = Color(0xFF888888),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 2
        )
    }
}
