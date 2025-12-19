package com.bsbeetle.bananaspaghetti.utils.fileHandling

import android.content.Context
import android.util.Log
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.Session
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType
import com.bsbeetle.bananaspaghetti.utils.webViewHandling.CustomWebView
import com.bsbeetle.bananaspaghetti.viewmodel.BufferViewModel
import com.bsbeetle.bananaspaghetti.viewmodel.FileViewModel
import java.io.FileNotFoundException
import java.io.OutputStreamWriter

fun saveFile(
        fileViewModel: FileViewModel,
        bufferViewModel: BufferViewModel,
        context: Context,
) {

    val session: Session = SessionList.sessions[SessionList.activeSessionIndex!!]

    if (session.type != SessionType.EDITOR) {
        Log.e("saveFile", "Not an editor session, cant save a file")
        return
    }

    val webView: CustomWebView = session.view!!

    Log.d("Save", "Save clicked for ${fileViewModel.selectedUri}")

    // Get the code from code mirror, override that with the opened file
    webView.evaluateJavascript("window.editor?.state?.doc?.toString() || window.code_ || ''") {
            result ->
        val newCode =
                result.removeSurrounding("\"")
                        .replace("\\n", "\n")
                        .replace("\\u003C", "<")
                        .replace("\\u003E", ">")
                        .replace("\\\"", "\"")

        Log.d("Save", "$newCode")

        val resolver = context.contentResolver
        fileViewModel.selectedUri?.let { uri ->
            try {
                val out =
                        resolver.openOutputStream(uri, "wt") // truncate mode
                         ?: throw FileNotFoundException("Unable to open output stream for $uri")

                OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                    writer.write(newCode)
                    writer.flush()
                }

                // Clear dirty state after successful save using the actual saved URI
                bufferViewModel.markAsClean(uri.toString())
                Log.d("Save", "Marked ${uri} as clean")

                Log.d("Save", "File saved successfully and marked clean")
                android.widget.Toast.makeText(
                                context,
                                "File saved",
                                android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
            } catch (e: Exception) {
                Log.e("Save", "Failed to save file", e)
            }
        }
    }
}
