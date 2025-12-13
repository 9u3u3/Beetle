package com.bsbeetle.bananaspaghetti.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList

class BufferViewModel : ViewModel() {
    val bufferLine = mutableStateMapOf<DocumentFile, String>()
    val filesOpenedList = mutableStateListOf<DocumentFile>()
    
    // Track currently active file
    var currentFile = mutableStateOf<DocumentFile?>(null)
    
    /**
     * Save the current editor content to the buffer before switching files.
     * Uses async JS evaluation, so the actual save happens in background.
     */
    fun saveCurrentContentToBuffer() {
        val file = currentFile.value ?: return
        val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession
        
        session?.view?.evaluateJavascript("window.view?.state.doc.toString() || window.code_ || ''") { result ->
            if (result != null && result != "null" && result != "\"\"") {
                val content = result.removeSurrounding("\"")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                bufferLine[file] = content
                Log.d("Buffer", "Saved content for ${file.name}: ${content.length} chars")
            }
        }
    }
}