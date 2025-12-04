package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bsbeetle.bananaspaghetti.tenebrio.pty.Tenebrio
import com.bsbeetle.bananaspaghetti.utils.AppConfig
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import java.io.FileOutputStream

class TerminalSession(context: Context, name: String = "Terminal") : Session(context, SessionType.TERMINAL, name) {

    var outputFd: FileOutputStream? = null
    private var tenebrio: Tenebrio? = null
    var rows: Int? = null
    var cols: Int? = null

    init {
        Log.d("TerminalSession", "Creating tenebrio for ID: $id")
        tenebrio = Tenebrio(context.applicationContext, this)
        try {
            tenebrio!!.init()
        } catch (e: Exception) {
            Log.e("TerminalSession", "Null tenebrio, object creation failed!")
        }
        createView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(): CustomWebView {
        if (view != null) return view!!
        view = CustomWebView(context.applicationContext, this).apply {
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
            loadUrl(AppConfig.TERMINAL_URL)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            addJavascriptInterface(object {
                @JavascriptInterface
                fun sendInput(input: String) {
                    outputFd?.write("$input\n".toByteArray())
                    outputFd?.flush()
                }

                @JavascriptInterface
                fun sendRaw(input: String) {
                    outputFd?.write(input.toByteArray())
                    outputFd?.flush()
                }
            }, "AndroidTerminal")

            addJavascriptInterface(object {
                @JavascriptInterface
                fun resizeTerminal(rows: Int, cols: Int) {
                    tenebrio?.resizeTerminal(rows, cols)
                }

                @JavascriptInterface
                fun getRowsCols(_rows: Int, _cols: Int) {
                    rows = _rows
                    cols = _cols
                    if (tenebrio?.gMasterFd ?: -1 < 0) {
                        try {
                            tenebrio!!.init()
                        } catch (e: Exception) {
                            Log.e("TerminalSession", "Tenebrio init failed", e)
                        }
                    } else {
                        tenebrio!!.resizeTerminal(this@TerminalSession.rows!!, this@TerminalSession.cols!!)
                    }
                }
            }, "AndroidTerminalResize")

            webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Log.d("TerminalSession", "WebView Crashed")
                    destroy()
                    return true
                }
            }
        }
        return view!!
    }

    fun insertText(text: String) {
        try {
            outputFd?.write(text.toByteArray())
            outputFd?.flush()
        } catch (e: Exception) {
            Log.e("TerminalSession", "Failed to write to terminal", e)
        }
    }
}
