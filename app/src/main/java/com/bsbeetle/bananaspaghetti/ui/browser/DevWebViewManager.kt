package com.bsbeetle.bananaspaghetti.ui.browser


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

object DevWebViewManager {

    fun createWebView(
        context: Context,
        onPageFinished: () -> Unit,
        onProgressChanged: (Int) -> Unit,
        onFrameworkDetected: (String, String) -> Unit,
        onNetworkRequest: () -> Unit,
        onConsoleLog: () -> Unit,
        onUrlChanged: (String) -> Unit
    ): WebView {
        return try {
            WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                setRenderPriority(WebSettings.RenderPriority.HIGH)
                cacheMode = WebSettings.LOAD_DEFAULT

                useWideViewPort = true
                loadWithOverviewMode = true

                userAgentString =
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36"
            }

            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true

            // Enable debugging
            WebView.setWebContentsDebuggingEnabled(true)

            // Add JavaScript Bridge
            addJavascriptInterface(
                DevBridge(onFrameworkDetected, onNetworkRequest, onConsoleLog),
                "DevBridge"
            )

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let { onUrlChanged(it) }
                    view?.evaluateJavascript(DevToolsInjector.EARLY_INJECTION, null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let { onUrlChanged(it) }
                    view?.evaluateJavascript(DevToolsInjector.LATE_INJECTION, null)
                    onPageFinished()
                }

                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    url?.let { onUrlChanged(it) }
                }

            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onProgressChanged(newProgress)
                }

                override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                    message?.let {
                        val logMessage =
                            "[${it.messageLevel()}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})"
                        when (it.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR -> Log.e("WebConsole", logMessage)
                            ConsoleMessage.MessageLevel.WARNING -> Log.w("WebConsole", logMessage)
                            ConsoleMessage.MessageLevel.LOG -> Log.i("WebConsole", logMessage)
                            else -> Log.d("WebConsole", logMessage)
                        }
                    }
                    return true
                }
            }
        }
        }catch (e: Exception) {
            Log.e("DevWebView", "Failed to create WebView: ${e.message}", e)
            throw e
        }
    }
}