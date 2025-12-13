package com.bsbeetle.bananaspaghetti.ui.home

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.bsbeetle.bananaspaghetti.R
import com.bsbeetle.bananaspaghetti.ui.navigation.Screen
import com.bsbeetle.bananaspaghetti.ui.theme.born2bsporty
import com.bsbeetle.bananaspaghetti.utils.fileHandling.RecentFile

@Composable
fun RecentFileItem(
    file: RecentFile,
    navController: NavController,
    fileViewModel: com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
) {
    val context = LocalContext.current
    
    // ImageLoader for SVGs
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    val editorViewModel = com.bsbeetle.bananaspaghetti.LocalEditorViewModel.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val uri = Uri.parse(file.uri)
                
                // Create new session for the file to prevent NPE in InitialEditorUI
                val session = com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession(
                    context, 
                    file.name
                )
                
                // Add to session list and set active
                com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList.sessions.add(session)
                com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList.activeSessionIndex = 
                    com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList.sessions.lastIndex
                
                // Initialize EditorViewModel with the new session's WebView
                // CRITICAL: Ensure view is created immediately
                val view = session.getOrInitView()
                editorViewModel.setEditorWebView(view)
                
                fileViewModel.selectedUri = uri
                fileViewModel.currentFilePath = file.uri
                navController.navigate(Screen.OpenFolder.route)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on extension
        val ext = file.name.substringAfterLast(".", "").lowercase()
        val iconName = when(ext) {
            "kt" -> "kotlin.svg"
            "py" -> "python.svg"
            "js" -> "javascript.svg"
            "cpp", "c", "h" -> "cpp.svg"
            "html" -> "html.svg"
            "css" -> "css.svg"
            "java" -> "java.svg"
            "xml" -> "xml.svg"
            "json" -> "json.svg"
            "md" -> "markdown.svg"
            "txt" -> "text.svg"
            else -> "default_file.svg"
        }
        
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/file_icons/$iconName")
                .decoderFactory(SvgDecoder.Factory())
                .error(R.drawable.file_open_24dp_e3e3e3_fill0_wght400_grad0_opsz24) // Fallback to valid drawable
                .build(),
            contentDescription = null,
            imageLoader = imageLoader,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = file.name,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = born2bsporty
            )
            // URI display removed as requested
        }
    }
}
