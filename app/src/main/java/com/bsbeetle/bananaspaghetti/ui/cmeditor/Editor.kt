package com.bsbeetle.bananaspaghetti.ui.cmeditor

// Used for both terminal and the code editor

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.NotebookSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun Editor(modifier: Modifier = Modifier, webView: CustomWebView) {

    // Get isReady state from session (works for both EditorSession and NotebookSession)
    val session = webView.session
    val isEditorReady by when (session) {
        is EditorSession -> session.isReady.collectAsStateWithLifecycle()
        is NotebookSession -> session.isReady.collectAsStateWithLifecycle()
        else -> kotlinx.coroutines.flow.MutableStateFlow(true).collectAsStateWithLifecycle()
    }
    
    val isNotebookSession = session is NotebookSession

    // Set active session when this editor view is displayed
    DisposableEffect(webView) {
        webView.session?.let { sess ->
            val index = SessionList.sessions.indexOf(sess)
            if (index >= 0) {
                SessionList.activeSessionIndex = index
            }
        }
        onDispose {}
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Loading overlay - shown while CM6 is initializing
        if (!isEditorReady) {
            Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
            ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF007ACC),
                            strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                            text = "Initializing editor...",
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp
                    )
                }
            }
        }

        // Editor WebView - invisible until ready
        AndroidView(
                factory = { ctx ->
                    // Remove from existing parent if any (prevents crash)
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    
                    // Ensure WebView fills the parent completely
                    webView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Request initial focus
                    webView.post {
                        webView.requestFocus()
                        webView.evaluateJavascript(
                                """
                    if (window.editor) { 
                        window.editor.focus(); 
                    } else if (window.term) {
                        window.term.focus();
                    }
                """.trimIndent(),
                                null
                        )
                    }

                    webView
                },
                modifier = Modifier.fillMaxSize().alpha(if (isEditorReady) 1f else 0f),
                onReset = { view ->
                    view.post {
                        view.evaluateJavascript("if(window.fitTerminal) fitTerminal();", null)
                    }
                },
                update = { view ->
                    view.post {
                        view.evaluateJavascript("if(window.fitTerminal) fitTerminal();", null)
                    }
                    // Ensure visibility matches ready state
                    if (isEditorReady) {
                        view.visibility = android.view.View.VISIBLE
                        view.alpha = 1f
                    }
                }
        )
    }
}
