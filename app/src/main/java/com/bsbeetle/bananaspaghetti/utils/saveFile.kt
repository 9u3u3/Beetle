package com.bsbeetle.bananaspaghetti.utils

import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bsbeetle.bananaspaghetti.viewmodel.EditorViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import java.io.FileNotFoundException
import java.io.OutputStreamWriter

fun saveFile(
    fileViewModel: FileViewModel,
    context: Context,
    editorViewModel: EditorViewModel
) {

    val webView: WebView = editorViewModel.webView!!

    Log.d("Save", "Save clicked for ${fileViewModel.selectedUri}")

    // Get the code from code mirror, override that with the opened file
    webView.evaluateJavascript("window.code_") { result ->
        val newCode = result.removeSurrounding("\"")
            .replace("\\n", "\n")
            .replace("\\u003C", "<")
            .replace("\\u003E", ">")
            .replace("\\\"", "\"")

        Log.d("Save", "$newCode")

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