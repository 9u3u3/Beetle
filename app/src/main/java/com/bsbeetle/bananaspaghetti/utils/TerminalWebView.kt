package com.bsbeetle.bananaspaghetti.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import com.bsbeetle.bananaspaghetti.tenebrio.data.ShellExecutor
import com.bsbeetle.bananaspaghetti.tenebrio.data.outputfd
import com.bsbeetle.bananaspaghetti.tenebrio.pty.Tenebrio
import com.bsbeetle.bananaspaghetti.tenebrio.terminal.TerminalBridge

@SuppressLint("StaticFieldLeak")
object TerminalWebView {
    private var webView: WebView? = null
    private var tenebrio: Tenebrio? = null
    @SuppressLint("SetJavaScriptEnabled")
    fun getWebview(context: Context, tenebrio: Tenebrio): WebView {
        if(webView == null) {

            this.tenebrio = tenebrio

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

            webView!!.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webView!!.addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun sendInput(input: String) {
                    outputfd.outputFd!!.write("$input\n".toByteArray())
                    outputfd.outputFd!!.flush()
                }

                @android.webkit.JavascriptInterface
                fun sendRaw(input: String) {
                    Log.d("Terminal", "sendRaw: bytes=${input.toByteArray().joinToString(" ") { "%02x".format(it) }}")
                    outputfd.outputFd!!.write(input.toByteArray())
                    outputfd.outputFd!!.flush()
                }
            }, "AndroidTerminal")

            webView!!.addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun resizeTerminal(rows: Int, cols: Int) {
                    tenebrio.resizeTerminal(rows, cols)
                }
            }, "AndroidTerminalResize")

        }
        return webView!!
    }

    fun destroy() {
        webView?.destroy()
        webView = null
    }

}