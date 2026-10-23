package com.bsbeetle.bananaspaghetti.utils.fileHandling

import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

object FileOperations {

    // Clipboard for copy/cut operations - using mutableStateOf for recomposition
    enum class ClipboardOp {
        NONE,
        COPY,
        CUT
    }
    var clipboardFile: DocumentFile? by mutableStateOf(null)
    var clipboardFiles: List<DocumentFile> by mutableStateOf(emptyList())
    var clipboardOperation: ClipboardOp by mutableStateOf(ClipboardOp.NONE)

    fun createNewFile(
            context: Context,
            folder: DocumentFile?,
            fileName: String,
            onFileOpened: (DocumentFile) -> Unit,
            fileViewModel: FileViewModel
    ) {
        val currentSession =
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as?
                        EditorSession
        val targetFolder = folder ?: currentSession?.projectRoot

        if (targetFolder == null) {
            Toast.makeText(context, "Please select a folder first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mimeType =
                    when {
                        fileName.endsWith(".kt") -> "text/x-kotlin"
                        fileName.endsWith(".java") -> "text/x-java-source"
                        fileName.endsWith(".py") -> "text/x-python"
                        fileName.endsWith(".js") -> "application/javascript"
                        fileName.endsWith(".ts") -> "application/typescript"
                        fileName.endsWith(".html") -> "text/html"
                        fileName.endsWith(".css") -> "text/css"
                        fileName.endsWith(".json") -> "application/json"
                        fileName.endsWith(".xml") -> "text/xml"
                        fileName.endsWith(".md") -> "text/markdown"
                        else -> "text/plain"
                    }

            val newFile = targetFolder.createFile(mimeType, fileName)
            if (newFile != null) {
                onFileOpened(newFile)
                fileViewModel.refreshFileTree()
                Toast.makeText(context, "Created: $fileName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FileOperations", "Error creating file", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun createNewFolder(
            context: Context,
            parentFolder: DocumentFile?,
            folderName: String,
            fileViewModel: FileViewModel
    ) {
        val currentSession =
                SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as?
                        EditorSession
        val targetFolder = parentFolder ?: currentSession?.projectRoot

        if (targetFolder == null) {
            Toast.makeText(context, "Please select a parent folder first", Toast.LENGTH_SHORT)
                    .show()
            return
        }

        try {
            val newFolder = targetFolder.createDirectory(folderName)
            if (newFolder != null) {
                fileViewModel.refreshFileTree()
                Toast.makeText(context, "Created folder: $folderName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to create folder", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FileOperations", "Error creating folder", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun renameFile(
            context: Context,
            file: DocumentFile,
            newName: String,
            fileViewModel: FileViewModel
    ) {
        Log.d(
                "FileOperations",
                "renameFile called: file=${file.name}, uri=${file.uri}, newName=$newName"
        )

        var renamed = false
        try {
            Log.d("FileOperations", "Trying DocumentFile.renameTo...")
            renamed = file.renameTo(newName)
            Log.d("FileOperations", "DocumentFile.renameTo result: $renamed")
        } catch (e: UnsupportedOperationException) {
            Log.d(
                    "FileOperations",
                    "renameTo threw UnsupportedOperationException, will try DocumentsContract"
            )
        }

        if (renamed) {
            fileViewModel.refreshFileTree()
            Toast.makeText(context, "Renamed to: $newName", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Log.d("FileOperations", "Trying DocumentsContract.renameDocument...")
            val uri = file.uri
            val newUri =
                    android.provider.DocumentsContract.renameDocument(
                            context.contentResolver,
                            uri,
                            newName
                    )
            Log.d("FileOperations", "DocumentsContract.renameDocument result: $newUri")
            if (newUri != null) {
                fileViewModel.refreshFileTree()
                Toast.makeText(context, "Renamed to: $newName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to rename", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FileOperations", "Error renaming with DocumentsContract", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteFile(context: Context, file: DocumentFile, fileViewModel: FileViewModel, showToast: Boolean = true): Boolean {
        val name = file.name ?: "item"
        Log.d("FileOperations", "deleteFile called: $name")

        try {
            val deleted = file.delete()
            if (deleted) {
                fileViewModel.refreshFileTree()
                if (showToast) Toast.makeText(context, "Deleted: $name", Toast.LENGTH_SHORT).show()
                return true
            } else {
                // Try DocumentsContract as fallback
                try {
                    val success =
                            android.provider.DocumentsContract.deleteDocument(
                                    context.contentResolver,
                                    file.uri
                            )
                    if (success) {
                        fileViewModel.refreshFileTree()
                        if (showToast) Toast.makeText(context, "Deleted: $name", Toast.LENGTH_SHORT).show()
                        return true
                    } else {
                        if (showToast) Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                        return false
                    }
                } catch (e: Exception) {
                    Log.e("FileOperations", "Error deleting with DocumentsContract", e)
                    if (showToast) Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
        } catch (e: Exception) {
            Log.e("FileOperations", "Error deleting file", e)
            if (showToast) Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    fun copyToClipboard(file: DocumentFile, context: Context) {
        clipboardFile = file
        clipboardOperation = ClipboardOp.COPY
        Log.d("FileOperations", "Copied to clipboard: ${file.name}")
        Toast.makeText(context, "Copied: ${file.name}", Toast.LENGTH_SHORT).show()
    }

    fun cutToClipboard(file: DocumentFile, context: Context) {
        clipboardFile = file
        clipboardOperation = ClipboardOp.CUT
        Log.d("FileOperations", "Cut to clipboard: ${file.name}")
        Toast.makeText(context, "Cut: ${file.name}", Toast.LENGTH_SHORT).show()
    }
    
    // Bulk clipboard operations for multi-select
    fun copyMultipleToClipboard(files: List<DocumentFile>, context: Context) {
        clipboardFiles = files
        clipboardFile = files.firstOrNull()
        clipboardOperation = ClipboardOp.COPY
        Log.d("FileOperations", "Copied ${files.size} items to clipboard")
        Toast.makeText(context, "Copied ${files.size} items", Toast.LENGTH_SHORT).show()
    }
    
    fun cutMultipleToClipboard(files: List<DocumentFile>, context: Context) {
        clipboardFiles = files
        clipboardFile = files.firstOrNull()
        clipboardOperation = ClipboardOp.CUT
        Log.d("FileOperations", "Cut ${files.size} items to clipboard")
        Toast.makeText(context, "Cut ${files.size} items", Toast.LENGTH_SHORT).show()
    }

    fun clearClipboard() {
        clipboardFile = null
        clipboardFiles = emptyList()
        clipboardOperation = ClipboardOp.NONE
    }

    fun hasClipboardContent(): Boolean = clipboardFile != null || clipboardFiles.isNotEmpty()

    fun paste(context: Context, destFolder: DocumentFile, fileViewModel: FileViewModel) {
        // Handle multiple files if present
        val filesToPaste = if (clipboardFiles.isNotEmpty()) {
            clipboardFiles
        } else if (clipboardFile != null) {
            listOf(clipboardFile!!)
        } else {
            Toast.makeText(context, "Nothing to paste", Toast.LENGTH_SHORT).show()
            return
        }
        
        val operation = clipboardOperation
        val count = filesToPaste.size

        Log.d(
                "FileOperations",
                "paste called: ${count} items, dest=${destFolder.name}, op=$operation"
        )

        try {
            var successCount = 0
            val destId = runCatching { DocumentsContract.getDocumentId(destFolder.uri) }.getOrNull()
            filesToPaste.forEach { source ->
                val sourceId =
                        runCatching { DocumentsContract.getDocumentId(source.uri) }.getOrNull()
                if (source.isDirectory && sourceId != null && destId != null &&
                                DocumentIds.isSameOrDescendant(sourceId, destId)
                ) {
                    // Copying a folder into itself would recurse without end
                    Toast.makeText(
                                    context,
                                    "Can't paste '${source.name}' into itself",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                    return@forEach
                }
                if (operation == ClipboardOp.CUT && sourceId != null && destId != null &&
                                DocumentIds.isDirectChild(destId, sourceId)
                ) {
                    // Moving an item into the folder it is already in changes nothing
                    successCount++
                    return@forEach
                }
                try {
                    if (source.isDirectory) {
                        copyFolderRecursive(context, source, destFolder)
                    } else {
                        copyFile(context, source, destFolder)
                    }
                    
                    if (operation == ClipboardOp.CUT) {
                        // Delete source after copy for move operation
                        source.delete()
                    }
                    successCount++
                } catch (e: Exception) {
                    Log.e("FileOperations", "Error pasting ${source.name}", e)
                }
            }

            fileViewModel.refreshFileTree()

            if (operation == ClipboardOp.CUT) {
                clearClipboard()
                Toast.makeText(context, "Moved $successCount items", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Pasted $successCount items", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FileOperations", "Error during paste", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyFile(context: Context, source: DocumentFile, destFolder: DocumentFile) {
        val fileName = source.name ?: return
        val mimeType = source.type ?: "application/octet-stream"

        val destFile =
                destFolder.createFile(mimeType, fileName)
                        ?: throw Exception("Failed to create destination file")

        context.contentResolver.openInputStream(source.uri)?.use { input ->
            context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun copyFolderRecursive(
            context: Context,
            source: DocumentFile,
            destFolder: DocumentFile
    ) {
        val folderName = source.name ?: return
        val newFolder =
                destFolder.createDirectory(folderName)
                        ?: throw Exception("Failed to create destination folder")

        source.listFiles().forEach { child ->
            if (child.isDirectory) {
                copyFolderRecursive(context, child, newFolder)
            } else {
                copyFile(context, child, newFolder)
            }
        }
    }
}
