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

abstract class Session(val context: Context, val type: SessionType, var name: String = "") {
    var id: Int? = null
    var view: CustomWebView? = null
    var _inputConnection: InputConnection? = null
    
    // Project root folder for the session (used by EditorSession and NotebookSession)
    var projectRoot: androidx.documentfile.provider.DocumentFile? = null

    init {
        // Register session
        id = SessionList.sessions.size
        SessionList.sessions.add(this)
        SessionList.activeSessionIndex = SessionList.sessions.indexOf(this)

        // Automatically create the view upon initialization
        // Note: Be careful calling abstract methods in init, but for this structure it works
        // provided implementation doesn't rely on uninitialized child properties.
    }

    // Abstract method: Children must implement how their view is built
    abstract fun createView(): CustomWebView

    // Shared helper to get the view (if you prefer a getter method)
    fun getOrInitView(): CustomWebView {
        if (view == null) {
            view = createView()
        }
        return view!!
    }

    open fun destroy() {
        view?.destroy()
        view = null
        SessionList.sessions.remove(this)

        // Update active index
        if (SessionList.sessions.isNotEmpty()) {
            SessionList.activeSessionIndex = SessionList.sessions.size - 1
        } else {
            SessionList.activeSessionIndex = null
        }
    }
}
