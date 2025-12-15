package com.bsbeetle.bananaspaghetti.directGit.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebView-based session for displaying file diffs. Shows current (HEAD) on top and incoming changes
 * on bottom in a stacked layout.
 */
class DiffViewerSession(private val context: Context, private val onClose: () -> Unit = {}) {
    private var webView: WebView? = null
    private var isReady = false
    private var pendingDiff: Triple<String, String, String>? = null // current, incoming, path

    val view: WebView?
        get() = webView

    init {
        createView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createView() {
        webView =
                WebView(context).apply {
                    WebView.setWebContentsDebuggingEnabled(true)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowUniversalAccessFromFileURLs = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        setSupportZoom(false)
                    }

                    addJavascriptInterface(DiffJsInterface(), "AndroidDiff")

                    webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isReady = true
                                    Log.d("DiffViewerSession", "Page loaded, ready for content")

                                    // Apply pending diff if any
                                    pendingDiff?.let { (current, incoming, path) ->
                                        setDiffContent(current, incoming, path)
                                        pendingDiff = null
                                    }
                                }
                            }

                    loadUrl("file:///android_asset/git/diff-viewer.html")
                }
    }

    /**
     * Set diff content to display
     * @param currentContent Content from current (HEAD) version
     * @param incomingContent Content from incoming version
     * @param filePath File path for display
     */
    fun setDiffContent(currentContent: String, incomingContent: String, filePath: String) {
        if (!isReady) {
            pendingDiff = Triple(currentContent, incomingContent, filePath)
            return
        }

        val escapedCurrent = escapeForJs(currentContent)
        val escapedIncoming = escapeForJs(incomingContent)
        val escapedPath = escapeForJs(filePath)

        webView?.evaluateJavascript(
                "if(window.setDiffContent) window.setDiffContent($escapedCurrent, $escapedIncoming, $escapedPath);",
                null
        )
    }

    private fun escapeForJs(text: String): String {
        return org.json.JSONObject.quote(text)
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }

    private inner class DiffJsInterface {
        @JavascriptInterface
        fun onClose() {
            webView?.post { this@DiffViewerSession.onClose() }
        }
    }
}
