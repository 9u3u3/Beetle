package com.bsbeetle.bananaspaghetti.directGit.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bsbeetle.bananaspaghetti.directGit.ui.GitTheme
import com.bsbeetle.bananaspaghetti.directGit.ui.MergeEditorSession

/**
 * Merge Editor Dialog with WebView-based inline conflict resolution.
 */
@Composable
fun MergeEditorDialog(
    filePath: String,
    content: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Create merge editor session
    val mergeSession = remember(filePath) {
        MergeEditorSession(
            context = context,
            onSave = { path, mergedContent -> onSave(path, mergedContent) },
            onClose = { onDismiss() }
        )
    }

    // Set content when session is ready
    LaunchedEffect(content, filePath) {
        mergeSession.setMergeContent(content, filePath)
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose { mergeSession.destroy() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
            mergeSession.view?.let { webView ->
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GitTheme.AccentBlue)
            }
        }
    }
}
