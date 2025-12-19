package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bsbeetle.bananaspaghetti.utils.python.PythonExecutor
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NotebookSession - A special session type for Python notebooks.
 * Uses notebook.html instead of editor.html and provides Python execution capabilities.
 */
class NotebookSession(context: Context, name: String = "Notebook") :
        Session(context.applicationContext, SessionType.EDITOR, name) {

    companion object {
        private const val TAG = "NotebookSession"
        // URL matches the asset serving pattern used by editor (localhost:8080 serves /editor folder)
        private const val NOTEBOOK_URL = "http://localhost:8080/notebook.html"
    }

    private var currentFilePath: String? = null
    private var pythonExecutor: PythonExecutor? = null

    // Observable state for UI
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // projectRoot is inherited from Session base class

    init {
        // Initialize Python executor
        pythonExecutor = PythonExecutor(context.applicationContext)
        pythonExecutor?.initialize()
        
        // Set up callbacks from Python executor
        pythonExecutor?.onExecutionComplete = { cellId, output, outputType, executionTime ->
            sendResultToWebView(cellId, output, outputType, executionTime)
        }
        
        pythonExecutor?.onPlotGenerated = { cellId, base64Data ->
            sendPlotToWebView(cellId, base64Data)
        }
        
        createView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(): CustomWebView {
        if (view != null) return view!!

        view = CustomWebView(context.applicationContext, null).apply {
            session = this@NotebookSession
            visibility = android.view.View.INVISIBLE
            alpha = 0f
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
            
            // Ensure WebView fills the parent
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            WebView.setWebContentsDebuggingEnabled(true)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowUniversalAccessFromFileURLs = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                setSafeBrowsingEnabled(false)
            }

            // Add JavaScript interface for Python execution
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun runCode(code: String, cellId: String) {
                        Log.d(TAG, "runCode called for cell $cellId")
                        pythonExecutor?.executeCode(code, cellId)
                    }

                    @JavascriptInterface
                    fun requestFileContent(filePath: String) {
                        Log.d(TAG, "requestFileContent: $filePath")
                        loadFileContent(filePath)
                    }
                },
                "AndroidPython"
            )

            // Add JavaScript interface for notebook ready signal
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onReady() {
                        Log.d(TAG, "Notebook JS reported ready")
                        _isReady.value = true
                        
                        // Make WebView visible
                        view?.post {
                            view?.visibility = android.view.View.VISIBLE
                            view?.alpha = 1f
                        }
                        
                        // Load file content if we have a path
                        currentFilePath?.let { loadFileContent(it) }
                    }
                },
                "AndroidNotebookReady"
            )
            
            // Add JavaScript interface for notebook operations (save, etc.)
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun saveContent(content: String) {
                        Log.d(TAG, "saveContent called with ${content.length} chars")
                        saveNotebookContent(content)
                    }
                },
                "AndroidNotebook"
            )

            loadUrl(NOTEBOOK_URL)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Notebook page finished loading")
                    
                    // Make WebView visible immediately
                    view?.visibility = android.view.View.VISIBLE
                    view?.alpha = 1f
                    Log.d(TAG, "WebView made visible")
                    
                    // Set file path if available
                    currentFilePath?.let { path ->
                        view?.evaluateJavascript(
                            "if(window.setFilePath) window.setFilePath('$path');",
                            null
                        )
                    }
                    
                    // Mark as ready immediately
                    _isReady.value = true
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    Log.e(TAG, "WebView Render Process Gone: $detail")
                    view?.destroy()
                    this@NotebookSession.view = null
                    return true
                }
            }
        }

        return view!!
    }

    /**
     * Set the file path and load content into the notebook.
     */
    fun setFilePath(path: String) {
        currentFilePath = path
        view?.evaluateJavascript(
            "if(window.setFilePath) window.setFilePath('$path');",
            null
        )
        loadFileContent(path)
    }

    /**
     * Load file content into the notebook.
     * Handles both content:// URIs and regular file paths.
     */
    private fun loadFileContent(pathOrUri: String) {
        try {
            val content = when {
                // Handle content:// URIs
                pathOrUri.startsWith("content://") -> {
                    val uri = android.net.Uri.parse(pathOrUri)
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                // Handle file:// URIs
                pathOrUri.startsWith("file://") -> {
                    val path = pathOrUri.removePrefix("file://")
                    java.io.File(path).readText()
                }
                // Handle regular file paths
                else -> {
                    val file = java.io.File(pathOrUri)
                    if (file.exists()) file.readText() else null
                }
            }
            
            if (content != null) {
                val escapedContent = content
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                
                view?.post {
                    view?.evaluateJavascript(
                        "if(window.loadContent) window.loadContent('$escapedContent');",
                        null
                    )
                }
                Log.d(TAG, "Loaded content from $pathOrUri (${content.length} chars)")
            } else {
                Log.w(TAG, "Could not read content from: $pathOrUri")
                // Load empty notebook with one cell
                view?.post {
                    view?.evaluateJavascript(
                        "if(window.loadContent) window.loadContent('# %%\\n');",
                        null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading file content: ${e.message}", e)
            // Load empty notebook on error
            view?.post {
                view?.evaluateJavascript(
                    "if(window.loadContent) window.loadContent('# %%\\n');",
                    null
                )
            }
        }
    }
    
    /**
     * Save notebook content back to the file.
     */
    private fun saveNotebookContent(content: String) {
        val pathOrUri = currentFilePath ?: return
        
        try {
            when {
                // Handle content:// URIs
                pathOrUri.startsWith("content://") -> {
                    val uri = android.net.Uri.parse(pathOrUri)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                }
                // Handle file:// URIs
                pathOrUri.startsWith("file://") -> {
                    val path = pathOrUri.removePrefix("file://")
                    java.io.File(path).writeText(content)
                }
                // Handle regular file paths
                else -> {
                    java.io.File(pathOrUri).writeText(content)
                }
            }
            Log.d(TAG, "Saved notebook content to $pathOrUri (${content.length} chars)")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notebook content: ${e.message}", e)
        }
    }

    /**
     * Send Python execution result to the WebView.
     */
    private fun sendResultToWebView(cellId: String, output: String, outputType: String, executionTime: Double) {
        val escapedOutput = output
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        
        view?.post {
            view?.evaluateJavascript(
                "if(window.onPythonResult) window.onPythonResult('$cellId', '$escapedOutput', '$outputType', $executionTime);",
                null
            )
        }
    }

    /**
     * Send plot image to the WebView.
     */
    private fun sendPlotToWebView(cellId: String, base64Data: String) {
        view?.post {
            view?.evaluateJavascript(
                "if(window.onPlotImage) window.onPlotImage('$cellId', '$base64Data');",
                null
            )
        }
    }

    /**
     * Get current notebook content for saving.
     */
    fun getContent(callback: (String) -> Unit) {
        view?.evaluateJavascript("window.getContent ? window.getContent() : ''") { result ->
            val content = result
                ?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\r", "\r")
                ?.replace("\\'", "'")
                ?.replace("\\\\", "\\")
                ?: ""
            callback(content)
        }
    }

    // =========================================
    // KEYBOARD INPUT METHODS
    // =========================================

    /**
     * Insert text at the cursor position in the focused cell's editor.
     */
    fun insertTextToEditor(text: String) {
        val escapedText = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        view?.evaluateJavascript("if(window.insertText) window.insertText('$escapedText');", null)
    }

    /**
     * Perform an editor action (e.g., performBackspace, performEnter, performTab).
     */
    fun performEditorAction(action: String) {
        view?.evaluateJavascript("if(window.$action) window.$action();", null)
    }

    /**
     * Perform navigation action (cursor movement).
     */
    fun performNavigation(type: com.bsbeetle.bananaspaghetti.model.KeyType) {
        val action = when (type) {
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_UP -> "moveUp"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_DOWN -> "moveDown"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_LEFT -> "moveLeft"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_RIGHT -> "moveRight"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_HOME -> "moveHome"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_END -> "moveEnd"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_PGUP -> "pageUp"
            com.bsbeetle.bananaspaghetti.model.KeyType.ACTION_PGDN -> "pageDown"
            else -> null
        }
        action?.let { view?.evaluateJavascript("if(window.$it) window.$it();", null) }
    }

    override fun destroy() {
        Log.d(TAG, "Destroying notebook session")
        pythonExecutor?.destroy()
        pythonExecutor = null
        view?.destroy()
        view = null
        super.destroy()
    }
}
