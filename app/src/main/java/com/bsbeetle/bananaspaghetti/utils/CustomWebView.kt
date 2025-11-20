package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import com.bsbeetle.bananaspaghetti.data.Ic

class CustomWebView(context: Context) : WebView(context) {

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val inputConnection = super.onCreateInputConnection(outAttrs)

        if (inputConnection != null) {
            Ic.ic = inputConnection
            Log.d("NoKeyboardWebView", "InputConnection saved to Ic")
        }

        if (isLandscape) {
            // KEY CHANGES: Suppress keyboard but keep connection active
            outAttrs.inputType = EditorInfo.TYPE_NULL  // Change from 0 to TYPE_NULL
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
                    EditorInfo.IME_ACTION_NONE
            outAttrs.initialSelStart = 0
            outAttrs.initialSelEnd = 0
        }

        return null  // Always return the connection
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }
}