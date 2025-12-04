package com.bsbeetle.bananaspaghetti.utils.webViewHandling

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session

class CustomWebView(context: Context, var session: Session?) : WebView(context) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val rotation = windowManager.defaultDisplay.rotation
        val isLandscape = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_270

        val inputConnection = super.onCreateInputConnection(outAttrs)

        if (inputConnection != null) {
            session?._inputConnection = inputConnection
            Log.d("NoKeyboardWebView", "InputConnection saved to Ic")
        }

        if (isLandscape) {
            outAttrs.inputType = EditorInfo.TYPE_NULL
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or
                    EditorInfo.IME_ACTION_NONE
            outAttrs.initialSelStart = 0
            outAttrs.initialSelEnd = 0
        }
        return inputConnection
    }
    override fun onCheckIsTextEditor() = true
}