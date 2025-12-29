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
import kotlinx.coroutines.GlobalScope
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

    // Track if this session has been counted in the service
    private var isRegisteredWithService = false
    var onCtrlUsed: (() -> Unit)? = null

    // Observable state for UI to show loading until terminal is ready
    private val _isReady = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isReady: kotlinx.coroutines.flow.StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        Log.d("TerminalSession", "Creating tenebrio for ID: $id, initialDir: $initialDir")
        tenebrio = Tenebrio(context.applicationContext, this)
        try {
            tenebrio!!.init()

            // Start foreground service and register this session
            // DISABLED - notification functionality removed
            // if (!name.startsWith("Install-")) {
            //     TerminalService.startService(context.applicationContext)
            //     TerminalService.incrementSessionCount()
            //     isRegisteredWithService = true
            // }

            // If initial directory is provided, we need to cd into it
            // We delay this so it runs AFTER the shell's init script completes
            initialDir?.let { dir ->
                Log.d("TerminalSession", "Setting up initial directory: $dir")
                if (dir.startsWith("/")) {
                    // Use a coroutine to delay the cd command
                    GlobalScope.launch {
                        // Wait for shell to initialize (initrc runs and does 'cd')
                        delay(1500)

                        // Convert the path to Ubuntu-friendly format
                        val ubuntuPath =
                                when {
                                    // External storage - create symlink in /home
                                    dir.startsWith("/storage/emulated/0/") -> {
                                        val projectName =
                                                dir.substringAfterLast("/").ifEmpty { "project" }
                                        val symlinkPath = "/home/$projectName"
                                        Log.d(
                                                "TerminalSession",
                                                "Creating symlink: $symlinkPath -> $dir"
                                        )
                                        // Export HOME=/home so that /home/project shows as
                                        // ~/project
                                        insertText(
                                                "rm -f \"$symlinkPath\" 2>/dev/null; ln -sf \"$dir\" \"$symlinkPath\" && export HOME=/home && cd \"$symlinkPath\" && clear\n"
                                        )
                                        null // Already handled
                                    }
                                    // Ubuntu internal storage - exact home directory
                                    // (e.g., /data/user/0/.../ubuntu/home)
                                    dir.endsWith("/ubuntu/home") -> {
                                        "/home"
                                    }
                                    // Ubuntu internal storage with subpath (e.g.,
                                    // /data/user/0/.../ubuntu/home/Test)
                                    dir.contains("/ubuntu/home/") -> {
                                        // Extract the path after "ubuntu/home/" to get the relative
                                        // path
                                        val relativePath = dir.substringAfter("/ubuntu/home/")
                                        "/home/$relativePath"
                                    }
                                    // Already a /home path
                                    dir.startsWith("/home/") -> dir
                                    // Other paths
                                    else -> dir
                                }

                        // If we have a path to cd to (wasn't handled by symlink)
                        ubuntuPath?.let { path ->
                            Log.d("TerminalSession", "cd to: $path")
                            insertText("export HOME=/home && cd \"$path\" && clear\n")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TerminalSession", "Null tenebrio, object creation failed!")
        }
    }

    override fun destroy() {
        Log.d("TerminalSession", "Destroying terminal session: $name")

        // Unregister from service if we were registered - DISABLED
        // if (isRegisteredWithService) {
        //     TerminalService.decrementSessionCount(context)
        //     isRegisteredWithService = false
        // }

        // Clean up tenebrio reference (no explicit destroy method)
        tenebrio = null

        // Clean up output callback
        onOutput = null

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
        try {
            outputFd?.write(data)
            outputFd?.flush()
        } catch (e: Exception) {
            Log.e("TerminalSession", "Failed to write to terminal", e)
        }
    }

    fun insertText(text: String) {
        writeToTerminal(text.toByteArray())
    }
}
