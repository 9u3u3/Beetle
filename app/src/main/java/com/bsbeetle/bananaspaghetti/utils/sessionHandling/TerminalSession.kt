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
import com.bsbeetle.bananaspaghetti.services.TerminalService
import com.bsbeetle.bananaspaghetti.tenebrio.pty.Tenebrio
import com.bsbeetle.bananaspaghetti.utils.AppConfig
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalSession(context: Context, name: String = "Terminal", val initialDir: String? = null) :
        Session(context.applicationContext, SessionType.TERMINAL, name) {

    var outputFd: FileOutputStream? = null
    private var tenebrio: Tenebrio? = null
    var rows: Int? = 24
    var cols: Int? = 80
    var onOutput: ((String) -> Unit)? = null
    var onProcessExited: (() -> Unit)? = null

    // Track if this session has been counted in the service
    private var isRegisteredWithService = false
    var onCtrlUsed: (() -> Unit)? = null

    // Session-scoped coroutines (cancelled on destroy)
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Observable state for UI to show loading until terminal is ready
    private val _isReady = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isReady: kotlinx.coroutines.flow.StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        Log.d("TerminalSession", "Creating tenebrio for ID: $id, initialDir: $initialDir")
        tenebrio = Tenebrio(context.applicationContext, this)
        try {
            tenebrio!!.init()

            // Start foreground service and register this session
            if (!name.startsWith("Install-")) {
                TerminalService.startService(context.applicationContext)
                TerminalService.incrementSessionCount()
                isRegisteredWithService = true
            }

            // If initial directory is provided, we need to cd into it
            // We detect shell readiness via prompt output instead of a hardcoded delay
            initialDir?.let { dir ->
                Log.d("TerminalSession", "Setting up initial directory: $dir")
                if (dir.startsWith("/")) {
                    setupInitialDirectory(dir)
                }
            }
        } catch (e: Exception) {
            Log.e("TerminalSession", "Null tenebrio, object creation failed!")
        }
    }

    private fun setupInitialDirectory(dir: String) {
        var prompted = false
        val originalOnOutput = onOutput

        // Watch for shell prompt to know when it's ready for commands
        onOutput = { text ->
            originalOnOutput?.invoke(text)
            if (!prompted && (text.contains("$ ") || text.contains("# "))) {
                prompted = true
                onOutput = originalOnOutput // Restore original callback

                sessionScope.launch {
                    val cdCommand = buildCdCommand(dir)
                    cdCommand?.let { insertText(it) }
                }
            }
        }

        // Safety fallback: if no prompt detected within 3 seconds, send cd anyway
        sessionScope.launch {
            delay(3000)
            if (!prompted) {
                prompted = true
                onOutput = originalOnOutput
                val cdCommand = buildCdCommand(dir)
                cdCommand?.let { insertText(it) }
            }
        }
    }

    private fun buildCdCommand(dir: String): String? {
        return when {
            // External storage - create symlink in /home
            dir.startsWith("/storage/emulated/0/") -> {
                val projectName = dir.substringAfterLast("/").ifEmpty { "project" }
                val symlinkPath = "/home/$projectName"
                Log.d("TerminalSession", "Creating symlink: $symlinkPath -> $dir")
                "rm -f \"$symlinkPath\" 2>/dev/null; ln -sf \"$dir\" \"$symlinkPath\" && export HOME=/home && cd \"$symlinkPath\" && clear\n"
            }
            // Ubuntu internal storage - exact home directory
            dir.endsWith("/ubuntu/home") -> {
                "export HOME=/home && cd /home && clear\n"
            }
            // Ubuntu internal storage with subpath
            dir.contains("/ubuntu/home/") -> {
                val relativePath = dir.substringAfter("/ubuntu/home/")
                "export HOME=/home && cd \"/home/$relativePath\" && clear\n"
            }
            // Already a /home path
            dir.startsWith("/home/") -> {
                "export HOME=/home && cd \"$dir\" && clear\n"
            }
            // Other paths
            else -> {
                "export HOME=/home && cd \"$dir\" && clear\n"
            }
        }
    }

    override fun destroy() {
        Log.d("TerminalSession", "Destroying terminal session: $name")

        // Cancel all session-scoped coroutines
        sessionScope.cancel()

        // Unregister from service if we were registered
        if (isRegisteredWithService) {
            TerminalService.decrementSessionCount(context)
            isRegisteredWithService = false
        }

        // Properly destroy tenebrio (closes FDs, kills process, stops reader thread)
        tenebrio?.destroy()
        tenebrio = null

        // Clean up output callback
        onOutput = null
        onProcessExited = null

        // Close file descriptor
        try {
            outputFd?.close()
        } catch (e: Exception) {
            Log.e("TerminalSession", "Error closing outputFd", e)
        }
        outputFd = null

        // Call parent destroy
        super.destroy()
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
                                    writeToTerminal("$input\n".toByteArray())
                                }

                                @JavascriptInterface
                                fun sendRaw(input: String) {
                                    writeToTerminal(input.toByteArray())
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
                                    if ((tenebrio?.gMasterFd ?: -1) < 0) {
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

                    // CTRL callback interface - called by JS when Ctrl+key is used
                    addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun ctrlUsed() {
                                    Log.d("TerminalSession", "CTRL used, resetting state")
                                    onCtrlUsed?.invoke()
                                }
                            },
                            "AndroidTerminalCtrl"
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

    @Synchronized
    private fun writeToTerminal(data: ByteArray) {
        if (data.isEmpty()) return
        val fd = outputFd ?: return
        try {
            fd.write(data)
            fd.flush()
        } catch (e: Exception) {
            Log.e("TerminalSession", "Failed to write to terminal", e)
        }
    }

    fun insertText(text: String) {
        writeToTerminal(text.toByteArray())
    }

    /**
     * Send installation progress message directly to terminal WebView.
     * Used during Ubuntu installation when PTY doesn't exist yet.
     */
    fun sendInstallationMessage(message: String) {
        view?.post {
            val jsCommand = "term.write(${org.json.JSONObject.quote("$message\r\n")});"
            view?.evaluateJavascript(jsCommand, null)
            Log.d("TerminalSession", "Installation message: $message")
        }
    }
}
