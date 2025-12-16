package com.bsbeetle.bananaspaghetti.ui.components

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.data.Ic
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import com.bsbeetle.bananaspaghetti.utils.fileHandling.FileOperations
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

// FileMenuAction enum is defined in FileMenuDialog.kt (same package)

/**
 * File options panel for landscape mode. Contains options for new file, new folder, open file, open
 * folder, rename file.
 */
@Composable
fun FileOptionsPanel(
        fileViewModel: FileViewModel?,
        bufferViewModel: BufferViewModel?,
        onFileOpened: (DocumentFile) -> Unit,
        onFolderOpened: (DocumentFile) -> Unit
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
            }

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
            }

    val renameFileLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri ->
                Log.d("FileMenu", "Rename file launcher callback - uri: $uri")
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromSingleUri(context, it)
                        Log.d(
                                "FileMenu",
                                "DocumentFile: $documentFile, name: ${documentFile?.name}"
                        )
                        documentFile?.let { file ->
                            fileToRename = file
                            renameFileName = file.name ?: ""
                            currentAction = FileMenuAction.RENAME_FILE
                            Log.d(
                                    "FileMenu",
                                    "Set currentAction=RENAME_FILE, fileToRename=${fileToRename?.name}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error selecting file for rename", e)
                        Toast.makeText(context, "Error selecting file", Toast.LENGTH_SHORT).show()
                        currentAction = FileMenuAction.NONE
                    }
                }
                if (uri == null) {
                    Log.d("FileMenu", "Rename launcher - uri was null, user cancelled")
                    currentAction = FileMenuAction.NONE
                }
            }

    val renameFolderLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                Log.d("FileMenu", "Rename folder launcher callback - uri: $uri")
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        val documentFile = DocumentFile.fromTreeUri(context, it)
                        Log.d(
                                "FileMenu",
                                "DocumentFile (folder): $documentFile, name: ${documentFile?.name}"
                        )
                        documentFile?.let { folder ->
                            fileToRename = folder
                            renameFileName = folder.name ?: ""
                            currentAction = FileMenuAction.RENAME_FILE
                            Log.d(
                                    "FileMenu",
                                    "Set currentAction=RENAME_FILE, fileToRename=${fileToRename?.name}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("FileMenu", "Error selecting folder for rename", e)
                        Toast.makeText(context, "Error selecting folder", Toast.LENGTH_SHORT).show()
                        currentAction = FileMenuAction.NONE
                    }
                }
                if (uri == null) {
                    Log.d("FileMenu", "Rename folder launcher - uri was null, user cancelled")
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

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (currentAction == FileMenuAction.NONE) {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                            text = "FILE OPTIONS",
                            color = GitTheme.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    FileOptionItem(
                            icon = R.drawable.article_icon,
                            title = "New File",
                            onClick = { currentAction = FileMenuAction.NEW_FILE }
                    )
                }
                item {
                    FileOptionItem(
                            icon = R.drawable.folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                            title = "New Folder",
                            onClick = { currentAction = FileMenuAction.NEW_FOLDER }
                    )
                }
                item {
                    HorizontalDivider(
                            color = GitTheme.BorderColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                item {
                    FileOptionItem(
                            icon = R.drawable.file_open_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                            title = "Open File",
                            onClick = { openFileLauncher.launch(arrayOf("*/*")) }
                    )
                }
                item {
                    FileOptionItem(
                            icon = R.drawable.computer_folder_open_icon,
                            title = "Open Folder",
                            onClick = {
                                currentAction = FileMenuAction.OPEN_FOLDER
                                openFolderLauncher.launch(null)
                            }
                    )
                }
                item {
                    HorizontalDivider(
                            color = GitTheme.BorderColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                item {
                    FileOptionItem(
                            icon = R.drawable.file_type_text,
                            title = "Rename",
                            onClick = {
                                pendingOperation = "rename"
                                showTypeSelector = true
                            }
                    )
                }
                item {
                    FileOptionItem(
                            icon = R.drawable.file_type_text,
                            title = "Delete",
                            onClick = {
                                pendingOperation = "delete"
                                showTypeSelector = true
                            }
                    )
                }
                item {
                    HorizontalDivider(
                            color = GitTheme.BorderColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                item {
                    FileOptionItem(
                            icon = R.drawable.file_type_text,
                            title = "Copy",
                            onClick = {
                                pendingOperation = "copy"
                                showTypeSelector = true
                            }
                    )
                }
                item {
                    FileOptionItem(
                            icon = R.drawable.file_type_text,
                            title = "Move",
                            onClick = {
                                pendingOperation = "move"
                                showTypeSelector = true
                            }
                    )
                }
                if (FileOperations.hasClipboardContent()) {
                    item {
                        HorizontalDivider(
                                color = GitTheme.BorderColor,
                                modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    item {
                        FileOptionItem(
                                icon = R.drawable.computer_folder_open_icon,
                                title = "Paste",
                                onClick = { pasteDestLauncher.launch(null) }
                        )
                    }
                }
            }

            // Type selection dialog
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
                                    color = GitTheme.TextSecondary
                            )
                        },
                        confirmButton = {
                            TextButton(
                                    onClick = {
                                        showTypeSelector = false
                                        when (pendingOperation) {
                                            "rename" -> renameFileLauncher.launch(arrayOf("*/*"))
                                            "delete" -> deleteFileLauncher.launch(arrayOf("*/*"))
                                            "copy" -> copyFileLauncher.launch(arrayOf("*/*"))
                                            "move" -> moveFileLauncher.launch(arrayOf("*/*"))
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
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable {
                                            currentAction = FileMenuAction.NONE
                                            newFileName = ""
                                            newFolderName = ""
                                            renameFileName = ""
                                            selectedFolder = null
                                            fileToRename = null
                                        }
                                        .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Rounded.ArrowBack,
                            "Back",
                            tint = GitTheme.TextSecondary,
                            modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back", color = GitTheme.TextSecondary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (currentAction) {
                    FileMenuAction.NEW_FILE -> {
                        InlineInputForm(
                                title = "New File",
                                placeholder = "filename.txt",
                                value = newFileName,
                                onValueChange = { newFileName = it },
                                selectedFolder = selectedFolder,
                                onSelectFolder = { openFolderLauncher.launch(null) },
                                onConfirm = {
                                    if (newFileName.isNotBlank() && fileViewModel != null) {
                                        FileOperations.createNewFile(
                                                context,
                                                selectedFolder,
                                                newFileName,
                                                onFileOpened,
                                                fileViewModel
                                        )
                                        newFileName = ""
                                        selectedFolder = null
                                        currentAction = FileMenuAction.NONE
                                    }
                                },
                                confirmText = "Create"
                        )
                    }
                    FileMenuAction.NEW_FOLDER -> {
                        InlineInputForm(
                                title = "New Folder",
                                placeholder = "folder_name",
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                selectedFolder = selectedFolder,
                                onSelectFolder = { openFolderLauncher.launch(null) },
                                onConfirm = {
                                    if (newFolderName.isNotBlank() && fileViewModel != null) {
                                        FileOperations.createNewFolder(
                                                context,
                                                selectedFolder,
                                                newFolderName,
                                                fileViewModel
                                        )
                                        newFolderName = ""
                                        selectedFolder = null
                                        currentAction = FileMenuAction.NONE
                                    }
                                },
                                confirmText = "Create"
                        )
                    }
                    FileMenuAction.RENAME_FILE -> {
                        if (fileToRename != null) {
                            InlineInputForm(
                                    title = "Rename",
                                    placeholder = "new_name.ext",
                                    value = renameFileName,
                                    onValueChange = { renameFileName = it },
                                    selectedFolder = null,
                                    onSelectFolder = {},
                                    onConfirm = {
                                        if (renameFileName.isNotBlank() && fileViewModel != null) {
                                            FileOperations.renameFile(
                                                    context,
                                                    fileToRename!!,
                                                    renameFileName,
                                                    fileViewModel
                                            )
                                            renameFileName = ""
                                            fileToRename = null
                                            currentAction = FileMenuAction.NONE
                                        }
                                    },
                                    confirmText = "Rename",
                                    isRename = true
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

/** File option item for the file menu. */
@Composable
fun FileOptionItem(icon: Int, title: String, onClick: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(GitTheme.BgCard)
                            .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(6.dp))
                            .clickable(onClick = onClick)
                            .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = GitTheme.TextSecondary,
                modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = GitTheme.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Inline input form for file operations. */
@Composable
fun InlineInputForm(
        title: String,
        placeholder: String,
        value: String,
        onValueChange: (String) -> Unit,
        selectedFolder: DocumentFile?,
        onSelectFolder: () -> Unit,
        onConfirm: () -> Unit,
        confirmText: String,
        isRename: Boolean = false
) {
    val currentSession =
            SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession
    val projectRoot = currentSession?.projectRoot

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        LandscapeInputField(value = value, placeholder = placeholder, onValueChange = onValueChange)

        if (!isRename) {
            val displayFolder = selectedFolder?.name ?: projectRoot?.name ?: "Select folder..."

            Column {
                Text(
                        "Location:",
                        color = GitTheme.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GitTheme.BgCard)
                                        .border(
                                                1.dp,
                                                GitTheme.BorderColor,
                                                RoundedCornerShape(6.dp)
                                        )
                                        .clickable(onClick = onSelectFolder)
                                        .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            painter =
                                    painterResource(
                                            R.drawable.folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                    ),
                            contentDescription = "Folder",
                            tint = GitTheme.TextSecondary,
                            modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            displayFolder,
                            color =
                                    if (selectedFolder != null || projectRoot != null)
                                            GitTheme.TextPrimary
                                    else GitTheme.TextTertiary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Button(
                onClick = onConfirm,
                enabled = value.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = GitTheme.AccentBlue,
                                disabledContainerColor = GitTheme.BgHover
                        ),
                shape = RoundedCornerShape(6.dp)
        ) { Text(confirmText, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

/**
 * Native EditText wrapper for landscape mode. Uses the custom split keyboard via the session's
 * input connection.
 */
@Composable
fun LandscapeInputField(
        value: String,
        placeholder: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier
) {
    val editTextRef = remember { mutableStateOf<EditText?>(null) }
    // Safe access to session
    val activeSession =
            if (SessionList.sessions.isNotEmpty() && SessionList.activeSessionIndex != null) {
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex!!)
            } else null

    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GitTheme.BgCard)
                            .border(1.dp, GitTheme.BorderColor, RoundedCornerShape(6.dp))
    ) {
        AndroidView(
                factory = { ctx ->
                    EditText(ctx).apply {
                        showSoftInputOnFocus = false // Disable IME keyboard
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 14f
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setPadding(24, 16, 24, 16)
                        hint = placeholder
                        setHintTextColor(android.graphics.Color.parseColor("#6A6A6A"))
                        setText(value)
                        setSingleLine(true)

                        editTextRef.value = this

                        addTextChangedListener(
                                object : TextWatcher {
                                    override fun beforeTextChanged(
                                            s: CharSequence?,
                                            start: Int,
                                            count: Int,
                                            after: Int
                                    ) {}
                                    override fun onTextChanged(
                                            s: CharSequence?,
                                            start: Int,
                                            before: Int,
                                            count: Int
                                    ) {
                                        val txt = s?.toString() ?: ""
                                        onValueChange(txt)
                                    }
                                    override fun afterTextChanged(s: Editable?) {}
                                }
                        )

                        setOnFocusChangeListener { _, hasFocus ->
                            if (hasFocus) {
                                post {
                                    // Set global Ic.ic so keyboard routes to this EditText
                                    Ic.ic = onCreateInputConnection(EditorInfo())
                                }
                            } else {
                                // Clear when focus is lost so keyboard routes back to session
                                Ic.ic = null
                            }
                        }

                        // Request focus immediately to enable keyboard input
                        post { requestFocus() }
                    }
                },
                update = { editText ->
                    if (editText.text.toString() != value) {
                        // Prevent cursor jumping if text is same
                        val selectionStart = editText.selectionStart
                        editText.setText(value)
                        try {
                            editText.setSelection(kotlin.math.min(selectionStart, value.length))
                        } catch (e: Exception) {
                            editText.setSelection(value.length)
                        }
                    }

                    // Re-establish connection if lost
                    if (editText.isFocused &&
                                    activeSession != null &&
                                    activeSession._inputConnection == null
                    ) {
                        editText.post {
                            activeSession._inputConnection =
                                    editText.onCreateInputConnection(EditorInfo())
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
        )
    }
}
