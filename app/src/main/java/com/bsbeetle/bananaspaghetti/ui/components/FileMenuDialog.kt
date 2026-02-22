package com.bsbeetle.bananaspaghetti.ui.components

import android.util.Log
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.bsbeetle.bananaspaghetti.utils.fileHandling.FileOperations
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

enum class FileMenuAction {
    NONE,
    NEW_FILE,
    NEW_FOLDER,
    OPEN_FILE,
    OPEN_FOLDER,
    RENAME_FILE,
    DELETE_FILE,
    DELETE_FOLDER,
    COPY_FILE,
    COPY_FOLDER,
    MOVE_FILE,
    MOVE_FOLDER,
    PASTE
}

@Composable
fun FileMenuDialog(
        show: Boolean,
        onDismiss: () -> Unit,
        bufferViewModel: BufferViewModel,
        fileViewModel: FileViewModel,
        onFileOpened: (DocumentFile) -> Unit,
        onFolderOpened: (DocumentFile) -> Unit,
        onSshRemote: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var currentAction by remember { mutableStateOf(FileMenuAction.NONE) }
    var newFileName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var renameFileName by remember { mutableStateOf("") }
    var fileToRename by remember { mutableStateOf<DocumentFile?>(null) }
    var selectedFolder by remember { mutableStateOf<DocumentFile?>(null) }

    // For file/folder type selection dialog
    var pendingOperation by remember { mutableStateOf<String?>(null) }
    var showTypeSelector by remember { mutableStateOf(false) }

    // File picker launcher
    val openFileLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromSingleUri(context, it)
                        documentFile?.let { file -> onFileOpened(file) }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error opening file", e)
                        Toast.makeText(context, "Error opening file", Toast.LENGTH_SHORT).show()
                    }
                }
                onDismiss()
            }

    // Folder picker launcher
    val openFolderLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromTreeUri(context, it)
                        documentFile?.let { folder ->
                            if (currentAction == FileMenuAction.OPEN_FOLDER) {
                                onFolderOpened(folder)
                            } else {
                                selectedFolder = folder
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error opening folder", e)
                        Toast.makeText(context, "Error opening folder", Toast.LENGTH_SHORT).show()
                    }
                }
                if (currentAction == FileMenuAction.OPEN_FOLDER) {
                    onDismiss()
                }
            }

    // Rename file picker launcher
    val renameFileLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromSingleUri(context, it)
                        documentFile?.let { file ->
                            fileToRename = file
                            renameFileName = file.name ?: ""
                            currentAction = FileMenuAction.RENAME_FILE
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error selecting file for rename", e)
                        Toast.makeText(context, "Error selecting file", Toast.LENGTH_SHORT).show()
                        currentAction = FileMenuAction.NONE
                    }
                }
                if (uri == null) {
                    currentAction = FileMenuAction.NONE
                }
            }

    // Rename folder picker launcher
    val renameFolderLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromTreeUri(context, it)
                        documentFile?.let { folder ->
                            fileToRename = folder
                            renameFileName = folder.name ?: ""
                            currentAction = FileMenuAction.RENAME_FILE
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error selecting folder for rename", e)
                        Toast.makeText(context, "Error selecting folder", Toast.LENGTH_SHORT).show()
                        currentAction = FileMenuAction.NONE
                    }
                }
                if (uri == null) {
                    currentAction = FileMenuAction.NONE
                }
            }

    // Delete file launcher
    val deleteFileLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromSingleUri(context, it)
                        documentFile?.let { file ->
                            if (fileViewModel != null) {
                                FileOperations.deleteFile(context, file, fileViewModel)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error deleting file", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

    // Delete folder launcher
    val deleteFolderLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromTreeUri(context, it)
                        documentFile?.let { folder ->
                            if (fileViewModel != null) {
                                FileOperations.deleteFile(context, folder, fileViewModel)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error deleting folder", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

    // Copy file launcher
    val copyFileLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromSingleUri(context, it)
                        documentFile?.let { file -> FileOperations.copyToClipboard(file, context) }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error copying file", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

    // Copy folder launcher
    val copyFolderLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromTreeUri(context, it)
                        documentFile?.let { folder ->
                            FileOperations.copyToClipboard(folder, context)
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error copying folder", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

    // Move (cut) file launcher
    val moveFileLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromSingleUri(context, it)
                        documentFile?.let { file -> FileOperations.cutToClipboard(file, context) }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error cutting file", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

    // Move (cut) folder launcher
    val moveFolderLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromTreeUri(context, it)
                        documentFile?.let { folder ->
                            FileOperations.cutToClipboard(folder, context)
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error cutting folder", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

    // Paste destination launcher
    val pasteDestLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromTreeUri(context, it)
                        documentFile?.let { destFolder ->
                            if (fileViewModel != null) {
                                FileOperations.paste(context, destFolder, fileViewModel)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error pasting", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

    // Centered popup overlay
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
                visible = show,
                enter = scaleIn(initialScale = 0.9f) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut()
        ) {
            // Backdrop
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { onDismiss() }
            )
        }

        AnimatedVisibility(
                visible = show,
                enter = scaleIn(initialScale = 0.9f) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut()
        ) {
            when (currentAction) {
                FileMenuAction.NONE -> {
                    // Main menu popup
                    Box(
                            modifier =
                                    Modifier.widthIn(max = 300.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF252526))
                                            .padding(16.dp)
                    ) {
                        Column {
                            // Title
                            Text(
                                    text = "File",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                            )

                            FileMenuItem(
                                    icon = R.drawable.article_icon,
                                    title = "New File",
                                    onClick = { currentAction = FileMenuAction.NEW_FILE }
                            )

                            HorizontalDivider(
                                    color = Color(0xFF3C3C3C),
                                    modifier = Modifier.padding(vertical = 4.dp)
                            )

                            FileMenuItem(
                                    icon = R.drawable.folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                                    title = "New Folder",
                                    onClick = { currentAction = FileMenuAction.NEW_FOLDER }
                            )

                            HorizontalDivider(
                                    color = Color(0xFF3C3C3C),
                                    modifier = Modifier.padding(vertical = 4.dp)
                            )

                            FileMenuItem(
                                    icon =
                                            R.drawable
                                                    .file_open_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                                    title = "Open File",
                                    onClick = { openFileLauncher.launch(arrayOf("*/*")) }
                            )

                            HorizontalDivider(
                                    color = Color(0xFF3C3C3C),
                                    modifier = Modifier.padding(vertical = 4.dp)
                            )

                            FileMenuItem(
                                    icon = R.drawable.computer_folder_open_icon,
                                    title = "Open Folder",
                                    onClick = {
                                        currentAction = FileMenuAction.OPEN_FOLDER
                                        openFolderLauncher.launch(null)
                                    }
                            )

                            if (onSshRemote != null) {
                                HorizontalDivider(
                                        color = Color(0xFF3C3C3C),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                )

                                FileMenuItem(
                                        icon = R.drawable.network_intel,
                                        title = "SSH Remote",
                                        onClick = { onSshRemote() }
                                )
                            }

                            HorizontalDivider(
                                    color = Color(0xFF3C3C3C),
                                    modifier = Modifier.padding(vertical = 4.dp)
                            )

                            FileMenuItem(
                                    icon = R.drawable.file_type_text,
                                    title = "Rename",
                                    onClick = {
                                        pendingOperation = "rename"
                                        showTypeSelector = true
                                    }
                            )

                            if (FileOperations.hasClipboardContent()) {
                                HorizontalDivider(
                                        color = Color(0xFF3C3C3C),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                )

                                FileMenuItem(
                                        icon = R.drawable.computer_folder_open_icon,
                                        title = "Paste",
                                        onClick = { pasteDestLauncher.launch(null) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Cancel button
                            TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.align(Alignment.End)
                            ) { Text("Cancel", color = Color(0xFF888888)) }
                        }
                    }

                    // Type selection dialog - inside NONE case but outside the Card
                    if (showTypeSelector) {
                        androidx.compose.material3.AlertDialog(
                                onDismissRequest = {
                                    showTypeSelector = false
                                    pendingOperation = null
                                },
                                title = { Text("Select Type", color = Color.White) },
                                text = {
                                    Text(
                                            "What do you want to ${pendingOperation}?",
                                            color = Color(0xFF888888)
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                            onClick = {
                                                showTypeSelector = false
                                                when (pendingOperation) {
                                                    "rename" ->
                                                            renameFileLauncher.launch(
                                                                    arrayOf("*/*")
                                                            )
                                                    "delete" ->
                                                            deleteFileLauncher.launch(
                                                                    arrayOf("*/*")
                                                            )
                                                    "copy" ->
                                                            copyFileLauncher.launch(arrayOf("*/*"))
                                                    "move" ->
                                                            moveFileLauncher.launch(arrayOf("*/*"))
                                                }
                                                pendingOperation = null
                                            }
                                    ) { Text("File", color = Color(0xFF007ACC)) }
                                },
                                dismissButton = {
                                    TextButton(
                                            onClick = {
                                                showTypeSelector = false
                                                when (pendingOperation) {
                                                    "rename" -> renameFolderLauncher.launch(null)
                                                    "delete" -> deleteFolderLauncher.launch(null)
                                                    "copy" -> copyFolderLauncher.launch(null)
                                                    "move" -> moveFolderLauncher.launch(null)
                                                }
                                                pendingOperation = null
                                            }
                                    ) { Text("Folder", color = Color(0xFF007ACC)) }
                                },
                                containerColor = Color(0xFF252526)
                        )
                    }
                }
                FileMenuAction.NEW_FILE -> {
                    NewItemPopup(
                            title = "New File",
                            placeholder = "filename.txt",
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            selectedFolder = selectedFolder,
                            onSelectFolder = { openFolderLauncher.launch(null) },
                            onConfirm = {
                                if (newFileName.isNotBlank()) {
                                    com.bsbeetle.bananaspaghetti.utils.fileHandling.FileOperations
                                            .createNewFile(
                                                    context,
                                                    selectedFolder,
                                                    newFileName,
                                                    onFileOpened,
                                                    fileViewModel
                                            )
                                    newFileName = ""
                                    selectedFolder = null
                                    currentAction = FileMenuAction.NONE
                                    onDismiss()
                                }
                            },
                            onCancel = {
                                newFileName = ""
                                currentAction = FileMenuAction.NONE
                            }
                    )
                }
                FileMenuAction.NEW_FOLDER -> {
                    NewItemPopup(
                            title = "New Folder",
                            placeholder = "folder_name",
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            selectedFolder = selectedFolder,
                            onSelectFolder = { openFolderLauncher.launch(null) },
                            onConfirm = {
                                if (newFolderName.isNotBlank()) {
                                    com.bsbeetle.bananaspaghetti.utils.fileHandling.FileOperations
                                            .createNewFolder(
                                                    context,
                                                    selectedFolder,
                                                    newFolderName,
                                                    fileViewModel
                                            )
                                    newFolderName = ""
                                    selectedFolder = null
                                    currentAction = FileMenuAction.NONE
                                    onDismiss()
                                }
                            },
                            onCancel = {
                                newFolderName = ""
                                currentAction = FileMenuAction.NONE
                            }
                    )
                }
                FileMenuAction.RENAME_FILE -> {
                    if (fileToRename != null) {
                        NewItemPopup(
                                title = "Rename",
                                placeholder = "new_name.ext",
                                value = renameFileName,
                                onValueChange = { renameFileName = it },
                                selectedFolder = null, // Not needed for rename
                                onSelectFolder = {}, // No-op
                                onConfirm = {
                                    if (renameFileName.isNotBlank()) {
                                        com.bsbeetle.bananaspaghetti.utils.fileHandling
                                                .FileOperations.renameFile(
                                                context,
                                                fileToRename!!,
                                                renameFileName,
                                                fileViewModel
                                        )
                                        renameFileName = ""
                                        fileToRename = null
                                        currentAction = FileMenuAction.NONE
                                        onDismiss()
                                    }
                                },
                                onCancel = {
                                    renameFileName = ""
                                    fileToRename = null
                                    currentAction = FileMenuAction.NONE
                                },
                                isRename = true
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun FileMenuItem(icon: Int, title: String, onClick: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onClick)
                            .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun NewItemPopup(
        title: String,
        placeholder: String,
        value: String,
        onValueChange: (String) -> Unit,
        selectedFolder: DocumentFile?,
        onSelectFolder: () -> Unit,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
        isRename: Boolean = false
) {
    val currentSession =
            SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession
    val projectRoot = currentSession?.projectRoot

    Box(
            modifier =
                    Modifier.widthIn(max = 320.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF252526))
                            .padding(16.dp)
    ) {
        Column {
            Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(placeholder, color = Color(0xFF666666)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF007ACC),
                                    unfocusedBorderColor = Color(0xFF3C3C3C),
                                    cursorColor = Color.White
                            )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(12.dp))

            if (!isRename) {
                Text(text = "Location:", color = Color(0xFFCCCCCC), fontSize = 12.sp)

                Spacer(modifier = Modifier.height(6.dp))

                val displayFolder = selectedFolder?.name ?: projectRoot?.name ?: "Select folder..."

                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E1E1E))
                                        .clickable(onClick = onSelectFolder)
                                        .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            painter =
                                    painterResource(
                                            R.drawable.folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                    ),
                            contentDescription = "Folder",
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = displayFolder,
                            color =
                                    if (selectedFolder != null || projectRoot != null) Color.White
                                    else Color(0xFF666666),
                            fontSize = 13.sp
                    )
                }

                if (projectRoot != null && selectedFolder == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Using project root", color = Color(0xFF50fa7b), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text("Cancel", color = Color(0xFF888888)) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onConfirm, enabled = value.isNotBlank()) {
                    Text(
                            if (isRename) "Rename" else "Create",
                            color = if (value.isNotBlank()) Color(0xFF007ACC) else Color(0xFF666666)
                    )
                }
            }
        }
    }
}
