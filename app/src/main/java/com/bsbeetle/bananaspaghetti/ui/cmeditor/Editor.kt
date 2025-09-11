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
    fileViewModel: FileViewModel
) {


   // to get the local context
    val context = LocalContext.current

    if(fileViewModel.selectedUri == null) {
        Log.wtf("NULLURI", "URI IS NULL")
    } else {
        println("EDITOR: ${getFileName(context, fileViewModel.selectedUri)}")
    }

    // variable to make the Android view mode readable does the same work as before it AndroidView
    //...is not cluttered
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

            // Load the content only when the page is loaded
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    fileViewModel.selectedUri?.let { uri ->
                        val fileContent = readFromFile(context, uri)
                        loadContentWhenReady(view, fileContent)
                    }
                }
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

    // 3. The AndroidView factory now just provides the instance webview
    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}

fun loadContentWhenReady(webView: WebView?, content: String) {
    val jsCheck = "window.isEditorReady === true;"
    webView?.evaluateJavascript(jsCheck) { result ->
        if (result == "true") {
            val jsCommand = """
                window.editor.dispatch({
                    changes: { from: 0, to: window.editor.state.doc.length, insert: ${JSONObject.quote(content)} }
                });
            """.trimIndent()
            webView.evaluateJavascript(jsCommand, null)
        } else {
            // Wait a bit and try again
            Handler(Looper.getMainLooper()).postDelayed({
                loadContentWhenReady(webView, content)
            }, 50)
        }
    }
}