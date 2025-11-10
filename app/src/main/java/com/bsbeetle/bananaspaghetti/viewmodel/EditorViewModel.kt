package com.bsbeetle.bananaspaghetti.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.Configuration
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import com.bsbeetle.bananaspaghetti.data.Ic
import com.bsbeetle.bananaspaghetti.utils.AssetServer
import com.bsbeetle.bananaspaghetti.utils.NoKeyboardWebView

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    @SuppressLint("StaticFieldLeak")
    var webView: NoKeyboardWebView? = null
    private var server: AssetServer? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun init() {
        if (webView != null) return  // already created

        // Start AssetServer on port 8080
        if (server == null) {
            server = AssetServer(getApplication(), 8080)
            server?.start()
        }

        webView = NoKeyboardWebView(getApplication()).apply {
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

            // Wait for the WebView to actually be ready
            post {
                val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

                if (isPortrait) {
                    // Only request focus in portrait mode
                    requestFocus()
                } else {
                    // In landscape, don't request focus
                    clearFocus()
                }
            }
        }
    }

    // Add this method to update focus based on orientation
    fun updateOrientationFocus(isPortrait: Boolean) {
        webView?.post {
            if (isPortrait) {
                webView?.isFocusable = true
                webView?.isFocusableInTouchMode = true
                webView?.requestFocus()
            } else {
                webView?.isFocusable = false
                webView?.isFocusableInTouchMode = false
                webView?.clearFocus()
            }
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