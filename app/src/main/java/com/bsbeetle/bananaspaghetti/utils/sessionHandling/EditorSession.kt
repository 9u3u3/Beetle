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
                    // Inject dynamic inputmode handling
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
                            // Check periodically or on load
                            const checkExist = setInterval(function() {
                               const content = document.querySelector('.cm-content');
                               if (content) {
                                  clearInterval(checkExist);
                                  updateInputMode();
                                  // Optional: Observer if CodeMirror fights back
                                  const observer = new MutationObserver(() => updateInputMode());
                                  observer.observe(content, { attributes: true, attributeFilter: ['inputmode'] });
                               }
                            }, 100);
                        })();
                    """.trimIndent(), null)
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

    fun performEditorAction(action: String) {
        view?.evaluateJavascript("if(window.$action) window.$action();", null)
    }
}
