package com.bsbeetle.bananaspaghetti.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    @SuppressLint("StaticFieldLeak")
    var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun init() {
        if (webView != null) return  // already created, don't recreate

        Log.wtf("EDITORWEBVIEW", "Init called, creating persistent WebView")

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

            loadUrl("file:///android_asset/editor/index.html")
        }
    }

    override fun onCleared() {
        // Only destroy when ViewModel actually dies (process end)
        webView?.destroy()
        webView = null
        super.onCleared()
    }
}
