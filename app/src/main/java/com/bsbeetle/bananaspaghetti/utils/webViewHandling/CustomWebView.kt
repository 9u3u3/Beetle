package com.bsbeetle.bananaspaghetti.utils.webViewHandling

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import com.bsbeetle.bananaspaghetti.data.Ic
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session

/*TODO*/
// I have to make the each connection of IC to each session and not global one connection
class CustomWebView(context: Context, val session: Session) : WebView(context) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val inputConnection = super.onCreateInputConnection(outAttrs)

        if (inputConnection != null) {
            // Global, has to make it session wise
            session._inputConnection = inputConnection
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
        // return null  // Always return the connection
        return inputConnection
    }
    override fun onCheckIsTextEditor() = true

}