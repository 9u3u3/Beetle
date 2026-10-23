package com.bsbeetle.bananaspaghetti.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.webkit.ConsoleMessage
import com.bsbeetle.bananaspaghetti.utils.UrlResolver
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.lifecycle.AndroidViewModel
import com.bsbeetle.bananaspaghetti.ui.browser.DevToolsInjector
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Viewport mode enum for mobile/desktop view switching
enum class ViewportMode {
    MOBILE,
    DESKTOP
}

data class WebViewUiState(
        val url: String = "",
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
        val isLoading: Boolean = false,
        val loadProgress: Int = 0,
        val isErudaInjected: Boolean = false,
        val viewportMode: ViewportMode = ViewportMode.MOBILE
)

class DevWebViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        // User agents for mobile and desktop mode
        private const val MOBILE_USER_AGENT =
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val DESKTOP_USER_AGENT =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

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
        // Configure WebView settings for flawless browsing
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true // Deprecated but still needed for compatibility
            allowFileAccess = true
            allowContentAccess = true

            // Enhanced settings for better browsing experience
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)

            // Performance and media settings
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true

            // Cache and storage
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

            // Mixed content mode for modern sites
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // Set initial user agent (mobile by default)
            userAgentString = MOBILE_USER_AGENT
        }

        // Set WebViewClient
        webView.webViewClient =
                object : WebViewClient() {
                    override fun onPageStarted(
                            view: android.webkit.WebView?,
                            url: String?,
                            favicon: Bitmap?
                    ) {
                        _uiState.value =
                                _uiState.value.copy(
                                        isLoading = true,
                                        url = url ?: "",
                                        isErudaInjected = false
                                )
                    }

                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        _uiState.value =
                                _uiState.value.copy(
                                        isLoading = false,
                                        canGoBack = webView.canGoBack(),
                                        canGoForward = webView.canGoForward(),
                                        url = url ?: ""
                                )

                        // Apply viewport meta tag for proper scaling based on mode
                        applyViewportSettings()

                        // Inject dev tools
                        injectDevTools()
                    }

                    override fun onReceivedError(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                    ) {
                        Log.e(
                                "DevBrowser",
                                "Error loading: ${request?.url}, error: ${error?.description}"
                        )
                    }
                }

        // Set WebChromeClient
        webView.webChromeClient =
                object : WebChromeClient() {
                    override fun onProgressChanged(
                            view: android.webkit.WebView?,
                            newProgress: Int
                    ) {
                        _uiState.value =
                                _uiState.value.copy(
                                        loadProgress = newProgress,
                                        isLoading = newProgress < 100
                                )
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d(
                                    "WebViewConsole",
                                    "[${it.messageLevel()}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})"
                            )
                        }
                        return true
                    }
                }
    }

    /** Toggle between mobile and desktop viewport modes */
    fun toggleViewportMode() {
        val currentMode = _uiState.value.viewportMode
        val newMode =
                if (currentMode == ViewportMode.MOBILE) ViewportMode.DESKTOP
                else ViewportMode.MOBILE
        setViewportMode(newMode)
    }

    /** Set a specific viewport mode */
    fun setViewportMode(mode: ViewportMode) {
        webView?.let { wv ->
            // Update user agent based on mode
            val userAgent =
                    when (mode) {
                        ViewportMode.MOBILE -> MOBILE_USER_AGENT
                        ViewportMode.DESKTOP -> DESKTOP_USER_AGENT
                    }

            wv.settings.userAgentString = userAgent

            // Update state
            _uiState.value = _uiState.value.copy(viewportMode = mode)

            // Apply viewport settings via JavaScript
            applyViewportSettings()

            // Reload to apply changes
            val currentUrl = wv.url
            if (currentUrl != null) {
                wv.reload()
            }

            Log.d("DevBrowser", "Viewport mode changed to: $mode")
        }
    }

    /** Apply viewport meta tag settings based on current mode */
    private fun applyViewportSettings() {
        webView?.let { wv ->
            val viewportScript =
                    when (_uiState.value.viewportMode) {
                        ViewportMode.MOBILE ->
                                """
                    (function() {
                        var viewport = document.querySelector('meta[name="viewport"]');
                        if (!viewport) {
                            viewport = document.createElement('meta');
                            viewport.name = 'viewport';
                            document.head.appendChild(viewport);
                        }
                        viewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
                    })();
                """.trimIndent()
                        ViewportMode.DESKTOP ->
                                """
                    (function() {
                        var viewport = document.querySelector('meta[name="viewport"]');
                        if (!viewport) {
                            viewport = document.createElement('meta');
                            viewport.name = 'viewport';
                            document.head.appendChild(viewport);
                        }
                        viewport.content = 'width=1280, initial-scale=0.25, maximum-scale=5.0, user-scalable=yes';
                    })();
                """.trimIndent()
                    }
            wv.evaluateJavascript(viewportScript, null)
        }
    }

    fun loadUrl(url: String) {
        webView?.loadUrl(UrlResolver.resolve(url))
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
            _uiState.value =
                    _uiState.value.copy(
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
            wv.postDelayed(
                    {
                        wv.evaluateJavascript(DevToolsInjector.LATE_INJECTION) { result ->
                            Log.d("DevTools", "Late injection result: $result")
                            _uiState.value = _uiState.value.copy(isErudaInjected = true)
                        }
                    },
                    500
            )
        }
    }

    fun toggleDevTools(show: Boolean) {
        webView?.let { wv ->
            if (_uiState.value.isErudaInjected) {
                // Eruda already loaded, just toggle
                wv.evaluateJavascript(if (show) "eruda.show();" else "eruda.hide();", null)
            } else {
                // Force injection if not loaded
                Log.d("DevTools", "Forcing Eruda injection...")
                wv.evaluateJavascript(DevToolsInjector.EARLY_INJECTION) {
                    wv.postDelayed(
                            {
                                wv.evaluateJavascript(DevToolsInjector.LATE_INJECTION) {
                                    _uiState.value = _uiState.value.copy(isErudaInjected = true)
                                    if (show) {
                                        wv.evaluateJavascript("eruda.show();", null)
                                    }
                                }
                            },
                            500
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webView = null
    }
}
