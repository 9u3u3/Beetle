package com.bsbeetle.bananaspaghetti.ui.Extension

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * Terminal-style dialog showing extension installation progress.
 * This dialog is unclosable until installation completes.
 */
@Composable
fun InstallationDialog(
    extensionName: String,
    outputLines: List<String>,
    isComplete: Boolean,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new output arrives
    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(outputLines.size - 1)
            }
        }
    }
    
    Dialog(
        onDismissRequest = {
            // Only allow dismiss when complete
            if (isComplete || isError) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = isComplete || isError,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0D0D0D)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Installing $extensionName",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (!isComplete && !isError) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF4EC9B0),
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Terminal output area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            Color(0xFF1E1E1E),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    if (outputLines.isEmpty()) {
                        Text(
                            text = "Starting installation...",
                            color = Color(0xFF4EC9B0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(outputLines) { line ->
                                Text(
                                    text = line,
                                    color = when {
                                        line.contains("error", ignoreCase = true) ||
                                        line.contains("failed", ignoreCase = true) -> Color(0xFFFF6B6B)
                                        line.contains("warning", ignoreCase = true) -> Color(0xFFFFB86C)
                                        line.contains("success", ignoreCase = true) ||
                                        line.contains("complete", ignoreCase = true) ||
                                        line.contains("done", ignoreCase = true) -> Color(0xFF50FA7B)
                                        line.startsWith("$") || line.startsWith("#") -> Color(0xFF8BE9FD)
                                        else -> Color(0xFFCCCCCC)
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Status and button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isError -> "Installation failed"
                            isComplete -> "Installation complete!"
                            else -> "Please wait..."
                        },
                        color = when {
                            isError -> Color(0xFFFF6B6B)
                            isComplete -> Color(0xFF50FA7B)
                            else -> Color(0xFF888888)
                        },
                        fontSize = 13.sp
                    )
                    
                    if (isComplete || isError) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isError) Color(0xFFFF6B6B) else Color(0xFF4EC9B0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isError) "Close" else "Done",
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
