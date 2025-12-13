package com.bsbeetle.bananaspaghetti.utils.webViewHandling

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList

@SuppressLint("ClickableViewAccessibility")
class CustomWebView(context: Context, var session: Session?) : WebView(context) {
    
    private val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTapFocus()
            return false 
        }
        override fun onDown(e: MotionEvent): Boolean = false
    })

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = true
        // Disable overscroll to prevent bounce effect which might interfere
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    // CRITICAL: Override dispatchTouchEvent to request parent not to intercept EARLY
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        
        // Pass to gesture detector for focus handling
        gestureDetector.onTouchEvent(event)
        
        return super.dispatchTouchEvent(event)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Always request disallow intercept to be safe
        parent?.requestDisallowInterceptTouchEvent(true)
        
        return super.onTouchEvent(event)
    }
    
    private fun handleTapFocus() {
        session?.let { sess ->
            val index = SessionList.sessions.indexOf(sess)
            if (index >= 0 && SessionList.activeSessionIndex != index) {
                SessionList.activeSessionIndex = index
            }
        }
        requestFocus()
        evaluateJavascript("""
            (function() {
                if (window.editor) { window.editor.focus(); } 
                else if (window.term) { window.term.focus(); }
            })();
        """.trimIndent(), null)
    }
    
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val rotation = windowManager.defaultDisplay.rotation
        val isLandscape = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_270

        val inputConnection = super.onCreateInputConnection(outAttrs)

        if (inputConnection != null) {
            session?._inputConnection = inputConnection
        }

        if (isLandscape) {
            outAttrs.inputType = EditorInfo.TYPE_NULL
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_ACTION_NONE
            outAttrs.initialSelStart = 0
            outAttrs.initialSelEnd = 0
        }
        return inputConnection
    }
    
    override fun onCheckIsTextEditor() = true
}