package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bsbeetle.bananaspaghetti.ui.browser.DevBridge
import com.bsbeetle.bananaspaghetti.ui.browser.DevToolsInjector
import com.bsbeetle.bananaspaghetti.utils.AppConfig
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.WebViewPool

class BrowserSession(
    context: Context,
    val onPageFinished: () -> Unit = {},
    val onProgressChanged: (Int) -> Unit = {},
    val onFrameworkDetected: (String, String) -> Unit = { _, _ -> },
    val onNetworkRequest: () -> Unit = {},
    val onConsoleLog: () -> Unit = {},
    val onUrlChanged: (String) -> Unit = {},
    name: String = "Browser"
) : Session(context, SessionType.BROWSER, name) {

    init {
        createView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(): CustomWebView {
        if (view != null) return view!!
        view = WebViewPool.get(context.applicationContext).apply {
            session = this@BrowserSession
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
                userAgentString = AppConfig.USER_AGENT
                minimumFontSize = 1
                minimumLogicalFontSize = 1
                textZoom = 100
            }

            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            WebView.setWebContentsDebuggingEnabled(true)

            addJavascriptInterface(
                DevBridge(onFrameworkDetected, onNetworkRequest, onConsoleLog),
                "DevBridge"
            )

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onProgressChanged(newProgress)
                }

                override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                    message?.let {
                        val logMessage = "[${it.messageLevel()}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})"
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

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let { onUrlChanged(it) }
                    view?.evaluateJavascript(DevToolsInjector.EARLY_INJECTION, null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        """
                        (function() {
                            function forceDesktop() {
                                var meta = document.querySelector('meta[name=viewport]');
                                if (!meta) {
                                    meta = document.createElement('meta');
                                    meta.name = "viewport";
                                    document.head.appendChild(meta);
                                }
                                meta.setAttribute('content', 'width=1280, initial-scale=1.0, maximum-scale=1.0, user-scalable=yes');
                            }
                            forceDesktop();
                            let count = 0;
                            const interval = setInterval(() => {
                                forceDesktop();
                                if (++count >= 10) clearInterval(interval);
                            }, 500);
                        })();
                        """.trimIndent(),
                        null
                    )
                    url?.let { onUrlChanged(it) }
                    view?.evaluateJavascript(DevToolsInjector.LATE_INJECTION, null)
                    onPageFinished()
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    url?.let { onUrlChanged(it) }
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Log.d("BrowserSession", "WebView Crashed")
                    destroy()
                    return true
                }
            }
        }
        return view!!
    }

    override fun destroy() {
        view?.let { WebViewPool.recycle(it) }
        super.destroy()
    }
}
