package com.bsbeetle.bananaspaghetti.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import com.bsbeetle.bananaspaghetti.tenebrio.data.ShellExecutor
import com.bsbeetle.bananaspaghetti.tenebrio.data.outputfd
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

            webView!!.addJavascriptInterface(TerminalBridge { input ->
                outputfd.outputFd!!.write("$input\n".toByteArray())
                outputfd.outputFd!!.flush()
            }, "AndroidTerminal")

        }
        return webView!!
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }

}