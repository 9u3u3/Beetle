package com.bsbeetle.bananaspaghetti.ui.cmeditor

// Used for both terminal and the code editor

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun Editor(modifier: Modifier = Modifier, webView: CustomWebView) {

    // Set active session when this editor view is displayed
    DisposableEffect(webView) {
        webView.session?.let { sess ->
            val index = SessionList.sessions.indexOf(sess)
            if (index >= 0) {
                SessionList.activeSessionIndex = index
            }
        }
        onDispose { }
    }

    AndroidView(
        factory = { ctx ->
            // Remove from existing parent if any (prevents crash)
            (webView.parent as? ViewGroup)?.removeView(webView)
            
            // Request initial focus
            webView.post {
                webView.requestFocus()
                webView.evaluateJavascript("""
                    if (window.editor) { 
                        window.editor.focus(); 
                    } else if (window.term) {
                        window.term.focus();
                    }
                """.trimIndent(), null)
            }
            
            webView
        },
        modifier = modifier,
        onReset = { view ->
            view.post {
                view.evaluateJavascript("if(window.fitTerminal) fitTerminal();", null)
            }
        },
        update = { view ->
            view.post {
                view.evaluateJavascript("if(window.fitTerminal) fitTerminal();", null)
            }
        }
    )
}