package com.bsbeetle.bananaspaghetti.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import com.bsbeetle.bananaspaghetti.utils.AssetServer

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    @SuppressLint("StaticFieldLeak")
    var webView: WebView? = null

    private var server: AssetServer? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun init() {
        if (webView != null) return  // already created

        // Start AssetServer on port 8080
        if (server == null) {
            server = AssetServer(getApplication(), 8080)
            server?.start()
        }

        webView = WebView(getApplication()).apply {
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

            // Load via HTTP server instead of directly from assets
            loadUrl("http://localhost:8080/index.html")
        }
    }

    override fun onCleared() {
        // Destroy WebView
        webView?.destroy()
        webView = null

        // Stop server when ViewModel is cleared
        server?.stop()
        server = null

        super.onCleared()
    }
}