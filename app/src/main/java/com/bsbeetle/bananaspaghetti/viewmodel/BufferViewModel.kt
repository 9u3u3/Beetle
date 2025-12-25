package com.bsbeetle.bananaspaghetti.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.EditorSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.NotebookSession
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType

class BufferViewModel : ViewModel() {

    companion object {
        private const val TAG = "Buffer"
    }

    // Track which session this buffer belongs to - now uses composite key "TYPE_ID"
    private var currentSessionKey: String? = null
    private var currentSessionId: Int? = null
    private var currentSessionType: SessionType? = null

    // Per-session buffers: Map<SessionKey, Map<URI, Content>>
    // SessionKey format: "EDITOR_0", "NOTEBOOK_1", etc.
    private val sessionBuffers = mutableStateMapOf<String, MutableMap<String, String>>()

    // Per-session file lists: Map<SessionKey, List<DocumentFile>>
    private val sessionFiles = mutableStateMapOf<String, MutableList<DocumentFile>>()

    // Per-session dirty files: Map<SessionKey, Set<URI>>
    private val sessionDirtyFiles = mutableStateMapOf<String, MutableSet<String>>()

    // Current session's buffer (for backward compatibility)
    val bufferLine: MutableMap<String, String>
        get() = sessionBuffers.getOrPut(currentSessionKey ?: "UNKNOWN_-1") { mutableMapOf() }

    val filesOpenedList = mutableStateListOf<DocumentFile>()

    // Track currently active file
    var currentFile = mutableStateOf<DocumentFile?>(null)

    // Track files with unsaved changes (dirty state) - Store URIs as Strings
    val dirtyFiles = mutableStateListOf<String>()

    /**
     * Create a composite session key from type and ID. This ensures Editor session 0 and Notebook
     * session 0 have different buffer spaces.
     */
    private fun createSessionKey(sessionType: SessionType, sessionId: Int): String {
        return "${sessionType.name}_$sessionId"
    }

    /**
     * Set the current session ID with session type for proper isolation. This loads that session's
     * buffer state.
     */
    fun setSessionId(sessionId: Int, sessionType: SessionType) {
        val newKey = createSessionKey(sessionType, sessionId)

        if (currentSessionKey != newKey) {
            // Save current state to old session
            currentSessionKey?.let { oldKey ->
                val savedFiles = filesOpenedList.toMutableList()
                val savedDirty = dirtyFiles.toMutableSet()
                sessionFiles[oldKey] = savedFiles
                sessionDirtyFiles[oldKey] = savedDirty
            }

            // Clear current state
            filesOpenedList.clear()
            dirtyFiles.clear()
            currentFile.value = null

            // Update current session info
            currentSessionKey = newKey
            currentSessionId = sessionId
            currentSessionType = sessionType

            // Load new session's state
            sessionFiles[newKey]?.let { loadedFiles ->
                filesOpenedList.addAll(loadedFiles)
            }

            sessionDirtyFiles[newKey]?.let { loadedDirty ->
                dirtyFiles.addAll(loadedDirty)
            }
        }
    }

    /**
     * Legacy setSessionId for backward compatibility. Tries to determine session type from
     * SessionList.
     */
    fun setSessionId(sessionId: Int) {
        // Try to determine session type from the actual session
        val session = SessionList.sessions.getOrNull(sessionId)
        val sessionType = session?.type ?: SessionType.EDITOR
        setSessionId(sessionId, sessionType)
    }

    fun markAsDirty(fileUri: String) {
        if (!dirtyFiles.contains(fileUri)) {
            dirtyFiles.add(fileUri)
            // Also track in session-specific storage
            currentSessionKey?.let { key ->
                sessionDirtyFiles.getOrPut(key) { mutableSetOf() }.add(fileUri)
            }
        }
    }

    fun markAsClean(fileUri: String) {
        dirtyFiles.remove(fileUri)
        // Also remove from session-specific storage
        currentSessionKey?.let { key -> sessionDirtyFiles[key]?.remove(fileUri) }
    }

    /**
     * Save the current editor content to the buffer before switching files. Uses async JS
     * evaluation, so the actual save happens in background.
     */
    fun saveCurrentContentToBuffer() {
        val file = currentFile.value ?: return
        val fileUri = file.uri.toString()

        // Get the actual session - could be Editor or Notebook
        val session = SessionList.sessions.getOrNull(SessionList.activeSessionIndex ?: -1)

        when (session) {
            is EditorSession -> {
                if (session.view == null) {
                    Log.e(TAG, "saveCurrentContentToBuffer: EditorSession WebView is null!")
                    return
                }

                session.view?.evaluateJavascript(
                        "window.editor?.state?.doc?.toString() || window.code_ || ''"
                ) { result -> saveContentFromJsResult(file, fileUri, result) }
            }
            is NotebookSession -> {
                if (session.view == null) {
                    Log.e(TAG, "saveCurrentContentToBuffer: NotebookSession WebView is null!")
                    return
                }

                session.view?.evaluateJavascript("window.getContent ? window.getContent() : ''") {
                        result ->
                    saveContentFromJsResult(file, fileUri, result)
                }
            }
            else -> {
                Log.e(TAG, "saveCurrentContentToBuffer: No Editor/Notebook session found!")
            }
        }
    }

    private fun saveContentFromJsResult(file: DocumentFile, fileUri: String, result: String?) {
        if (result != null && result != "null" && result != "\"\"") {
            var content = result.removeSurrounding("\"")

            // Unescape common sequences first
            content =
                    content.replace("\\n", "\n")
                            .replace("\\t", "\t")
                            .replace("\\r", "\r")
                            .replace("\\\"", "\"")
                            .replace("\\'", "'")
                            .replace("\\\\", "\\")

            // Unescape ALL Unicode sequences (\uXXXX)
            val unicodePattern = Regex("\\\\u([0-9a-fA-F]{4})")
            content =
                    unicodePattern.replace(content) { matchResult ->
                        val hexCode = matchResult.groupValues[1]
                        hexCode.toInt(16).toChar().toString()
                    }

            bufferLine[fileUri] = content // Use URI as key
        }
    }

    /**
     * Clear all buffer data for a specific session. Call this when a session is destroyed to
     * prevent stale data from persisting.
     */
    fun clearSessionBuffer(sessionId: Int, sessionType: SessionType) {
        val sessionKey = createSessionKey(sessionType, sessionId)

        sessionBuffers.remove(sessionKey)
        sessionFiles.remove(sessionKey)
        sessionDirtyFiles.remove(sessionKey)

        // If this was the current session, clear current state too
        if (currentSessionKey == sessionKey) {
            currentSessionKey = null
            currentSessionId = null
            currentSessionType = null
            filesOpenedList.clear()
            currentFile.value = null
            dirtyFiles.clear()
        }
    }

    fun clear() {
        bufferLine.clear()
        filesOpenedList.clear()
        currentFile.value = null
        dirtyFiles.clear()
    }

    /** Clear all session data (for app reset) */
    fun clearAll() {
        sessionBuffers.clear()
        sessionFiles.clear()
        sessionDirtyFiles.clear()
        currentSessionKey = null
        currentSessionId = null
        currentSessionType = null
        filesOpenedList.clear()
        currentFile.value = null
        dirtyFiles.clear()
    }

    /** Debug helper: Log current buffer state */
    fun debugLogState() {
        Log.d(TAG, "Buffer state: session=$currentSessionKey, files=${filesOpenedList.size}, dirty=${dirtyFiles.size}")
    }
}
