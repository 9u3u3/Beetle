package com.bsbeetle.bananaspaghetti.utils.fileHandling

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

object FileOperations {

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
                fileViewModel.refreshFileTree() // Refresh tree immediately
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
                fileViewModel.refreshFileTree() // Refresh tree immediately
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

        // Try DocumentFile.renameTo first (works for TreeDocumentFile)
        // SingleDocumentFile throws UnsupportedOperationException, so we catch it
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

        // Fallback to DocumentsContract.renameDocument (works for SingleDocumentFile)
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
                Toast.makeText(context, "Failed to rename file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FileOperations", "Error renaming file with DocumentsContract", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
