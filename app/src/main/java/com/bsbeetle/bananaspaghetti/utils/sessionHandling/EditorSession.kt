package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bsbeetle.bananaspaghetti.utils.AppConfig
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorSession(context: Context, name: String = "Editor") :
        Session(context.applicationContext, SessionType.EDITOR, name) {

    private var currentFilePath: String? = null

    // Observable state for UI to show loading until editor is ready
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // Project root folder for the editor session
    var projectRoot: androidx.documentfile.provider.DocumentFile? = null
    
    // Terminal sessions linked to this editor session (bottom panel terminals)
    // Using mutableStateListOf so Compose can observe changes
    val linkedTerminals = androidx.compose.runtime.mutableStateListOf<TerminalSession>()

    // Track cursor position for backspace guard (updated by LSP-STATUS parsing)
    var cursorLine: Int = 1
        private set
    var cursorColumn: Int = 1
        private set

    // Content preservation: save editor content when navigating away
    private var savedContent: String? = null
    private var needsRestore: Boolean = false

    // Reference to BufferViewModel for dirty state tracking
    private var bufferViewModel: com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel? = null

    fun setBufferViewModel(viewModel: com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel) {
        this.bufferViewModel = viewModel
        android.util.Log.d("EditorSession", "BufferViewModel set")
    }

    fun updateCursorPosition(line: Int, col: Int) {
        cursorLine = line
        cursorColumn = col
    }

    /**
     * Save the current editor content before navigating away. Call this when leaving the editor
     * screen.
     */
    fun saveContent() {
        view?.evaluateJavascript("window.code_") { result ->
            if (result != null && result != "null" && result != "\"\"") {
                savedContent =
                        result.removeSurrounding("\"")
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
     * Restore saved content after navigating back to the editor. Call this when the editor screen
     * is shown again.
     */
    fun restoreContent() {
        if (needsRestore && savedContent != null) {
            val content = savedContent!!
            val jsCommand =
                    """
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
        view =
                CustomWebView(context.applicationContext, null).apply {
                    session = this@EditorSession
                    // Make WebView completely invisible during creation to prevent grey flash
                    visibility = android.view.View.INVISIBLE
                    alpha = 0f
                    // Set dark background as fallback
                    setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
                    WebView.setWebContentsDebuggingEnabled(true)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowUniversalAccessFromFileURLs =
                                true // Required for dynamic imports from file://
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

                    // Add JavaScript interface for dirty state tracking
                    addJavascriptInterface(
                            object {
                                @android.webkit.JavascriptInterface
                                fun onContentChanged() {
                                    android.util.Log.d(
                                            "EditorSession",
                                            "✓✓✓ JS: onContentChanged called ✓✓✓"
                                    )
                                    android.util.Log.d(
                                            "EditorSession",
                                            "BufferViewModel is null? ${bufferViewModel == null}"
                                    )
                                    android.util.Log.d(
                                            "EditorSession",
                                            "CurrentFile is null? ${bufferViewModel?.currentFile?.value == null}"
                                    )

                                    // MUST run on main thread for Compose state updates
                                    view?.post {
                                        bufferViewModel?.currentFile?.value?.let { file ->
                                            bufferViewModel?.markAsDirty(file.uri.toString())
                                            android.util.Log.d(
                                                    "EditorSession",
                                                    "✓ Marked dirty on main thread: ${file.name}"
                                            )
                                        }
                                                ?: android.util.Log.e(
                                                        "EditorSession",
                                                        "✗ Could not mark dirty - currentFile is null"
                                                )
                                    }
                                }
                            },
                            "AndroidEditor"
                    )

                    // Add JavaScript interface to signal when editor is ready
                    addJavascriptInterface(
                            object {
                                @android.webkit.JavascriptInterface
                                fun onReady() {
                                    android.util.Log.d(
                                            "EditorSession",
                                            "✓ JS: onReady called - CM6 initialized"
                                    )
                                    _isReady.value = true
                                    
                                    // Apply saved editor settings
                                    view?.post {
                                        // Load preferences
                                        val prefs = com.bsbeetle.bananaspaghetti.utils.preferences.EditorPreferences(context)
                                        
                                        // Apply theme
                                        view?.evaluateJavascript(
                                            "if(window.setTheme) window.setTheme('${prefs.theme}');",
                                            null
                                        )
                                        
                                        // Apply word wrap
                                        view?.evaluateJavascript(
                                            "if(window.setWordWrap) window.setWordWrap(${prefs.wordWrap});",
                                            null
                                        )
                                        
                                        // Apply line numbers
                                        view?.evaluateJavascript(
                                            "if(window.setLineNumbers) window.setLineNumbers(${prefs.lineNumbers});",
                                            null
                                        )
                                        
                                        // Apply tab size
                                        view?.evaluateJavascript(
                                            "if(window.setTabSize) window.setTabSize(${prefs.tabSize});",
                                            null
                                        )
                                        
                                        // Apply relative line numbers
                                        view?.evaluateJavascript(
                                            "if(window.setRelativeLineNumbers) window.setRelativeLineNumbers(${prefs.relativeLineNumbers});",
                                            null
                                        )
                                        
                                        // Apply indent guides
                                        view?.evaluateJavascript(
                                            "if(window.setIndentGuides) window.setIndentGuides(${prefs.indentGuides});",
                                            null
                                        )
                                        
                                        // Apply auto save
                                        view?.evaluateJavascript(
                                            "if(window.setAutoSave) window.setAutoSave(${prefs.autoSave}, ${prefs.autoSaveDelay});",
                                            null
                                        )
                                        
                                        android.util.Log.d("EditorSession", "✓ Applied saved editor settings")
                                        
                                        // Make WebView visible
                                        view?.visibility = android.view.View.VISIBLE
                                        view?.alpha = 1f
                                    }
                                }
                            },
                            "AndroidEditorReady"
                    )

                    // Add JavaScript interface for auto-save functionality
                    addJavascriptInterface(
                            object {
                                @android.webkit.JavascriptInterface
                                fun saveFile() {
                                    android.util.Log.d(
                                            "EditorSession",
                                            "✓ JS: AndroidInterface.saveFile() called - auto-save triggered"
                                    )
                                    // Post to main thread and trigger save
                                    view?.post {
                                        val currentFile = bufferViewModel?.currentFile?.value
                                        if (currentFile != null) {
                                            val uri = currentFile.uri
                                            // Get content from editor
                                            view?.evaluateJavascript(
                                                    "window.editor?.state?.doc?.toString() || window.code_ || ''"
                                            ) { result ->
                                                if (result != null && result != "null" && result != "\"\"") {
                                                    try {
                                                        var content = result.removeSurrounding("\"")
                                                                .replace("\\n", "\n")
                                                                .replace("\\t", "\t")
                                                                .replace("\\r", "\r")
                                                                .replace("\\\"", "\"")
                                                                .replace("\\\\", "\\")
                                                        
                                                        // Unescape Unicode sequences
                                                        val unicodePattern = Regex("\\\\u([0-9a-fA-F]{4})")
                                                        content = unicodePattern.replace(content) { matchResult ->
                                                            val hexCode = matchResult.groupValues[1]
                                                            hexCode.toInt(16).toChar().toString()
                                                        }
                                                        
                                                        // Save to file
                                                        val resolver = context.contentResolver
                                                        val out = resolver.openOutputStream(uri, "wt")
                                                        if (out != null) {
                                                            java.io.OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                                                                writer.write(content)
                                                                writer.flush()
                                                            }
                                                            // Clear dirty state
                                                            bufferViewModel?.markAsClean(uri.toString())
                                                            android.util.Log.d(
                                                                    "EditorSession",
                                                                    "✓ Auto-saved and marked clean: ${currentFile.name}"
                                                            )
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.e(
                                                                "EditorSession",
                                                                "Auto-save failed",
                                                                e
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            android.util.Log.w(
                                                    "EditorSession",
                                                    "Auto-save skipped - no current file"
                                            )
                                        }
                                    }
                                }
                            },
                            "AndroidInterface"
                    )

                    loadUrl(AppConfig.EDITOR_URL)

                    // Inject listener immediately after load
                    post {
                        postDelayed(
                                {
                                    android.util.Log.d(
                                            "EditorSession",
                                            "Injecting MutationObserver for content changes..."
                                    )
                                    evaluateJavascript(
                                            """
                        (function() {
                            // Legacy MutationObserver removed.
                            // Content change detection is now handled by event listeners in the WebViewClient.onPageFinished
                            console.log('✓ JS: Legacy MutationObserver skipped');
                        })();
                    """.trimIndent(),
                                            null
                                    )
                                },
                                2000
                        )
                    }
                    post {
                        val isPortrait =
                                resources.configuration.orientation ==
                                        Configuration.ORIENTATION_PORTRAIT
                        if (isPortrait) requestFocus() else clearFocus()
                    }
                    webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val isPortrait =
                                            resources.configuration.orientation ==
                                                    Configuration.ORIENTATION_PORTRAIT
                                    if (isPortrait) {
                                        view?.evaluateJavascript(
                                                "if (window.editor) { window.editor.focus(); }",
                                                null
                                        )
                                    }
                                    // Inject dynamic inputmode handling (One-time check + Resize
                                    // listener)
                                    view?.evaluateJavascript(
                                            """
                        (function() {
                            function updateInputMode() {
                                const content = document.querySelector('.cm-content');
                                if (!content) return;
                                // Use screen.orientation API for reliable landscape detection
                                // This doesn't get fooled by editor size changes when custom keyboard grows
                                const orientation = screen.orientation ? screen.orientation.type : '';
                                const isLandscape = orientation.includes('landscape') || 
                                    (window.screen && window.screen.width > window.screen.height);
                                
                                // Check if useLandscapeIme is enabled (set from Kotlin)
                                // If enabled, use 'text' mode even in landscape to allow system IME
                                const useLandscapeIme = window.useLandscapeIme || false;
                                const mode = (isLandscape && !useLandscapeIme) ? 'none' : 'text';
                                
                                if (content.getAttribute('inputmode') !== mode) {
                                    content.setAttribute('inputmode', mode);
                                    console.log('[Editor] inputmode set to: ' + mode + ' (landscape: ' + isLandscape + ', useLandscapeIme: ' + useLandscapeIme + ')');
                                }
                            }
                            window.addEventListener('resize', updateInputMode);
                            // Also listen for orientation change event
                            window.addEventListener('orientationchange', updateInputMode);
                            if (screen.orientation) {
                                screen.orientation.addEventListener('change', updateInputMode);
                            }
                            
                            // Expose updateInputMode globally so it can be called when setting changes
                            window.updateInputMode = updateInputMode;
                            
                            // Initial check
                            updateInputMode();
                            
                            // Use MutationObserver ONLY for inputmode attribute
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
                        
                        // CONTENT CHANGE DETECTION
                        // Now handled internally by CodeMirror's updateListener in editor.js
                        // This ensures we only catch actual content changes and respect isProgrammaticChange
                    """.trimIndent(),
                                            null
                                    )

                                    // Apply pending file path if any
                                    currentFilePath?.let { setFilePath(it) }

                                    // Start Memory Monitor
                                    startMemoryMonitor()

                                    // Fallback: Check if isEditorReady is set after a delay
                                    // This handles cases where the JS interface might not work
                                    view?.postDelayed(
                                            {
                                                if (!_isReady.value) {
                                                    Log.d(
                                                            "EditorSession",
                                                            "Fallback: Checking window.isEditorReady..."
                                                    )
                                                    view?.evaluateJavascript(
                                                            "window.isEditorReady === true"
                                                    ) { result ->
                                                        Log.d(
                                                                "EditorSession",
                                                                "Fallback: isEditorReady = $result"
                                                        )
                                                        if (result == "true" && !_isReady.value) {
                                                            Log.d(
                                                                    "EditorSession",
                                                                    "Fallback: Marking as ready via polling"
                                                            )
                                                            _isReady.value = true
                                                            view?.visibility =
                                                                    android.view.View.VISIBLE
                                                            view?.alpha = 1f
                                                        }
                                                    }
                                                }
                                            },
                                            1500
                                    ) // Check after 1.5s

                                    // Ultimate fallback: Force ready state after 3 seconds
                                    // This prevents the editor from being stuck on loading forever
                                    view?.postDelayed(
                                            {
                                                if (!_isReady.value) {
                                                    Log.w(
                                                            "EditorSession",
                                                            "Ultimate fallback: Forcing isReady after 3s timeout"
                                                    )
                                                    _isReady.value = true
                                                    view?.visibility = android.view.View.VISIBLE
                                                    view?.alpha = 1f
                                                }
                                            },
                                            3000
                                    ) // Force after 3s
                                }

                                override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                ): Boolean {
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
        val monitor =
                object : Runnable {
                    override fun run() {
                        try {
                            val am =
                                    context.getSystemService(Context.ACTIVITY_SERVICE) as
                                            android.app.ActivityManager
                            val memInfo = android.app.ActivityManager.MemoryInfo()
                            am.getMemoryInfo(memInfo)

                            val runtime = Runtime.getRuntime()
                            val usedJavaHeap =
                                    (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                            val availMem = memInfo.availMem / 1024 / 1024
                            val threshold = memInfo.threshold / 1024 / 1024

                            // Send stats to WebView
                            val jsonStats =
                                    """
                        {
                            "availMem": $availMem,
                            "threshold": $threshold,
                            "lowMemory": ${memInfo.lowMemory},
                            "javaHeap": $usedJavaHeap
                        }
                    """.trimIndent()

                            view?.evaluateJavascript(
                                    "if(window.updateDebugStats) window.updateDebugStats($jsonStats);",
                                    null
                            )

                            view?.postDelayed(this, 2000) // Update every 2 seconds
                        } catch (e: Exception) {
                            Log.e("WEBVERR", "Memory monitor failed", e)
                        }
                    }
                }
        view?.post(monitor)
    }

    override fun destroy() {
        // view?.let { WebViewPool.recycle(it) }
        view?.destroy()
        view = null
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

    fun setRootUri(rootUri: String) {
        view?.evaluateJavascript("if(window.setRootUri) window.setRootUri('$rootUri');", null)
    }

    fun clearLanguage() {
        view?.evaluateJavascript("if(window.clearLanguage) window.clearLanguage();", null)
    }

    fun performEditorAction(action: String) {
        view?.evaluateJavascript("if(window.$action) window.$action();", null)
    }

    // Overload for actions with count parameter (e.g., batched backspace)
    fun performEditorAction(action: String, count: Int) {
        view?.evaluateJavascript("if(window.$action) window.$action($count);", null)
    }

    fun performNavigation(type: com.bsbeetle.bananaspaghetti.model.KeyType) {
        val action =
                when (type) {
                    com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_UP -> "moveUp"
                    com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_DOWN -> "moveDown"
                    com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_LEFT -> "moveLeft"
                    com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_RIGHT -> "moveRight"
                    com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_HOME -> "moveHome"
                    com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_END -> "moveEnd"
                    com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_PGUP -> "pageUp"
                    com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_PGDN -> "pageDown"
                    else -> return
                }
        view?.evaluateJavascript("if(window.$action) window.$action();", null)
    }

    fun setLspViewModel(viewModel: com.bsbeetle.bananaspaghetti.viewmodel.LspViewModel) {
        // Use WebChromeClient to intercept console messages instead of JavascriptInterface
        // This avoids the need to reload the WebView which wipes content
        view?.webChromeClient =
                object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(
                            consoleMessage: android.webkit.ConsoleMessage?
                    ): Boolean {
                        consoleMessage?.let { cm ->
                            val msg = cm.message()
                            val level = cm.messageLevel()

                            // Log all console messages with WEBVERR if they are errors or warnings,
                            // or if requested
                            if (level == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                                Log.e(
                                        "WEBVERR",
                                        "Console Error: $msg at ${cm.sourceId()}:${cm.lineNumber()}"
                                )
                            } else if (level == android.webkit.ConsoleMessage.MessageLevel.WARNING
                            ) {
                                Log.w(
                                        "WEBVERR",
                                        "Console Warning: $msg at ${cm.sourceId()}:${cm.lineNumber()}"
                                )
                            }

                            if (msg.startsWith("[LSP-STATUS]")) {
                                // Offload parsing to background thread to avoid blocking UI
                                kotlinx.coroutines.CoroutineScope(
                                                kotlinx.coroutines.Dispatchers.Default
                                        )
                                        .launch {
                                            try {
                                                // Format: [LSP-STATUS] line:1 col:1 err:0 warn:0
                                                val parts =
                                                        msg.removePrefix("[LSP-STATUS] ").split(" ")
                                                var line = 1
                                                var col = 1
                                                var err = 0
                                                var warn = 0

                                                parts.forEach { part ->
                                                    when {
                                                        part.startsWith("line:") ->
                                                                line =
                                                                        part.substringAfter("line:")
                                                                                .toIntOrNull()
                                                                                ?: 1
                                                        part.startsWith("col:") ->
                                                                col =
                                                                        part.substringAfter("col:")
                                                                                .toIntOrNull()
                                                                                ?: 1
                                                        part.startsWith("err:") ->
                                                                err =
                                                                        part.substringAfter("err:")
                                                                                .toIntOrNull()
                                                                                ?: 0
                                                        part.startsWith("warn:") ->
                                                                warn =
                                                                        part.substringAfter("warn:")
                                                                                .toIntOrNull()
                                                                                ?: 0
                                                    }
                                                }

                                                // Update EditorSession cursor position for
                                                // backspace guard
                                                this@EditorSession.updateCursorPosition(line, col)

                                                viewModel.updateCursorPosition(line, col)
                                                viewModel.updateDiagnostics(err, warn)
                                                android.util.Log.d(
                                                        "LspStatus",
                                                        "Updated: line=$line col=$col err=$err warn=$warn"
                                                )
                                            } catch (e: Exception) {
                                                Log.e(
                                                        "WEBVERR",
                                                        "Failed to parse LSP status: $msg",
                                                        e
                                                )
                                            }
                                        }
                                return false
                            }

                            if (msg.startsWith("[LSP-DIAGS]")) {
                                kotlinx.coroutines.CoroutineScope(
                                                kotlinx.coroutines.Dispatchers.Default
                                        )
                                        .launch {
                                            try {
                                                val jsonStr = msg.removePrefix("[LSP-DIAGS] ")
                                                val jsonArray = org.json.JSONArray(jsonStr)
                                                val diagnostics =
                                                        mutableListOf<
                                                                com.bsbeetle.bananaspaghetti.viewmodel.DiagnosticItem>()

                                                for (i in 0 until jsonArray.length()) {
                                                    val obj = jsonArray.getJSONObject(i)
                                                    diagnostics.add(
                                                            com.bsbeetle.bananaspaghetti.viewmodel
                                                                    .DiagnosticItem(
                                                                            message =
                                                                                    obj.optString(
                                                                                            "msg",
                                                                                            "Unknown"
                                                                                    ),
                                                                            line =
                                                                                    obj.optInt(
                                                                                            "line",
                                                                                            1
                                                                                    ),
                                                                            column =
                                                                                    obj.optInt(
                                                                                            "col",
                                                                                            1
                                                                                    ),
                                                                            severity =
                                                                                    obj.optString(
                                                                                            "sev",
                                                                                            "info"
                                                                                    )
                                                                    )
                                                    )
                                                }

                                                viewModel.updateDiagnosticsList(diagnostics)
                                            } catch (e: Exception) {
                                                Log.e(
                                                        "WEBVERR",
                                                        "Failed to parse LSP diagnostics: $msg",
                                                        e
                                                )
                                            }
                                        }
                                return false
                            }

                            if (msg.startsWith("[LSP-SYMBOLS]")) {
                                kotlinx.coroutines.CoroutineScope(
                                                kotlinx.coroutines.Dispatchers.Default
                                        )
                                        .launch {
                                            try {
                                                val jsonStr = msg.removePrefix("[LSP-SYMBOLS] ")
                                                val jsonArray = org.json.JSONArray(jsonStr)
                                                val breadcrumbs =
                                                        mutableListOf<
                                                                com.bsbeetle.bananaspaghetti.viewmodel.BreadcrumbItem>()

                                                for (i in 0 until jsonArray.length()) {
                                                    val obj = jsonArray.getJSONObject(i)
                                                    breadcrumbs.add(
                                                            com.bsbeetle.bananaspaghetti.viewmodel
                                                                    .BreadcrumbItem(
                                                                            name =
                                                                                    obj.optString(
                                                                                            "name",
                                                                                            "Unknown"
                                                                                    ),
                                                                            kind =
                                                                                    obj.optInt(
                                                                                            "kind",
                                                                                            0
                                                                                    ),
                                                                            detail =
                                                                                    obj.optString(
                                                                                            "detail",
                                                                                            ""
                                                                                    )
                                                                    )
                                                    )
                                                }

                                                viewModel.updateBreadcrumbs(breadcrumbs)
                                            } catch (e: Exception) {
                                                Log.e(
                                                        "WEBVERR",
                                                        "Failed to parse LSP symbols: $msg",
                                                        e
                                                )
                                            }
                                        }
                                return false
                            }
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
    }
}
