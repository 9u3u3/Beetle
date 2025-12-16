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
import kotlinx.coroutines.flow.asStateFlow

class TerminalSession(context: Context, name: String = "Terminal", val initialDir: String? = null) :
        Session(context.applicationContext, SessionType.TERMINAL, name) {

    var outputFd: FileOutputStream? = null
    private var tenebrio: Tenebrio? = null
    var rows: Int? = 24
    var cols: Int? = 80
    var onOutput: ((String) -> Unit)? = null

    // Observable state for UI to show loading until terminal is ready
    private val _isReady = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isReady: kotlinx.coroutines.flow.StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        Log.d("TerminalSession", "Creating tenebrio for ID: $id")
        tenebrio = Tenebrio(context.applicationContext, this)
        try {
            tenebrio!!.init()

            // If initial directory is provided, cd into it
            initialDir?.let { dir ->
                // We need to wait a bit or just write it?
                // Writing immediately might be lost if shell isn't fully ready,
                // but usually it buffers.
                // Also need to handle internal vs external paths.
                // For now, assume the path is valid for the shell (or mapped).
                // If it's a content URI, we can't cd into it.
                if (dir.startsWith("/")) {
                    // It's a path
                    insertText("cd \"$dir\"\n")
                    insertText("clear\n") // Optional: clear the cd command from view
                }
            }
        } catch (e: Exception) {
            Log.e("TerminalSession", "Null tenebrio, object creation failed!")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(): CustomWebView {
        if (view != null) return view!!
        view =
                CustomWebView(context.applicationContext, this).apply {
                    // Make WebView completely invisible during creation to prevent any flash
                    visibility = android.view.View.INVISIBLE
                    alpha = 0f
                    // Set dark background as fallback
                    setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))

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
                    layoutParams =
                            ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )

                    addJavascriptInterface(
                            object {
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
                            },
                            "AndroidTerminal"
                    )

                    addJavascriptInterface(
                            object {
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
                                            // Terminal is now ready - hide loading screen
                                            _isReady.value = true
                                        } catch (e: Exception) {
                                            Log.e("TerminalSession", "Tenebrio init failed", e)
                                        }
                                    } else {
                                        // Already initialized, just resize
                                        _isReady.value = true
                                        tenebrio!!.resizeTerminal(
                                                this@TerminalSession.rows!!,
                                                this@TerminalSession.cols!!
                                        )
                                    }
                                }
                            },
                            "AndroidTerminalResize"
                    )

                    webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Make the WebView visible after page loads
                                    view?.visibility = android.view.View.VISIBLE
                                    view?.alpha = 1f
                                    Log.d("TerminalSession", "Terminal page loaded, making visible")
                                }
                                
                                override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                ): Boolean {
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
