package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bsbeetle.bananaspaghetti.utils.AppConfig
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import com.jcraft.jsch.ChannelShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/**
 * A terminal session that connects to a remote machine via an SSH ChannelShell.
 * Bridges JSch channel I/O to the xterm.js WebView (same terminal UI as TerminalSession).
 */
class SshTerminalSession(
    context: Context,
    name: String = "SSH Terminal",
    private val jschSession: com.jcraft.jsch.Session
) : Session(context.applicationContext, SessionType.TERMINAL, name) {

    private var channel: ChannelShell? = null
    private var channelInputStream: InputStream? = null
    private var channelOutputStream: OutputStream? = null
    private var readerThread: Thread? = null
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var termRows = 24
    private var termCols = 80

    init {
        sessionScope.launch {
            try {
                val ch = jschSession.openChannel("shell") as ChannelShell
                ch.setPtyType("xterm-256color")
                ch.setPtySize(termCols, termRows, 0, 0)
                ch.connect(15_000)
                channel = ch
                channelInputStream = ch.inputStream
                channelOutputStream = ch.outputStream
                startReaderThread()
                Log.d("SshTerminalSession", "SSH shell channel opened for: $name")
            } catch (e: Exception) {
                Log.e("SshTerminalSession", "Failed to open SSH shell channel", e)
            }
        }
    }

    private fun startReaderThread() {
        val stream = channelInputStream ?: return
        readerThread = Thread {
            val buffer = ByteArray(4096)
            try {
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    val text = String(buffer, 0, n, Charsets.UTF_8)
                    val escaped = JSONObject.quote(text)
                    view?.post {
                        view?.evaluateJavascript("term.write($escaped)", null)
                    }
                }
            } catch (e: Exception) {
                Log.d("SshTerminalSession", "Reader thread ended: ${e.message}")
            }
        }.also {
            it.isDaemon = true
            it.name = "ssh-terminal-reader-$name"
            it.start()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(): CustomWebView {
        if (view != null) return view!!
        view = CustomWebView(context.applicationContext, this).apply {
            visibility = android.view.View.INVISIBLE
            alpha = 0f
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
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Keyboard input → SSH channel
            addJavascriptInterface(object {
                @JavascriptInterface
                fun sendInput(input: String) { writeToChannel("$input\n") }

                @JavascriptInterface
                fun sendRaw(input: String) { writeToChannel(input) }
            }, "AndroidTerminal")

            // Resize events
            addJavascriptInterface(object {
                @JavascriptInterface
                fun resizeTerminal(r: Int, c: Int) {
                    termRows = r
                    termCols = c
                    channel?.setPtySize(c, r, 0, 0)
                }

                @JavascriptInterface
                fun getRowsCols(r: Int, c: Int) {
                    termRows = r
                    termCols = c
                    channel?.setPtySize(c, r, 0, 0)
                    _isReady.value = true
                }
            }, "AndroidTerminalResize")

            // Ctrl key callback (no-op for SSH — channel handles ctrl chars directly)
            addJavascriptInterface(object {
                @JavascriptInterface
                fun ctrlUsed() {}
            }, "AndroidTerminalCtrl")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.visibility = android.view.View.VISIBLE
                    view?.alpha = 1f
                    _isReady.value = true
                }
            }
        }
        return view!!
    }

    private fun writeToChannel(text: String) {
        try {
            channelOutputStream?.write(text.toByteArray(Charsets.UTF_8))
            channelOutputStream?.flush()
        } catch (e: Exception) {
            Log.e("SshTerminalSession", "Write to channel failed: ${e.message}")
        }
    }

    override fun destroy() {
        Log.d("SshTerminalSession", "Destroying SSH terminal session: $name")
        sessionScope.cancel()
        readerThread?.interrupt()
        try { channel?.disconnect() } catch (_: Exception) {}
        channel = null
        channelInputStream = null
        channelOutputStream = null
        super.destroy()
    }
}
