package com.bsbeetle.bananaspaghetti.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import com.bsbeetle.bananaspaghetti.tenebrio.data.ShellExecutor
import com.bsbeetle.bananaspaghetti.tenebrio.terminal.TerminalBridge

@SuppressLint("StaticFieldLeak")
object TerminalWebView {
    private var webView: WebView? = null
    @SuppressLint("SetJavaScriptEnabled")
    fun getWebview(context: Context): WebView {
        if(webView == null) {
            webView = WebView(context.applicationContext).apply {

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

                loadUrl("file:///android_asset/terminal/index.html")

            }

            // Attaching the interfaces
            webView!!.addJavascriptInterface(TerminalBridge { input ->
                // This is where the typed input arrives
                Log.d("TerminalInput", input)

                // Proot session
                ShellExecutor.sendCommand(input)
            }, "AndroidTerminal")

        }
        return webView!!
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }

}