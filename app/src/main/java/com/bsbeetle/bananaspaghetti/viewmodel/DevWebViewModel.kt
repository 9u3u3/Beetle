package com.bsbeetle.bananaspaghetti.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.AndroidViewModel
import com.bsbeetle.bananaspaghetti.ui.browser.DevBridge
import com.bsbeetle.bananaspaghetti.ui.browser.DevToolsInjector
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import io.ktor.http.ContentType

class DevWebViewModel(app: Application) : AndroidViewModel(app) {
    // Might have to change from CustomWebView to the normal WebView
    @SuppressLint("StaticFieldLeak")
    var webView: CustomWebView? = null
}