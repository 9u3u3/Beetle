package com.bsbeetle.bananaspaghetti.ui.cmeditor

// Used for both terminal and the code editor

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun Editor(modifier: Modifier = Modifier, webView: CustomWebView) {

    AndroidView(
        factory = { ctx ->
            // Remove from existing parent if any (prevents crash)
            (webView.parent as? ViewGroup)?.removeView(webView)
            
            webView.apply {
                // Set touch listener to handle focus
                setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        // Set this session as active for keyboard input
                        session?.let { sess ->
                            val index = SessionList.sessions.indexOf(sess)
                            if (index >= 0) {
                                SessionList.activeSessionIndex = index
                            }
                        }
                        // Request focus and ensure input connection
                        view.requestFocus()
                        // Focus the editor in JS
                        evaluateJavascript("if (window.editor) { window.editor.focus(); }", null)
                    }
                    false // Let the WebView handle the touch event normally
                }
            }
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