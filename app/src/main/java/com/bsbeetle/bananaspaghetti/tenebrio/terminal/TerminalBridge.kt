package com.bsbeetle.bananaspaghetti.tenebrio.terminal

import android.webkit.JavascriptInterface

class TerminalBridge(private val onInput: (String) -> Unit) {
    @JavascriptInterface
    fun sendInput(input: String) {
        // Called from JS whenever user types
        onInput(input)
    }
}