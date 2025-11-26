package com.bsbeetle.bananaspaghetti.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.lifecycle.AndroidViewModel
import com.bsbeetle.bananaspaghetti.ui.browser.DevToolsInjector
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WebViewUiState(
    val url: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val loadProgress: Int = 0,
    val isErudaInjected: Boolean = false
)

class DevWebViewModel(app: Application) : AndroidViewModel(app) {

    @SuppressLint("StaticFieldLeak")
    var webView: CustomWebView? = null
        private set

    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    fun setDevWebView(webView: CustomWebView) {
        this.webView = webView
        setupWebView(webView)
    }

    @SuppressLint("SetJavaScriptEnabled", "ObsoleteSdkInt")
    private fun setupWebView(webView: CustomWebView) {
        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true // Deprecated but still needed for compatibility
            allowFileAccess = true
            allowContentAccess = true
        }

        // Set WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: Bitmap?) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    url = url ?: "",
                    isErudaInjected = false
                )
            }

            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    canGoBack = webView.canGoBack(),
                    canGoForward = webView.canGoForward(),
                    url = url ?: ""
                )

                // Inject dev tools
                injectDevTools()
            }
        }

        // Set WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                _uiState.value = _uiState.value.copy(
                    loadProgress = newProgress,
                    isLoading = newProgress < 100
                )
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d("WebViewConsole", "[${it.messageLevel()}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                }
                return true
            }
        }
    }

    fun loadUrl(url: String) {
        var finalUrl = url.trim()
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "https://$finalUrl"
        }
        webView?.loadUrl(finalUrl)
    }

    fun goBack() {
        webView?.goBack()
        updateNavigation()
    }

    fun goForward() {
        webView?.goForward()
        updateNavigation()
    }

    fun reload() {
        webView?.reload()
    }

    fun updateNavigation() {
        webView?.let { wv ->
            _uiState.value = _uiState.value.copy(
                canGoBack = wv.canGoBack(),
                canGoForward = wv.canGoForward(),
                url = wv.url ?: ""
            )
        }
    }

    private fun injectDevTools() {
        webView?.let { wv ->
            // Inject early console capture
            wv.evaluateJavascript(DevToolsInjector.EARLY_INJECTION) { result ->
                Log.d("DevTools", "Early injection result: $result")
            }

            // Inject Eruda after delay
            wv.postDelayed({
                wv.evaluateJavascript(DevToolsInjector.LATE_INJECTION) { result ->
                    Log.d("DevTools", "Late injection result: $result")
                    _uiState.value = _uiState.value.copy(isErudaInjected = true)
                }
            }, 500)
        }
    }
    fun toggleDevTools(show: Boolean) {
        webView?.let { wv ->
            if (_uiState.value.isErudaInjected) {
                // Eruda already loaded, just toggle
                wv.evaluateJavascript(
                    if (show) "eruda.show();" else "eruda.hide();",
                    null
                )
            } else {
                // Force injection if not loaded
                Log.d("DevTools", "Forcing Eruda injection...")
                wv.evaluateJavascript(DevToolsInjector.EARLY_INJECTION) {
                    wv.postDelayed({
                        wv.evaluateJavascript(DevToolsInjector.LATE_INJECTION) {
                            _uiState.value = _uiState.value.copy(isErudaInjected = true)
                            if (show) {
                                wv.evaluateJavascript("eruda.show();", null)
                            }
                        }
                    }, 500)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webView = null
    }
}