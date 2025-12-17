package com.bsbeetle.bananaspaghetti.data.sidebar


import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bsbeetle.bananaspaghetti.utils.resourceHandling.ImageLoaderSingleton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeItem(
    file: FileItem,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggleExpand: () -> Unit,
    onFileClick: () -> Unit,
    // Multi-select support
    isMultiSelectMode: Boolean = false,
    isMultiSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onMultiSelectTap: () -> Unit = {}
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chevronRotation"
    )
    
    // Background color based on selection state
    val backgroundColor = when {
        isMultiSelected -> Color(0xFF264F78) // Blue highlight for multi-selected
        isSelected -> Color(0xFF37373D) // Existing selection highlight
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) {
                        // In multi-select mode, taps toggle selection
                        onMultiSelectTap()
                    } else {
                        // Normal mode behavior
                        if (file.isDirectory) {
                            onToggleExpand()
                        } else {
                            onFileClick()
                        }
                    }
                },
                onLongClick = {
                    // Long press enters multi-select mode
                    onLongPress()
                }
            )
            .padding(start = (8 + file.level * 16).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show checkbox when in multi-select mode
        if (isMultiSelectMode) {
            Checkbox(
                checked = isMultiSelected,
                onCheckedChange = { onMultiSelectTap() },
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF007ACC),
                    uncheckedColor = Color(0xFF858585),
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
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
            if (!isMultiSelectMode) {
                Spacer(modifier = Modifier.width(22.dp))
            }
            val context = LocalContext.current
            val iconPath = FileIconUtils.getIconPath(file.name)
            AsyncImage(
                model = iconPath,
                imageLoader = ImageLoaderSingleton.get(context),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = file.name,
            color = when {
                isMultiSelected -> Color(0xFFFFFFFF)
                isSelected -> Color(0xFFE8E8E8)
                else -> Color(0xFFCCCCCC)
            },
            fontSize = 13.sp,
            fontWeight = if (isSelected || isMultiSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}