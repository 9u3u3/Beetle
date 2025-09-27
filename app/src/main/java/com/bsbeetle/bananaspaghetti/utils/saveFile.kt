package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import android.webkit.WebView
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import java.io.FileNotFoundException
import java.io.OutputStreamWriter

fun saveFile(
    fileViewModel: FileViewModel,
    context: Context
) {

    val webView: WebView = EditorWebView.getWebview(context)

    // Get the code from code mirror, override that with the opened file
    webView.evaluateJavascript("window.code_") { result ->
        val newCode = result.removeSurrounding("\"")
            .replace("\\n", "\n")
            .replace("\\u003C", "<")
            .replace("\\u003E", ">")
            .replace("\\\"", "\"")

        val resolver = context.contentResolver
        fileViewModel.selectedUri?.let { uri ->
            val out = resolver.openOutputStream(uri, "wt") // truncate mode
                ?: throw FileNotFoundException("Unable to open output stream for $uri")

            OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                writer.write(newCode)
                writer.flush()
            }
        }

    }
}