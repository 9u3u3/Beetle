package com.bsbeetle.bananaspaghetti.utils.fileHandling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.data.FileData
import com.bsbeetle.bananaspaghetti.data.OpenedFiles
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionFactory
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

@Composable
fun fileLauncher(context: Context, navController: NavController): ActivityResultLauncher<Intent> {

        val fileViewModel: FileViewModel = LocalFileViewModel.current
        val editorViewModel: EditorViewModel = LocalEditorViewModel.current
        val bufferViewModel = com.bsbeetle.bananaspaghetti.LocalBufferViewModel.current

        return rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                { result -> // onResult only executes after the Result received
                        if (result.resultCode == Activity.RESULT_OK) {
                                val uri: Uri? = result.data?.data
                                if (uri != null) {
                                        val flags =
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        context.contentResolver.takePersistableUriPermission(
                                                uri,
                                                flags
                                        )
                                        // Checking if selected URI is a file or a directory
                                        if (DocumentsContract.isDocumentUri(context, uri)) {
                                                OpenedFiles.files.add(
                                                        FileData(
                                                                getFileName(context, uri)
                                                                        .substringAfter("."),
                                                                uri
                                                        )
                                                )

                                                fileViewModel.selectedUri = uri
                                                // Clear folder selection when opening a single file
                                                // so sidebar shows "No folder selected"
                                                fileViewModel.selectedFolderUri = null

                                                // Adding to the newly created session here
                                                // Always creates EditorSession - Notebook is a
                                                // separate session type
                                                val fileName = getFileName(context, uri)
                                                val session =
                                                        SessionFactory.createSession(
                                                                context.applicationContext,
                                                                fileName,
                                                                uri.toString()
                                                        )

                                                // Switch buffer context to this new session
                                                // immediately
                                                // This ensures file loading happens in the correct
                                                // session context
                                                session.id?.let { id ->
                                                        bufferViewModel.setSessionId(id)
                                                }

                                                // Ensure view is created before accessing it
                                                val view = session.getOrInitView()
                                                editorViewModel.setEditorWebView(view)

                                                navController.navigate(Screen.OpenFile.route)
                                        } else {
                                                fileViewModel.selectedFolderUri = uri

                                                // Adding to the newly created session here
                                                val folderDoc =
                                                        androidx.documentfile.provider.DocumentFile
                                                                .fromTreeUri(context, uri)
                                                val session =
                                                        EditorSession(
                                                                context.applicationContext,
                                                                folderDoc?.name ?: "Project"
                                                        )
                                                session.projectRoot = folderDoc

                                                // Switch buffer context to this new session
                                                session.id?.let { id ->
                                                        bufferViewModel.setSessionId(id)
                                                }

                                                // Ensure view is created before accessing it
                                                val view = session.getOrInitView()
                                                editorViewModel.setEditorWebView(view)

                                                navController.navigate(Screen.OpenFolder.route)
                                        }
                                }
                        }
                }
        )
}
