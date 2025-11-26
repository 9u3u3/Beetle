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
    BROWSER,
    TERMINAL,
    DEMO
}

class Session(
    val context: Context,
    val type: SessionType,
    onPageFinished: () -> Unit = {},
    onProgressChanged: (Int) -> Unit = {},
    onFrameworkDetected: (String, String) -> Unit = { _, _ -> },
    onNetworkRequest: () -> Unit = {},
    onConsoleLog: () -> Unit = {},
    onUrlChanged: (String) -> Unit = {}
) {

    // Views
    var editorView: CustomWebView? = null
    var terminalView: CustomWebView? = null
    var browserView: CustomWebView? = null
    var demoView: CustomWebView? = null

    var _inputConnection: InputConnection? = null
    var id: Int? = null

    // Terminal
    var outputFd: FileOutputStream? = null
    private var tenebrio: Tenebrio? = null
    var rows: Int? = null
    var cols: Int? = null

    init {
        // Add the session to the list when the object is created and set it as active
        id = SessionList.sessions.size
        SessionList.sessions.add(this)
        SessionList.activeSessionIndex = SessionList.sessions.indexOf(this)

        when (type) {
            SessionType.EDITOR -> createEditorView()
            SessionType.TERMINAL -> createTerminalView()
            SessionType.BROWSER -> createBrowserView(
                onPageFinished,
                onProgressChanged,
                onFrameworkDetected,
                onNetworkRequest,
                onConsoleLog,
                onUrlChanged
            )

            SessionType.DEMO -> createDemoView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createEditorView() {
        if (editorView != null) return // Don't recreate if exists
        editorView = CustomWebView(context.applicationContext, this).apply {
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
                val isPortrait =
                    resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
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

        Log.d("Session", "Creating tenebrio for ID: $id")
        tenebrio = Tenebrio(context.applicationContext, this)

        try {
            tenebrio!!.init()
        } catch (e: Exception) {
            Log.e("Session", "Null tenebrio, object creation failed!")
        }

        terminalView = CustomWebView(context.applicationContext, this).apply {

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
                    outputFd!!.write("$input\n".toByteArray())
                    outputFd!!.flush()
                }

                @JavascriptInterface
                fun sendRaw(input: String) {
                    Log.d(
                        "Terminal",
                        "sendRaw: bytes=${
                            input.toByteArray().joinToString(" ") { "%02x".format(it) }
                        }"
                    )
                    outputFd!!.write(input.toByteArray())
                    outputFd!!.flush()
                }
            }, "AndroidTerminal")

            addJavascriptInterface(object {

                @JavascriptInterface
                fun resizeTerminal(rows: Int, cols: Int) {
                    Log.d("TerminalWebView", "Called resize Terminal")
                    tenebrio!!.resizeTerminal(rows, cols)
                }

                @JavascriptInterface
                fun getRowsCols(_rows: Int, _cols: Int) {
                    Log.d("TerminalWebView", "Called resize Terminal")
                    rows = _rows
                    cols = _cols
                    if (tenebrio?.gMasterFd ?: -1 < 0) {
                        try {
                            tenebrio!!.init()
                        } catch (e: Exception) {
                            Log.e("Session", "Tenebrio init failed", e)
                        }
                    } else {

                        tenebrio!!.resizeTerminal(this@Session.rows!!, this@Session.cols!!)

                    }
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
        browserView = CustomWebView(context.applicationContext, this).apply {

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

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let { onUrlChanged(it) }
                    view?.evaluateJavascript(DevToolsInjector.EARLY_INJECTION, null)
                }

                // This doesn't work
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    /** 🔥 Inject desktop viewport override */
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

    @SuppressLint("SetJavaScriptEnabled")
    fun createDemoView() {
        if (demoView != null) return // Don't recreate if exists
        demoView = CustomWebView(context, this).apply {
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
            loadUrl("file:///android_asset/editor/index.html")
        }
    }

    fun destroySession() {
        // For now we will keep this session as dangling session, later change it do directly removing the session object from memory
        when (type) {

            SessionType.DEMO -> {
                demoView?.destroy()
                demoView = null
            }

            SessionType.TERMINAL -> {
                terminalView?.destroy()
                terminalView = null
            }

            SessionType.EDITOR -> {
                editorView?.destroy()
                editorView = null
            }

            SessionType.BROWSER -> {
                browserView?.destroy()
                browserView = null
            }
        }

        SessionList.activeSessionIndex =
            SessionList.sessions.size - 1 // This will be for sure buggy i will handle this later
        SessionList.sessions.remove(this)


    }

    // Getters
    fun _getEditorView(): CustomWebView {
        return editorView!!
    }

    fun _getTerminalView(): CustomWebView {
        return terminalView!!
    }

    fun _getBrowserView(): CustomWebView {
        return browserView!!
    }

    fun _getDemoView(): CustomWebView {
        return demoView!!
    }

}