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
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.WebViewPool

class EditorSession(context: Context, name: String = "Editor") : Session(context.applicationContext, SessionType.EDITOR, name) {

    private var currentFilePath: String? = null

    init {
        createView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(): CustomWebView {
        if (view != null) return view!!
        view = WebViewPool.get(context.applicationContext).apply {
            session = this@EditorSession
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
                    
                    // Apply pending file path if any
                    currentFilePath?.let { setFilePath(it) }
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    Log.d("EditorSession", "WebView Crashed")
                    destroy()
                    return true
                }
            }
        }
        return view!!
    }

    override fun destroy() {
        view?.let { WebViewPool.recycle(it) }
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

    fun performEditorAction(action: String) {
        view?.evaluateJavascript("if(window.$action) window.$action();", null)
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
                consoleMessage?.message()?.let { msg ->
                    if (msg.startsWith("[LSP-STATUS]")) {
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
                            
                            viewModel.updateCursorPosition(line, col)
                            viewModel.updateDiagnostics(err, warn)
                        } catch (e: Exception) {
                            Log.e("EditorSession", "Failed to parse LSP status: $msg", e)
                        }
                        return false // Return false to allow the log to appear in Logcat
                    }
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }
    }
}
