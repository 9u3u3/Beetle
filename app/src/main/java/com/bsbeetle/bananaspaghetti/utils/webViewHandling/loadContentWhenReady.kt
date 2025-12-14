package com.bsbeetle.bananaspaghetti.utils.webViewHandling

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import org.json.JSONObject

fun loadContentWhenReady(webView: WebView?, content: String) {
    val jsCheck = "window.isEditorReady === true;" // Check if editor is loaded or not
    webView?.evaluateJavascript(jsCheck) { result ->
        if (result == "true") {
            val jsCommand = """
                window.isProgrammaticChange = true;
                window.editor.dispatch({
                    changes: { from: 0, to: window.editor.state.doc.length, insert: ${JSONObject.quote(content)} }
                });
                setTimeout(() => { window.isProgrammaticChange = false; }, 300);
            """.trimIndent()
            webView.evaluateJavascript(jsCommand, null) // add the content
            //... to the editor
        } else {
            // Wait a bit and try again
            Handler(Looper.getMainLooper()).postDelayed({
                loadContentWhenReady(webView, content)
            }, 50) // Loop to try again is the editor is not ready yet
        }
    }
}