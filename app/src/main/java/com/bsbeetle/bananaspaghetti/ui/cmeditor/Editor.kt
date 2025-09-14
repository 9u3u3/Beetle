package com.bsbeetle.bananaspaghetti.ui.cmeditor

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import com.bsbeetle.bananaspaghetti.data.OpenedFiles
import com.bsbeetle.bananaspaghetti.utils.getFileName
import com.bsbeetle.bananaspaghetti.utils.loadContentWhenReady
import com.bsbeetle.bananaspaghetti.utils.readFromFile
import com.bsbeetle.bananaspaghetti.viewmodel.EditorAction
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel
import org.json.JSONObject
import kotlin.math.log

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