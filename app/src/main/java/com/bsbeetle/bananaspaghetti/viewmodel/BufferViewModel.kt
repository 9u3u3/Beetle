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
    // Use URI string as key instead of DocumentFile to avoid duplicate entries
    val bufferLine = mutableStateMapOf<String, String>()
    val filesOpenedList = mutableStateListOf<DocumentFile>()
    
    // Track currently active file
    var currentFile = mutableStateOf<DocumentFile?>(null)
    
    // Track files with unsaved changes (dirty state)
    val dirtyFiles = mutableStateListOf<DocumentFile>()
    
    fun markAsDirty(file: DocumentFile) {
        if (!dirtyFiles.contains(file)) {
            dirtyFiles.add(file)
            Log.d("Buffer", "Marked dirty: ${file.name}")
        }
    }
    
    fun markAsClean(file: DocumentFile) {
        dirtyFiles.remove(file)
        Log.d("Buffer", "Marked clean: ${file.name}")
    }
    
    /**
     * Save the current editor content to the buffer before switching files.
     * Uses async JS evaluation, so the actual save happens in background.
     */
    fun saveCurrentContentToBuffer() {
        val file = currentFile.value
        if (file == null) {
            Log.d("Buffer", "saveCurrentContentToBuffer: currentFile is null, skipping")
            return
        }
        
        val fileUri = file.uri.toString()
        Log.d("Buffer", "saveCurrentContentToBuffer: Attempting to save ${file.name} (URI: $fileUri)")
        
        val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1) as? EditorSession
        if (session == null) {
            Log.e("Buffer", "saveCurrentContentToBuffer: No EditorSession found!")
            return
        }
        
        if (session.view == null) {
            Log.e("Buffer", "saveCurrentContentToBuffer: WebView is null!")
            return
        }
        
        // Try window.editor.state.doc.toString() first (more reliable)
        session.view?.evaluateJavascript("window.editor?.state?.doc?.toString() || window.code_ || ''") { result ->
            Log.d("Buffer", "saveCurrentContentToBuffer: JS returned for ${file.name}, result length=${result?.length ?: 0}")
            
            if (result != null && result != "null" && result != "\"\"") {
                var content = result.removeSurrounding("\"")
                
                // Unescape common sequences first
                content = content
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\r", "\r")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\\\", "\\")
                
                // Unescape ALL Unicode sequences (\uXXXX)
                val unicodePattern = Regex("\\\\u([0-9a-fA-F]{4})")
                content = unicodePattern.replace(content) { matchResult ->
                    val hexCode = matchResult.groupValues[1]
                    hexCode.toInt(16).toChar().toString()
                }
                
                bufferLine[fileUri] = content  // Use URI as key
                Log.d("Buffer", "✓ Saved content for ${file.name}: ${content.length} chars, buffer size now: ${bufferLine.size}")
                Log.d("Buffer", "  URI key: $fileUri")
                Log.d("Buffer", "  First 50 chars: ${content.take(50)}")
            } else {
                Log.e("Buffer", "✗ Failed to save ${file.name}: result was null or empty")
            }
        }
    }
}