package com.bsbeetle.bananaspaghetti.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.utils.fileHandling.FileOperations
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

/**
 * Popup shown in portrait mode when multi-select mode is active.
 * Provides a confirmation button and Move, Copy, Delete options.
 */
@Composable
fun MultiSelectActionsPopup(
    show: Boolean,
    fileViewModel: FileViewModel,
    selectedFilePaths: Set<String>,
    onDismiss: () -> Unit,
    onOperationComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Destination folder picker for paste after copy/move
    val pasteDestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val destFolder = DocumentFile.fromTreeUri(context, it)
                destFolder?.let { dest ->
                    FileOperations.paste(context, dest, fileViewModel)
                    fileViewModel.exitMultiSelectMode()
                    onOperationComplete()
                    onDismiss()
                    Toast.makeText(context, "Pasted successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Backdrop
        AnimatedVisibility(
            visible = show,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { 
                        // Cancel selection on backdrop click
                        fileViewModel.exitMultiSelectMode()
                        onDismiss()
                    }
            )
        }

        // Popup content
        AnimatedVisibility(
            visible = show,
            enter = scaleIn(initialScale = 0.9f) + fadeIn(),
            exit = scaleOut(targetScale = 0.9f) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with count and cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedFilePaths.size} items selected",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                fileViewModel.exitMultiSelectMode()
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Cancel",
                                tint = Color(0xFF888888)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Show action buttons directly (confirmation is done via floating button in sidebar)
                    PopupActionButton(
                        icon = Icons.Rounded.DriveFileMove,
                        text = "Move",
                        color = Color(0xFF007ACC),
                        onClick = {
                            // Copy all selected files to clipboard as "cut"
                            val files = selectedFilePaths.mapNotNull { path ->
                                try {
                                    val uri = android.net.Uri.parse(path)
                                    DocumentFile.fromSingleUri(context, uri) 
                                        ?: DocumentFile.fromTreeUri(context, uri)
                                } catch (e: Exception) { null }
                            }
                            if (files.isNotEmpty()) {
                                FileOperations.cutMultipleToClipboard(files, context)
                                pasteDestLauncher.launch(null)
                            }
                        }
                    )
                    
                    PopupActionButton(
                        icon = Icons.Rounded.ContentCopy,
                        text = "Copy",
                        color = Color(0xFF4CAF50),
                        onClick = {
                            // Copy all selected files to clipboard
                            val files = selectedFilePaths.mapNotNull { path ->
                                try {
                                    val uri = android.net.Uri.parse(path)
                                    DocumentFile.fromSingleUri(context, uri) 
                                        ?: DocumentFile.fromTreeUri(context, uri)
                                } catch (e: Exception) { null }
                            }
                            if (files.isNotEmpty()) {
                                FileOperations.copyMultipleToClipboard(files, context)
                                pasteDestLauncher.launch(null)
                            }
                        }
                    )
                    
                    PopupActionButton(
                        icon = Icons.Rounded.Delete,
                        text = "Delete",
                        color = Color(0xFFE53935),
                        onClick = { showDeleteConfirmation = true }
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ${selectedFilePaths.size} items?", color = Color.White) },
            text = { 
                Text(
                    "This action cannot be undone.", 
                    color = Color(0xFF888888)
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        // Delete all selected files
                        val files = selectedFilePaths.mapNotNull { path ->
                            try {
                                val uri = android.net.Uri.parse(path)
                                DocumentFile.fromSingleUri(context, uri)
                                    ?: DocumentFile.fromTreeUri(context, uri)
                            } catch (e: Exception) { null }
                        }
                        var deleted = 0
                        files.forEach { file ->
                            if (FileOperations.deleteFile(context, file, fileViewModel, showToast = false)) {
                                deleted++
                            }
                        }
                        Toast.makeText(context, "Deleted $deleted items", Toast.LENGTH_SHORT).show()
                        fileViewModel.exitMultiSelectMode()
                        onOperationComplete()
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = Color(0xFF888888))
                }
            },
            containerColor = Color(0xFF252526)
        )
    }
}

@Composable
private fun PopupActionButton(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D2D2D))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
