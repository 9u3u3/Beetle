package com.bsbeetle.bananaspaghetti.ui.cmeditor

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bsbeetle.bananaspaghetti.viewmodel.EditorAction
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Editor(
    modifier: Modifier = Modifier,
    viewModel: KeyboardViewModel,
    webView: WebView
) {

    // The LaunchedEffect uses webview variable to listen for EditorAction events
    LaunchedEffect(Unit) {
        viewModel.editorAction.collect { action ->
            when (action) {
                is EditorAction.InsertText -> {
                    val escapedText = action.text.replace("'", "\\'")
                    webView.evaluateJavascript(
                        "window.insertText('$escapedText');",
                        null
                    )
                }
                is EditorAction.Backspace -> {
                    webView.evaluateJavascript(
                        "window.performBackspace();",
                        null
                    )
                }
                is EditorAction.Enter -> {
                    webView.evaluateJavascript(
                        "window.performEnter();",
                        null
                    )
                }
            }
        }
    }

    // The AndroidView factory now just provides the instance webview
    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}