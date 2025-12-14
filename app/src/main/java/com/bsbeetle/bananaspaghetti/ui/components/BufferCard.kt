package com.bsbeetle.bananaspaghetti.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import com.bsbeetle.bananaspaghetti.data.sidebar.FileIconUtils
import com.bsbeetle.bananaspaghetti.utils.resourceHandling.ImageLoaderSingleton

@Composable
fun BufferCard(
    file: DocumentFile,
    isSelected: Boolean,
    isDirty: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    context: Context
) {
    val fileName = file.name ?: "Unknown"
    
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(Color(0xFF1B1B1B))
    ) {
        Row(
            modifier = Modifier
                .height(28.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val iconPath = FileIconUtils.getIconPath(fileName)
            AsyncImage(
                model = iconPath,
                imageLoader = ImageLoaderSingleton.get(context),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            AnimatedContent(
                targetState = fileName,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                },
                label = "fileName"
            ) { name ->
                Text(
                    text = name,
                    color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }

            // Show white dot for unsaved files, close button otherwise
            if (isDirty) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(4.dp)
                        .background(Color.White, CircleShape)
                        .clickable(onClick = onClose)
                )
            } else {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isSelected) Color(0xFFB0B0B0) else Color(0xFF6A6A6A),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        if (isSelected) {
            HorizontalDivider(
                modifier = Modifier
                    .widthIn(min = 20.dp)
                    .height(1.dp),
                color = Color(0xFF4379FF)
            )
        }
    }
}