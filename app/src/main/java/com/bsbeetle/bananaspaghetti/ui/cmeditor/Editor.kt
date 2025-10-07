package com.bsbeetle.bananaspaghetti.ui.cmeditor

// Used for both terminal and the code editor

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
//    LaunchedEffect(Unit) {
//        viewModel.editorAction.collect { action ->
//            when (action) {
//                is EditorAction.InsertText -> {
//                    val escapedText = action.text.replace("'", "\\'")
//                    webView.evaluateJavascript(
//                        "window.insertText('$escapedText');",
//                        null
//                    )
//                }
//                is EditorAction.Backspace -> {
//                    webView.evaluateJavascript(
//                        "window.performBackspace();",
//                        null
//                    )
//                }
//                is EditorAction.Enter -> {
//                    webView.evaluateJavascript(
//                        "window.performEnter();",
//                        null
//                    )
//                }
//            }
//        }
//    }
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
                is EditorAction.Copy -> {
                    webView.evaluateJavascript(
                        "window.performCopy();",
                        null
                    )
                }
                is EditorAction.Paste -> {
                    webView.evaluateJavascript(
                        "window.performPaste();",
                        null
                    )
                }
                is EditorAction.Cut -> {
                    webView.evaluateJavascript(
                        "window.performCut();",
                        null
                    )
                }
                is EditorAction.Undo -> {
                    webView.evaluateJavascript(
                        "window.performUndo();",
                        null
                    )
                }
                is EditorAction.Redo -> {
                    webView.evaluateJavascript(
                        "window.performRedo();",
                        null
                    )
                }
                is EditorAction.SelectAll -> {
                    webView.evaluateJavascript(
                        "window.performSelectAll();",
                        null
                    )
                }
                is EditorAction.Find -> {
                    webView.evaluateJavascript(
                        "window.performFind();",
                        null
                    )
                }
                is EditorAction.Save -> {
                    webView.evaluateJavascript(
                        "window.performSave();",
                        null
                    )
                }
                is EditorAction.CtrlC -> {
                    webView.evaluateJavascript("window.performCtrlC();", null)
                }
                is EditorAction.CtrlD -> {
                    webView.evaluateJavascript("window.performCTRLD();",null)
                }
                is EditorAction.CtrlZ -> {
                    webView.evaluateJavascript("window.performCtrlZ();", null)
                }
                is EditorAction.Escape -> {
                    webView.evaluateJavascript("window.performEscape();", null)
                }
            }
        }
    }
    // The AndroidView factory now just provides the instance webview
    AndroidView(
        factory = { webView },
        modifier = modifier,
        // ADD THIS: Force a resize when the view gets laid out
        onReset = { view ->
            // Trigger a resize after the view is laid out
            view.post {
                view.evaluateJavascript("if(window.fitTerminal) fitTerminal();", null)
            }
        },
        update = { view ->
            // Trigger resize when size changes
            view.post {
                view.evaluateJavascript("if(window.fitTerminal) fitTerminal();", null)
            }
        }
    )
}