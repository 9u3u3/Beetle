package com.bsbeetle.bananaspaghetti.viewmodel

// Editor view model is only One in the whole app so it should switch up between all the webviews
// Editor view model is made to only keep the webview persistent

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bsbeetle.bananaspaghetti.utils.assetHandling.AssetServer
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

/*TODO*/
// Implementation of the sessions
// Write functions to swap the webViews on call

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    init { AssetServer(getApplication(), 8080).start() } // Start the server for editor files
    @SuppressLint("StaticFieldLeak")
    var webView: CustomWebView? = null

    // Call this value explicitly as there can be any type of session in the active session
    fun setEditorWebView(webView: CustomWebView) {this.webView = webView}
}