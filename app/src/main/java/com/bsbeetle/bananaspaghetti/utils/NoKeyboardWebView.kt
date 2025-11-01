package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView

class NoKeyboardWebView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attr, defStyleAttr) {

    fun currentInputConnection(): InputConnection? {
        return onCreateInputConnection(EditorInfo())
    }

}