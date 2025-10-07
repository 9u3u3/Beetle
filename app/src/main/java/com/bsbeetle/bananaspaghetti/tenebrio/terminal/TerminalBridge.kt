package com.bsbeetle.bananaspaghetti.tenebrio.terminal

import android.webkit.JavascriptInterface

//class TerminalBridge(private val onInput: (String) -> Unit) {
//    @JavascriptInterface
//    fun sendInput(input: String) {
//        onInput(input)
//    }
//}
class TerminalBridge(private val onInput: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun sendInput(input: String?) {
        input?.let { onInput(it) }  // safely unwrap null
    }
}
