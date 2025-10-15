package com.bsbeetle.bananaspaghetti.utils

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
import com.bsbeetle.bananaspaghetti.LocalFileViewModel
import com.bsbeetle.bananaspaghetti.data.FileData
import com.bsbeetle.bananaspaghetti.data.OpenedFiles
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel

@Composable
fun fileLauncher(context: Context, navController: NavController): ActivityResultLauncher<Intent> {
    val fileViewModel: FileViewModel = LocalFileViewModel.current
    return rememberLauncherForActivityResult (
        ActivityResultContracts.StartActivityForResult(),
        { result -> // onResult only executes after the Result received
            if(result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if(uri != null) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
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
                        navController.navigate(Screen.OpenFile.route)
                    } else {
                        fileViewModel.selectedFolderUri = uri
                        navController.navigate(Screen.OpenFolder.route)
                    }
                }
            }
        }
    )
}