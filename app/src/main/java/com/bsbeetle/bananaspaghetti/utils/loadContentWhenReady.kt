package com.bsbeetle.bananaspaghetti.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

fun loadContentWhenReady(webView: WebView?, content: String) {

    //test
    Log.d("LoadContent", "Called loadContent")

    val jsCheck = "window.isEditorReady === true;" // Check if editor is loaded or not
    Log.wtf("LoadContent", "Called Before Before from result == true")

    if(webView == null) {
        Log.wtf("LoadContent", "web view is null")
    }

    webView?.evaluateJavascript(jsCheck) { result ->

        Log.wtf("LoadContent", "Called Before from result == true")

        if (result == "true") {

            Log.wtf("LoadContent", "Called from result == true")

            val jsCommand = """
                window.editor.dispatch({
                    changes: { from: 0, to: window.editor.state.doc.length, insert: ${JSONObject.quote(content)} }
                });
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