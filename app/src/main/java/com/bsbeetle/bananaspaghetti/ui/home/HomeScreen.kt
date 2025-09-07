package com.bsbeetle.bananaspaghetti.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.data.FileData
import com.bsbeetle.bananaspaghetti.data.OpenedFiles
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.utils.getFileName
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@SuppressLint("RestrictedApi")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    keyboardViewModel: KeyboardViewModel = viewModel()
) {
    val uiState = keyboardViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // File Picker
    // Will make it generic later
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        { result -> // onResult only executes after the Result received
            if(result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data

                if(uri != null) {

                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    OpenedFiles.files.add(
                        FileData(
                            getFileName(context, uri)
                                .substringAfter("."),
                            uri
                        )
                    )
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate(Screen.OpenFile.route + "/$encodedUri")
                }
            }
        }
    )

    Box(
        modifier = modifier.background(color = Color(0xFF181818)),
        contentAlignment = Alignment.Center
    ) {
        // Centered content
        Box(
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Column() {
                // Heading
                Text(
                    text = "Banana Spaghetti",
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Menu Items
                // New file
                MenuItem(
                    iconRes = R.drawable.note_add_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                    label = "New File",
                    onClick = { navController.navigate(Screen.NewFile.route) },
                    keyboardViewModel = keyboardViewModel
                )

                // Open file
                MenuItem(
                    iconRes = R.drawable.file_open_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                    label = "Open File",
                    onClick = {
                        //launcher.launch(arrayOf("*/*")) // Launch the file picker
                        //... Non blocking call, asynchronous
                        //for(element in OpenedFiles.files) println(getFileName(context, element.uri))
                        //println("Hello world")
                        //navController.navigate(Screen.OpenFile.route)
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        launcher.launch(intent)
                    },
                )

                MenuItem(
                    iconRes = R.drawable.folder_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                    label = "Open Folder",
                )

                MenuItem(
                    iconRes = R.drawable.terminal_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                    label = "Open Terminal",
                )
            }
        }
    }
}

@Composable
fun MenuItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit = {},
    keyboardViewModel: KeyboardViewModel? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 6.dp)
            .padding(start = 16.dp)
            .clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(15.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}