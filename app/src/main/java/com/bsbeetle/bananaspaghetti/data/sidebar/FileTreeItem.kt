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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FileTreeItem(
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
                tint = FileUtils.getFileIconColor(file.name),
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