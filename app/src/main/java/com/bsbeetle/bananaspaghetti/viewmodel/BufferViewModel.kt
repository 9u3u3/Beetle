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
    // Track which session this buffer belongs to
    private var currentSessionId: Int? = null
    
    // Per-session buffers: Map<SessionId, Map<URI, Content>>
    private val sessionBuffers = mutableStateMapOf<Int, MutableMap<String, String>>()
    
    // Per-session file lists: Map<SessionId, List<DocumentFile>>
    private val sessionFiles = mutableStateMapOf<Int, MutableList<DocumentFile>>()
    
    // Per-session dirty files: Map<SessionId, Set<URI>>
    private val sessionDirtyFiles = mutableStateMapOf<Int, MutableSet<String>>()
    
    // Current session's buffer (for backward compatibility)
    val bufferLine: MutableMap<String, String>
        get() = sessionBuffers.getOrPut(currentSessionId ?: -1) { mutableMapOf() }
    
    val filesOpenedList = mutableStateListOf<DocumentFile>()
    
    // Track currently active file
    var currentFile = mutableStateOf<DocumentFile?>(null)
    
    // Track files with unsaved changes (dirty state) - Store URIs as Strings
    val dirtyFiles = mutableStateListOf<String>()
    
    /**
     * Set the current session ID. This loads that session's buffer state.
     */
    fun setSessionId(sessionId: Int) {
        if (currentSessionId != sessionId) {
            Log.d("Buffer", "Switching session from $currentSessionId to $sessionId")
            
            // Save current state to old session
            currentSessionId?.let { oldId ->
                sessionFiles[oldId] = filesOpenedList.toMutableList()
                sessionDirtyFiles[oldId] = dirtyFiles.toMutableSet()
            }
            
            // Clear current state
            filesOpenedList.clear()
            dirtyFiles.clear()
            currentFile.value = null
            
            // Load new session's state
            currentSessionId = sessionId
            sessionFiles[sessionId]?.let { files ->
                filesOpenedList.addAll(files)
            }
            sessionDirtyFiles[sessionId]?.let { dirty ->
                dirtyFiles.addAll(dirty)
            }
            
            Log.d("Buffer", "Session $sessionId loaded: ${filesOpenedList.size} files, ${dirtyFiles.size} dirty")
        }
    }
    
    fun markAsDirty(fileUri: String) {
        if (!dirtyFiles.contains(fileUri)) {
            dirtyFiles.add(fileUri)
            // Also track in session-specific storage
            currentSessionId?.let { id ->
                sessionDirtyFiles.getOrPut(id) { mutableSetOf() }.add(fileUri)
            }
            Log.d("Buffer", "Marked dirty: $fileUri")
        }
    }
    
    fun markAsClean(fileUri: String) {
        dirtyFiles.remove(fileUri)
        // Also remove from session-specific storage
        currentSessionId?.let { id ->
            sessionDirtyFiles[id]?.remove(fileUri)
        }
        Log.d("Buffer", "Marked clean: $fileUri")
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
    
    fun clear() {
        bufferLine.clear()
        filesOpenedList.clear()
        currentFile.value = null
        dirtyFiles.clear()
        Log.d("Buffer", "BufferViewModel cleared for session $currentSessionId")
    }
    
    /**
     * Clear all session data (for app reset)
     */
    fun clearAll() {
        sessionBuffers.clear()
        sessionFiles.clear()
        sessionDirtyFiles.clear()
        currentSessionId = null
        filesOpenedList.clear()
        currentFile.value = null
        dirtyFiles.clear()
        Log.d("Buffer", "BufferViewModel fully cleared")
    }
}