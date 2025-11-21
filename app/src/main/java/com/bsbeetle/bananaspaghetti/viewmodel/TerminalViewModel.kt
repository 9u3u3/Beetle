package com.bsbeetle.bananaspaghetti.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.AndroidViewModel
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

/*TODO*/
// Implementation of the sessions things to this code, this code is jumbled and wont work
// Unless you properly connect it to the Session class
// Same for the webviews if not fixed yet
// take care of the destroy thing later (resource handling take care later please)

class TerminalViewModel(app: android.app.Application) : AndroidViewModel(app) {
    @SuppressLint("StaticFieldLeak")
    var webView: CustomWebView? = null
    fun setTerminalWebView(webView: CustomWebView) {this.webView = webView}
}