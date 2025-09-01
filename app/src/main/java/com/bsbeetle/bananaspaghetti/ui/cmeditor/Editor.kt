package com.bsbeetle.bananaspaghetti.ui.cmeditor

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Editor(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {

                // Enable debugging (use chrome://inspect)
                WebView.setWebContentsDebuggingEnabled(true)

                settings.apply {
                    javaScriptEnabled = true // Turns on JS execution inside WebView as it is NEEDED by codemirror6
                    domStorageEnabled = true // Needed by codemirror6
                    allowFileAccess = true // Lets the WebView load files from assets
                    cacheMode = WebSettings.LOAD_NO_CACHE // Don't use cache to load files, fetch fresh resources to use
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // Allows both HTTPS and HTTP resources to load
                    useWideViewPort = true // To use CSS viewport width (to look like desktop)
                    loadWithOverviewMode = true // Fit the whole content to screen width initially
                    setSupportZoom(false) // No pinch to zoom allowed
                }
                loadUrl("file:///android_asset/editor/index.html")
            }
        },
        modifier = modifier
    )
}