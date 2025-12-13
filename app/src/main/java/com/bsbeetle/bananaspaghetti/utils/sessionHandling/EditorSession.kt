package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.ClipboardManager
import com.bsbeetle.bananaspaghetti.utils.AppConfig
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.WebViewPool
import kotlinx.coroutines.launch

class EditorSession(context: Context, name: String = "Editor") : Session(context.applicationContext, SessionType.EDITOR, name) {

    private var currentFilePath: String? = null
    
    // Project root folder for the editor session
    var projectRoot: androidx.documentfile.provider.DocumentFile? = null
    
    // Track cursor position for backspace guard (updated by LSP-STATUS parsing)
    var cursorLine: Int = 1
        private set
    var cursorColumn: Int = 1
        private set
    
    // Content preservation: save editor content when navigating away
    private var savedContent: String? = null
    private var needsRestore: Boolean = false
    
    fun updateCursorPosition(line: Int, col: Int) {
        cursorLine = line
        cursorColumn = col
    }
    
    /**
     * Save the current editor content before navigating away.
     * Call this when leaving the editor screen.
     */
    fun saveContent() {
        view?.evaluateJavascript("window.code_") { result ->
            if (result != null && result != "null" && result != "\"\"") {
                savedContent = result.removeSurrounding("\"")
                    .replace("\\n", "\n")
                    .replace("\\u003C", "<")
                    .replace("\\u003E", ">")
                    .replace("\\\"", "\"")
                needsRestore = true
                Log.d("EditorSession", "Content saved (${savedContent?.length ?: 0} chars)")
            }
        }
    }
    
    /**
     * Restore saved content after navigating back to the editor.
     * Call this when the editor screen is shown again.
     */
    fun restoreContent() {
        if (needsRestore && savedContent != null) {
            val content = savedContent!!
            val jsCommand = """
                if (window.isEditorReady === true) {
                    window.editor.dispatch({
                        changes: { from: 0, to: window.editor.state.doc.length, insert: ${org.json.JSONObject.quote(content)} }
                    });
                }
            """.trimIndent()
            view?.evaluateJavascript(jsCommand) { result ->
                Log.d("EditorSession", "Content restored")
            }
            needsRestore = false
        }
    }

