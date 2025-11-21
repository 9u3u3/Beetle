package com.bsbeetle.bananaspaghetti.ui.cmeditor

// Used for both terminal and the code editor

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.bsbeetle.bananaspaghetti.LocalEditorViewModel
import com.bsbeetle.bananaspaghetti.LocalKeyboardViewModel
import com.bsbeetle.bananaspaghetti.data.tenebrioObj
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import com.bsbeetle.bananaspaghetti.viewmodel.EditorAction
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import java.lang.NullPointerException
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Editor(modifier: Modifier = Modifier, webView: CustomWebView) {

    val keyboardViewModel: KeyboardViewModel = LocalKeyboardViewModel.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT


    LaunchedEffect(Unit) {
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (isPortrait) {
                    // In portrait, focus the CodeMirror editor
                    view?.evaluateJavascript(
                        "if (window.editor) { window.editor.focus(); }",
                        null
                    )
                }

                try {
                    tenebrioObj.tenebrio!!.init()
                } catch (e: NullPointerException) {
                    Log.e("Editor", "Null pointer exception")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        keyboardViewModel.editorAction.collect { action ->
            when (action) {
                is EditorAction.InsertText -> {
                    val escapedText = action.text.replace("'", "\\'")
                    webView.evaluateJavascript("window.insertText('$escapedText');", null)
                }
                is EditorAction.Backspace -> { webView.evaluateJavascript("window.performBackspace();", null) }
                is EditorAction.Enter -> { webView.evaluateJavascript("window.performEnter();", null) }
                is EditorAction.Copy -> { webView.evaluateJavascript("window.performCopy();", null) }
                is EditorAction.Paste -> { webView.evaluateJavascript("window.performPaste();", null) }
                is EditorAction.Cut -> { webView.evaluateJavascript("window.performCut();", null) }
                is EditorAction.Undo -> { webView.evaluateJavascript("window.performUndo();", null) }
                is EditorAction.Redo -> { webView.evaluateJavascript("window.performRedo();", null) }
                is EditorAction.SelectAll -> { webView.evaluateJavascript("window.performSelectAll();", null) }
                is EditorAction.Find -> { webView.evaluateJavascript("window.performFind();", null) }
                is EditorAction.Save -> { webView.evaluateJavascript("window.performSave();", null) }
                is EditorAction.CtrlC -> { webView.evaluateJavascript("window.performCtrlC();", null) }
                is EditorAction.CtrlD -> { webView.evaluateJavascript("window.performCTRLD();", null) }
                is EditorAction.CtrlZ -> { webView.evaluateJavascript("window.performCtrlZ();", null) }
                is EditorAction.Escape -> { webView.evaluateJavascript("window.performEscape();", null) }
            }
        }
    }

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