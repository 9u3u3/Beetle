package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.content.Context
import android.view.inputmethod.InputConnection
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView

enum class SessionType {
    EDITOR,
    BROWSER,
    TERMINAL,
    DEMO
}

abstract class Session(
    val context: Context,
    val type: SessionType
) {
    var id: Int? = null
    var view: CustomWebView? = null
    var _inputConnection: InputConnection? = null

    init {
        id = SessionList.sessions.size
        SessionList.sessions.add(this)
        SessionList.activeSessionIndex = SessionList.sessions.indexOf(this)
    }

    abstract fun createView()
    
    open fun destroy() {
        view?.destroy()
        view = null
        SessionList.sessions.remove(this)
        // Adjust active index if needed (simplified logic)
        if (SessionList.sessions.isNotEmpty()) {
             SessionList.activeSessionIndex = SessionList.sessions.size - 1
        } else {
            SessionList.activeSessionIndex = null
        }
    }
}