    init {
        createView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(): CustomWebView {
        if (view != null) return view!!
        // Disable WebViewPool to prevent recycling crashed views
        // view = WebViewPool.get(context.applicationContext).apply {
        view = CustomWebView(context.applicationContext, null).apply {
            session = this@EditorSession
            WebView.setWebContentsDebuggingEnabled(true)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowUniversalAccessFromFileURLs = true // Required for dynamic imports from file://
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                // Memory optimizations
                setSafeBrowsingEnabled(false)
                saveFormData = false
                databaseEnabled = false
                // cacheMode = WebSettings.LOAD_NO_CACHE // Already set
            }
            loadUrl(AppConfig.EDITOR_URL)
            post {
                val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                if (isPortrait) requestFocus() else clearFocus()
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                    if (isPortrait) {
                        view?.evaluateJavascript("if (window.editor) { window.editor.focus(); }", null)
                    }
                    // Inject dynamic inputmode handling (One-time check + Resize listener)
                    view?.evaluateJavascript("""
                        (function() {
                            function updateInputMode() {
                                const content = document.querySelector('.cm-content');
                                if (!content) return;
                                const isLandscape = window.innerWidth > window.innerHeight;
                                const mode = isLandscape ? 'none' : 'text';
                                if (content.getAttribute('inputmode') !== mode) {
                                    content.setAttribute('inputmode', mode);
                                }
                            }
                            window.addEventListener('resize', updateInputMode);
                            
                            // Initial check
                            updateInputMode();
                            
                            // Use MutationObserver
                            const content = document.querySelector('.cm-content');
                            if (content) {
                                const observer = new MutationObserver(() => updateInputMode());
                                observer.observe(content, { attributes: true, attributeFilter: ['inputmode'] });
                            } else {
                                // Fallback only if content not found immediately
                                const checkExist = setInterval(function() {
                                   const content = document.querySelector('.cm-content');
                                   if (content) {
                                      clearInterval(checkExist);
                                      updateInputMode();
                                      const observer = new MutationObserver(() => updateInputMode());
                                      observer.observe(content, { attributes: true, attributeFilter: ['inputmode'] });
                                   }
                                }, 100);
                                setTimeout(() => clearInterval(checkExist), 2000);
                            }
                        })();
                    """.trimIndent(), null)
<<<<<<< HEAD
                    updateLanguage(name)
=======
                    
                    // Apply pending file path if any
                    currentFilePath?.let { setFilePath(it) }

                    // Start Memory Monitor
                    startMemoryMonitor()
>>>>>>> bce1bd3c88405ca2ba91e3f0dba45b8bbaf33cf5
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Log.e("WEBVERR", "WebView Render Process Gone (Crash): $detail")
                    // Do NOT recycle this view. Destroy it immediately.
                    view?.destroy()
                    this@EditorSession.view = null
                    // Do not call super.destroy() which might recycle
                    return true
                }
            }
        }
        return view!!
    }

    private fun startMemoryMonitor() {
        val monitor = object : Runnable {
            override fun run() {
                try {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    val memInfo = android.app.ActivityManager.MemoryInfo()
                    am.getMemoryInfo(memInfo)
                    
                    val runtime = Runtime.getRuntime()
                    val usedJavaHeap = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                    val availMem = memInfo.availMem / 1024 / 1024
                    val threshold = memInfo.threshold / 1024 / 1024
                    
                    
                    // Send stats to WebView
                    val jsonStats = """
                        {
                            "availMem": $availMem,
                            "threshold": $threshold,
                            "lowMemory": ${memInfo.lowMemory},
                            "javaHeap": $usedJavaHeap
                        }
                    """.trimIndent()
                    
                    view?.evaluateJavascript("if(window.updateDebugStats) window.updateDebugStats($jsonStats);", null)
                    
                    view?.postDelayed(this, 2000) // Update every 2 seconds
                } catch (e: Exception) {
                    Log.e("WEBVERR", "Memory monitor failed", e)
                }
            }
        }
        view?.post(monitor)
    }

    override fun destroy() {
<<<<<<< HEAD
        view?.let { WebViewPool.recycle(it) }
        view = null // Prevent super.destroy() from destroying the recycled view
=======
        // view?.let { WebViewPool.recycle(it) }
        view?.destroy()
        view = null
>>>>>>> bce1bd3c88405ca2ba91e3f0dba45b8bbaf33cf5
        super.destroy()
    }

    // JS Injection methods for reliable input
    fun insertTextToEditor(text: String) {
        val escapedText = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        view?.evaluateJavascript("if(window.insertText) window.insertText('$escapedText');", null)
    }
    
    fun setFilePath(path: String) {
        currentFilePath = path
        view?.evaluateJavascript("if(window.setFilePath) window.setFilePath('$path');", null)
    }

    fun setLanguage(slug: String, port: Int = 8081) {
        val jsPath = "http://localhost:8080/plugins/$slug/$slug.js"
        view?.evaluateJavascript("if(window.setLanguage) window.setLanguage('$slug', $port);", null)
    }
    
    fun clearLanguage() {
        view?.evaluateJavascript("if(window.clearLanguage) window.clearLanguage();", null)
    }

    fun performEditorAction(action: String) {
        view?.evaluateJavascript("if(window.$action) window.$action();", null)
    }
