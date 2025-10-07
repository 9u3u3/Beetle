//package com.bsbeetle.bananaspaghetti.utils
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.util.Log
//import android.webkit.WebSettings
//import android.webkit.WebView
//import android.webkit.WebViewClient
//import androidx.compose.ui.platform.LocalGraphicsContext
//
//@SuppressLint("StaticFieldLeak")
//object EditorWebView {
//
//    var webView: WebView? = null
//    @SuppressLint("SetJavaScriptEnabled")
//
//    fun init(context: Context) {
//
//        Log.wtf("EDITORWEBVIEW", "Init called")
//
//        if(webView == null) {
//            webView = WebView(context.applicationContext).apply {
//
//                    WebView.setWebContentsDebuggingEnabled(true)
//
//                    settings.apply {
//                        javaScriptEnabled = true
//                        domStorageEnabled = true
//                        allowFileAccess = true
//                        cacheMode = WebSettings.LOAD_NO_CACHE
//                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//                        useWideViewPort = true
//                        loadWithOverviewMode = true
//                        setSupportZoom(false)
//
//
//                    }
//
//                    loadUrl("file:///android_asset/editor/index.html")
//
//            }
//        }
//    }
//
//    fun destroy() {
//        webView?.destroy()
//        webView = null
//    }
//
//}