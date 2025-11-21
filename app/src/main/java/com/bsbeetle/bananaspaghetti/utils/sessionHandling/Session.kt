package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bsbeetle.bananaspaghetti.tenebrio.data.outputfd
import com.bsbeetle.bananaspaghetti.tenebrio.data.termSize
import com.bsbeetle.bananaspaghetti.tenebrio.pty.Tenebrio
import com.bsbeetle.bananaspaghetti.ui.browser.DevBridge
import com.bsbeetle.bananaspaghetti.ui.browser.DevToolsInjector
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import java.io.FileOutputStream

enum class SessionType {
    EDITOR,
    // BROWSER,
    TERMINAL
}

class Session(
   val context: Context,
   val type: SessionType
) {

    // Views
    var editorView: CustomWebView? = null
    var terminalView: CustomWebView? = null
    var browserView: CustomWebView? = null

    // Other objects required for the functioning of the views
    private var _inputConnection: InputConnection? = null
    private var outputFd: FileOutputStream? = null
    private var tenebrio: Tenebrio? = null
    var id: Int? = null

    init {
        // Add the session to the list when the object is created
        id = SessionList.sessions.size
        SessionList.sessions.add(this)
        SessionList.activeSessionIndex = SessionList.sessions.indexOf(this)
        when(type) {
            SessionType.EDITOR -> createEditorView()
            SessionType.TERMINAL -> createTerminalView()
            else -> {}
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createEditorView() {
        if (editorView != null) return // Don't recreate if exists
        editorView = CustomWebView(context.applicationContext).apply {
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
            loadUrl("http://localhost:8080/index.html")
            post {
                val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                if (isPortrait) requestFocus()
                else clearFocus()
            }
            // Overriding the webview client to handle the render process crashing
            webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    Log.d("EditorViewModel", "WebView Crashed")
                    view?.destroy()
                    return true
                }
            }
        }
    }


    fun createTerminalView() {
        if (terminalView != null) return
        tenebrio = Tenebrio(context.applicationContext, id) // Create a PTY session

        terminalView = CustomWebView(context.applicationContext).apply {

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

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            addJavascriptInterface(object {

                @JavascriptInterface
                fun sendInput(input: String) {
                    outputfd.outputFd!!.write("$input\n".toByteArray())
                    outputfd.outputFd!!.flush()
                }

                @JavascriptInterface
                fun sendRaw(input: String) {
                    Log.d("Terminal", "sendRaw: bytes=${input.toByteArray().joinToString(" ") { "%02x".format(it) }}")
                    outputfd.outputFd!!.write(input.toByteArray())
                    outputfd.outputFd!!.flush()
                }
            }, "AndroidTerminal")

            addJavascriptInterface(object {

                @JavascriptInterface
                fun resizeTerminal(rows: Int, cols: Int) {
                    Log.d("TerminalWebView", "Called resize Terminal")
                    tenebrio!!.resizeTerminal(rows, cols)
                }

                @JavascriptInterface
                fun getRowsCols(rows: Int, cols: Int) {
                    termSize.row = rows
                    termSize.cols = cols
                }

            }, "AndroidTerminalResize")
        }

    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createBrowserView(
        onPageFinished: () -> Unit,
        onProgressChanged: (Int) -> Unit,
        onFrameworkDetected: (String, String) -> Unit,
        onNetworkRequest: () -> Unit,
        onConsoleLog: () -> Unit,
        onUrlChanged: (String) -> Unit
    ) {
        browserView = CustomWebView(context.applicationContext).apply {

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
            WebView.setWebContentsDebuggingEnabled(true)

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
    }

    // Getters
    fun _getEditorView() : CustomWebView {return editorView!!}
    fun _getTerminalView() : CustomWebView {return terminalView!!}
    fun _getBrowserView() : CustomWebView {return browserView!!}

}