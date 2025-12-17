package com.bsbeetle.bananaspaghetti.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import com.bsbeetle.bananaspaghetti.utils.fileHandling.FileOperations
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

/**
 * Panel shown in TopRightBox when multi-select mode is active (landscape mode).
 * Uses the same layout style as FileOptionsPanel for consistency.
 */
@Composable
fun MultiSelectActionsPanel(
    fileViewModel: FileViewModel,
    selectedFilePaths: Set<String>,
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
                    Toast.makeText(context, "Pasted successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header with selected count and cancel button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${selectedFilePaths.size} items selected",
                    color = GitTheme.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = { fileViewModel.exitMultiSelectMode() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Cancel selection",
                        tint = GitTheme.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        item {
            HorizontalDivider(
                color = GitTheme.BorderColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        // Move option
        item {
            FileOptionItem(
                icon = R.drawable.file_type_text,
                title = "Move",
                onClick = {
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
        }
        
        // Copy option
        item {
            FileOptionItem(
                icon = R.drawable.file_type_text,
                title = "Copy",
                onClick = {
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
        }
        
        item {
            HorizontalDivider(
                color = GitTheme.BorderColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        // Delete option
        item {
            FileOptionItem(
                icon = R.drawable.file_type_text,
                title = "Delete",
                onClick = { showDeleteConfirmation = true }
            )
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
                    color = GitTheme.TextSecondary
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
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
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = GitTheme.TextSecondary)
                }
            },
            containerColor = Color(0xFF252526)
        )
    }
}