<<<<<<< HEAD

    fun sendCtrlKey(key: String) {
        val js = """
            (function() {
                const key = '$key';
                const code = 'Key' + key.toUpperCase();
                const event = new KeyboardEvent('keydown', {
                    key: key,
                    code: code,
                    ctrlKey: true,
                    bubbles: true,
                    cancelable: true
                });
                document.activeElement.dispatchEvent(event);
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val item = clipboard.primaryClip?.getItemAt(0)
            val text = item?.text?.toString()
            if (!text.isNullOrEmpty()) {
                insertTextToEditor(text)
            }
        }
    }

    fun updateLanguage(filename: String) {
        val extension = filename.substringAfterLast('.', "")
        if (extension.isNotEmpty()) {
            view?.evaluateJavascript("if(window.setLanguage) window.setLanguage('$extension');", null)
        }
    }
=======
    
    // Overload for actions with count parameter (e.g., batched backspace)
    fun performEditorAction(action: String, count: Int) {
        view?.evaluateJavascript("if(window.$action) window.$action($count);", null)
    }

    fun performNavigation(type: com.bsbeetle.bananaspaghetti.model.KeyType) {
        val action = when(type) {
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_UP -> "moveUp"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_DOWN -> "moveDown"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_LEFT -> "moveLeft"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_RIGHT -> "moveRight"
            else -> return
        }
        view?.evaluateJavascript("if(window.$action) window.$action();", null)
    }

    fun setLspViewModel(viewModel: com.bsbeetle.bananaspaghetti.viewmodel.LspViewModel) {
        // Use WebChromeClient to intercept console messages instead of JavascriptInterface
        // This avoids the need to reload the WebView which wipes content
        view?.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let { cm ->
                    val msg = cm.message()
                    val level = cm.messageLevel()
                    
                    // Log all console messages with WEBVERR if they are errors or warnings, or if requested
                    if (level == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                        Log.e("WEBVERR", "Console Error: $msg at ${cm.sourceId()}:${cm.lineNumber()}")
                    } else if (level == android.webkit.ConsoleMessage.MessageLevel.WARNING) {
                        Log.w("WEBVERR", "Console Warning: $msg at ${cm.sourceId()}:${cm.lineNumber()}")
                    }

                    if (msg.startsWith("[LSP-STATUS]")) {
                        // Offload parsing to background thread to avoid blocking UI
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                            try {
                                // Format: [LSP-STATUS] line:1 col:1 err:0 warn:0
                                val parts = msg.removePrefix("[LSP-STATUS] ").split(" ")
                                var line = 1
                                var col = 1
                                var err = 0
                                var warn = 0
                                
                                parts.forEach { part ->
                                    when {
                                        part.startsWith("line:") -> line = part.substringAfter("line:").toIntOrNull() ?: 1
                                        part.startsWith("col:") -> col = part.substringAfter("col:").toIntOrNull() ?: 1
                                        part.startsWith("err:") -> err = part.substringAfter("err:").toIntOrNull() ?: 0
                                        part.startsWith("warn:") -> warn = part.substringAfter("warn:").toIntOrNull() ?: 0
                                    }
                                }
                                
                                // Update EditorSession cursor position for backspace guard
                                this@EditorSession.updateCursorPosition(line, col)
                                
                                viewModel.updateCursorPosition(line, col)
                                viewModel.updateDiagnostics(err, warn)
                            } catch (e: Exception) {
                                Log.e("WEBVERR", "Failed to parse LSP status: $msg", e)
                            }
                        }
                        return false
                    }
                    
                    if (msg.startsWith("[LSP-DIAGS]")) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                            try {
                                val jsonStr = msg.removePrefix("[LSP-DIAGS] ")
                                val jsonArray = org.json.JSONArray(jsonStr)
                                val diagnostics = mutableListOf<com.bsbeetle.bananaspaghetti.viewmodel.DiagnosticItem>()
                                
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    diagnostics.add(
                                        com.bsbeetle.bananaspaghetti.viewmodel.DiagnosticItem(
                                            message = obj.optString("msg", "Unknown"),
                                            line = obj.optInt("line", 1),
                                            column = obj.optInt("col", 1),
                                            severity = obj.optString("sev", "info")
                                        )
                                    )
                                }
                                
                                viewModel.updateDiagnosticsList(diagnostics)
                            } catch (e: Exception) {
                                Log.e("WEBVERR", "Failed to parse LSP diagnostics: $msg", e)
                            }
                        }
                        return false
                    }
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }
    }
>>>>>>> bce1bd3c88405ca2ba91e3f0dba45b8bbaf33cf5
}
