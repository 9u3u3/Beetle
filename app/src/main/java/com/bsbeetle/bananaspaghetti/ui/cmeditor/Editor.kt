package com.bsbeetle.bananaspaghetti.ui.cmeditor

// Used for both terminal and the code editor

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Editor(modifier: Modifier = Modifier, webView: CustomWebView) {

    AndroidView(
        factory = { webView },
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