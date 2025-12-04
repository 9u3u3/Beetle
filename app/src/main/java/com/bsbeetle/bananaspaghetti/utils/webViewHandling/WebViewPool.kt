package com.bsbeetle.bananaspaghetti.utils.webViewHandling

import android.content.Context
import android.content.MutableContextWrapper
import java.util.LinkedList
import java.util.Queue

object WebViewPool {
    private val pool: Queue<CustomWebView> = LinkedList()

    fun get(context: Context): CustomWebView {
        val webView = pool.poll()
        return if (webView != null) {
            // Update context
            (webView.context as MutableContextWrapper).baseContext = context
            webView
        } else {
            // Create new if pool empty
            // Note: We use MutableContextWrapper to allow context switching
            CustomWebView(MutableContextWrapper(context), null) 
        }
    }

    fun recycle(webView: CustomWebView) {
        // Reset state
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.onPause()
        webView.removeAllViews()
        // webView.destroy() // Don't destroy if recycling!
        
        // Only pool if we have a reasonable number
        if (pool.size < 5) {
            pool.offer(webView)
        } else {
            webView.destroy()
        }
    }
}
