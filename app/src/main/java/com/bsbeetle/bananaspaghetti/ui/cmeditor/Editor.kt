package com.bsbeetle.bananaspaghetti.ui.cmeditor

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bsbeetle.bananaspaghetti.viewmodel.EditorAction
import com.bsbeetle.bananaspaghetti.viewmodel.KeyboardViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Editor(modifier: Modifier = Modifier, viewModel: KeyboardViewModel) {
   // to get the local context
    val context = LocalContext.current

    // variable to make the Android view mode readable does the same work as before ut AndroidView is not cluttlered
    val webView = remember {
        WebView(context).apply {
            // Enable debugging (use chrome://inspect)
            WebView.setWebContentsDebuggingEnabled(true)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
            }
            loadUrl("file:///android_asset/editor/index.html")
        }
    }

    // 2. The LaunchedEffect uses webview variable to listen for EditorAction events
    LaunchedEffect(Unit) {
        viewModel.editorAction.collect { action ->
            when (action) {
                is EditorAction.InsertText -> {
                    val escapedText = action.text.replace("'", "\\'")
                    webView.evaluateJavascript("window.insertText('$escapedText');", null)
                }
                is EditorAction.Backspace -> {
                    webView.evaluateJavascript("window.performBackspace();", null)
                }

                is EditorAction.Enter -> {
                    webView.evaluateJavascript("window.performEnter();", null)
                }
            }
        }
    }

    // 3. The AndroidView factory now just provides the instance webview
    AndroidView(
        factory = { webView },
        modifier = modifier
    )


}