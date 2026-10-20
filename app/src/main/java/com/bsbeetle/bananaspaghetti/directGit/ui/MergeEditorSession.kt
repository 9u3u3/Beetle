package com.bsbeetle.bananaspaghetti.directGit.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebView-based session for resolving merge conflicts. Shows inline conflict markers with
 * per-conflict resolution buttons.
 */
class MergeEditorSession(
        private val context: Context,
        private val onSave: (filePath: String, content: String) -> Unit = { _, _ -> },
        private val onClose: () -> Unit = {}
) {
    private var webView: WebView? = null
    private var isReady = false
    private var pendingContent: Pair<String, String>? = null // content, path

    val view: WebView?
        get() = webView

    init {
        createView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createView() {
        Log.d("MergeEditorSession", "🚀 Creating WebView for merge editor")
        webView =
                WebView(context).apply {
                    // Set layout params to fill available space
                    layoutParams =
                            android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )

                    WebView.setWebContentsDebuggingEnabled(true)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        @Suppress("DEPRECATION") allowUniversalAccessFromFileURLs = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        setSupportZoom(false)
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    addJavascriptInterface(MergeJsInterface(), "AndroidMerge")

                    // Ensure WebView has visible background
                    setBackgroundColor(android.graphics.Color.parseColor("#1e1e1e"))
                    setInitialScale(100)

                    // Add WebChromeClient to capture console.log
                    webChromeClient =
                            object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(
                                        consoleMessage: android.webkit.ConsoleMessage?
                                ): Boolean {
                                    consoleMessage?.let {
                                        val level =
                                                when (it.messageLevel()) {
                                                    android.webkit.ConsoleMessage.MessageLevel
                                                            .ERROR -> "E"
                                                    android.webkit.ConsoleMessage.MessageLevel
                                                            .WARNING -> "W"
                                                    else -> "D"
                                                }
                                        Log.println(
                                                when (level) {
                                                    "E" -> Log.ERROR
                                                    "W" -> Log.WARN
                                                    else -> Log.DEBUG
                                                },
                                                "MergeEditorJS",
                                                "[${it.sourceId()}:${it.lineNumber()}] ${it.message()}"
                                        )
                                    }
                                    return true
                                }
                            }

                    webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isReady = true
                                    Log.d(
                                            "MergeEditorSession",
                                            "✅ Page loaded: $url, ready for content"
                                    )

                                    // Apply pending content if any
                                    pendingContent?.let { (content, path) ->
                                        Log.d(
                                                "MergeEditorSession",
                                                "📝 Applying pending content for: $path (${content.length} chars)"
                                        )
                                        injectContent(content, path)
                                        pendingContent = null
                                    }
                                }

                                override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                ) {
                                    Log.e(
                                            "MergeEditorSession",
                                            "❌ WebView error: $errorCode - $description at $failingUrl"
                                    )
                                }
                            }

                    Log.d(
                            "MergeEditorSession",
                            "📂 Loading: file:///android_asset/git/merge-editor.html"
                    )
                    loadUrl("file:///android_asset/git/merge-editor.html")
                }
    }

    /**
     * Set merge content with conflict markers
     * @param conflictContent Full file content with conflict markers (<<<<<<, =======, >>>>>>>)
     * @param filePath File path
     */
    fun setMergeContent(conflictContent: String, filePath: String) {
        Log.d(
                "MergeEditorSession",
                "📥 setMergeContent called: path=$filePath, isReady=$isReady, content=${conflictContent.take(100)}..."
        )

        if (!isReady) {
            Log.d("MergeEditorSession", "⏳ Not ready yet, queuing content")
            pendingContent = Pair(conflictContent, filePath)
            return
        }

        injectContent(conflictContent, filePath)
    }

    private fun injectContent(conflictContent: String, filePath: String) {
        val escapedContent = escapeForJs(conflictContent)
        val escapedPath = escapeForJs(filePath)

        val js =
                "if(window.setMergeContent) { console.log('Calling setMergeContent'); window.setMergeContent($escapedContent, $escapedPath); } else { console.error('setMergeContent not found!'); }"
        Log.d("MergeEditorSession", "💉 Injecting JS: ${js.take(200)}...")

        webView?.evaluateJavascript(js) { result ->
            Log.d("MergeEditorSession", "📤 JS result: $result")
        }
    }

    /** Check if there are remaining unresolved conflicts */
    fun hasConflicts(callback: (Boolean) -> Unit) {
        webView?.evaluateJavascript("window.hasConflicts ? window.hasConflicts() : false") { result
            ->
            callback(result == "true")
        }
    }

    /** Get the current merged content */
    fun getMergedContent(callback: (String) -> Unit) {
        webView?.evaluateJavascript("window.getMergedContent ? window.getMergedContent() : ''") {
                result ->
            val content =
                    com.bsbeetle.bananaspaghetti.utils.webViewHandling.JsResult.decodeString(result)
                            ?: ""
            callback(content)
        }
    }

    private fun escapeForJs(text: String): String {
        return org.json.JSONObject.quote(text)
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }

    private inner class MergeJsInterface {
        @JavascriptInterface
        fun onSave(filePath: String, content: String) {
            Log.d("MergeEditorSession", "Save requested for: $filePath")
            webView?.post { this@MergeEditorSession.onSave(filePath, content) }
        }

        @JavascriptInterface
        fun onClose() {
            webView?.post { this@MergeEditorSession.onClose() }
        }
    }
}
