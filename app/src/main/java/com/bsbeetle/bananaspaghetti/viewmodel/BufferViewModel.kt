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
            Log.d(TAG, "╔══════════════════════════════════════════════════════════════")
            Log.d(TAG, "║ SESSION SWITCH: $currentSessionKey → $newKey")
            Log.d(TAG, "║ Old: type=$currentSessionType, id=$currentSessionId")
            Log.d(TAG, "║ New: type=$sessionType, id=$sessionId")
            Log.d(TAG, "╚══════════════════════════════════════════════════════════════")

            // Save current state to old session
            currentSessionKey?.let { oldKey ->
                sessionFiles[oldKey] = filesOpenedList.toMutableList()
                sessionDirtyFiles[oldKey] = dirtyFiles.toMutableSet()
                Log.d(
                        TAG,
                        "  Saved state for $oldKey: ${filesOpenedList.size} files, ${dirtyFiles.size} dirty"
                )
            }

            // Clear current state
            filesOpenedList.clear()
            dirtyFiles.clear()
            currentFile.value = null
            Log.d(TAG, "  Cleared current state")

            // Update current session info
            currentSessionKey = newKey
            currentSessionId = sessionId
            currentSessionType = sessionType

            // Load new session's state
            sessionFiles[newKey]?.let { files ->
                filesOpenedList.addAll(files)
                Log.d(TAG, "  Loaded ${files.size} files for $newKey")
            }
                    ?: Log.d(TAG, "  No saved files for $newKey (new session)")

            sessionDirtyFiles[newKey]?.let { dirty ->
                dirtyFiles.addAll(dirty)
                Log.d(TAG, "  Loaded ${dirty.size} dirty files for $newKey")
            }
                    ?: Log.d(TAG, "  No dirty files for $newKey")

            Log.d(
                    TAG,
                    "Session $newKey initialized: ${filesOpenedList.size} files, ${dirtyFiles.size} dirty"
            )
        } else {
            Log.d(TAG, "setSessionId called with same key: $newKey (no-op)")
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

        Log.d(TAG, "setSessionId(legacy): id=$sessionId, detected type=$sessionType")
        setSessionId(sessionId, sessionType)
    }

    fun markAsDirty(fileUri: String) {
        if (!dirtyFiles.contains(fileUri)) {
            dirtyFiles.add(fileUri)
            // Also track in session-specific storage
            currentSessionKey?.let { key ->
                sessionDirtyFiles.getOrPut(key) { mutableSetOf() }.add(fileUri)
            }
            Log.d(TAG, "Marked dirty: $fileUri (session: $currentSessionKey)")
        }
    }

    fun markAsClean(fileUri: String) {
        dirtyFiles.remove(fileUri)
        // Also remove from session-specific storage
        currentSessionKey?.let { key -> sessionDirtyFiles[key]?.remove(fileUri) }
        Log.d(TAG, "Marked clean: $fileUri (session: $currentSessionKey)")
    }

    /**
     * Save the current editor content to the buffer before switching files. Uses async JS
     * evaluation, so the actual save happens in background.
     */
    fun saveCurrentContentToBuffer() {
        val file = currentFile.value
        if (file == null) {
            Log.d(TAG, "saveCurrentContentToBuffer: currentFile is null, skipping")
            return
        }

        val fileUri = file.uri.toString()
        Log.d(TAG, "saveCurrentContentToBuffer: Attempting to save ${file.name}")
        Log.d(TAG, "  Session: $currentSessionKey")
        Log.d(TAG, "  URI: $fileUri")

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
                Log.e(
                        TAG,
                        "saveCurrentContentToBuffer: No Editor/Notebook session found! Session type: ${session?.type}"
                )
            }
        }
    }

    private fun saveContentFromJsResult(file: DocumentFile, fileUri: String, result: String?) {
        Log.d(
                TAG,
                "saveCurrentContentToBuffer: JS returned for ${file.name}, result length=${result?.length ?: 0}"
        )

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
            Log.d(TAG, "✓ Saved content for ${file.name}: ${content.length} chars")
            Log.d(TAG, "  Session: $currentSessionKey")
            Log.d(TAG, "  Buffer size: ${bufferLine.size}")
            Log.d(TAG, "  First 50 chars: ${content.take(50)}")
        } else {
            Log.e(TAG, "✗ Failed to save ${file.name}: result was null or empty")
        }
    }

    /**
     * Clear all buffer data for a specific session. Call this when a session is destroyed to
     * prevent stale data from persisting.
     */
    fun clearSessionBuffer(sessionId: Int, sessionType: SessionType) {
        val sessionKey = createSessionKey(sessionType, sessionId)
        Log.d(TAG, "╔══════════════════════════════════════════════════════════════")
        Log.d(TAG, "║ CLEARING SESSION BUFFER: $sessionKey")
        Log.d(TAG, "║ Files: ${sessionFiles[sessionKey]?.size ?: 0}")
        Log.d(TAG, "║ Buffers: ${sessionBuffers[sessionKey]?.size ?: 0}")
        Log.d(TAG, "║ Dirty files: ${sessionDirtyFiles[sessionKey]?.size ?: 0}")
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════")

        sessionBuffers.remove(sessionKey)
        sessionFiles.remove(sessionKey)
        sessionDirtyFiles.remove(sessionKey)

        // If this was the current session, clear current state too
        if (currentSessionKey == sessionKey) {
            Log.d(TAG, "Cleared current session, resetting active state")
            currentSessionKey = null
            currentSessionId = null
            currentSessionType = null
            filesOpenedList.clear()
            currentFile.value = null
            dirtyFiles.clear()
        }

        Log.d(TAG, "Session $sessionKey buffer cleared")
    }

    fun clear() {
        bufferLine.clear()
        filesOpenedList.clear()
        currentFile.value = null
        dirtyFiles.clear()
        Log.d(TAG, "BufferViewModel cleared for session $currentSessionKey")
    }

    /** Clear all session data (for app reset) */
    fun clearAll() {
        Log.d(TAG, "╔══════════════════════════════════════════════════════════════")
        Log.d(TAG, "║ CLEARING ALL BUFFER DATA")
        Log.d(TAG, "║ Clearing ${sessionBuffers.size} session buffers")
        Log.d(TAG, "║ Clearing ${sessionFiles.size} session file lists")
        Log.d(TAG, "║ Clearing ${sessionDirtyFiles.size} session dirty lists")
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════")

        sessionBuffers.clear()
        sessionFiles.clear()
        sessionDirtyFiles.clear()
        currentSessionKey = null
        currentSessionId = null
        currentSessionType = null
        filesOpenedList.clear()
        currentFile.value = null
        dirtyFiles.clear()
        Log.d(TAG, "BufferViewModel fully cleared")
    }

    /** Debug helper: Log current buffer state */
    fun debugLogState() {
        Log.d(TAG, "╔══════════════════════════════════════════════════════════════")
        Log.d(TAG, "║ BUFFER STATE DEBUG")
        Log.d(TAG, "║ Current session: $currentSessionKey")
        Log.d(TAG, "║ Current file: ${currentFile.value?.name ?: "none"}")
        Log.d(TAG, "║ Files open: ${filesOpenedList.size}")
        Log.d(TAG, "║ Dirty files: ${dirtyFiles.size}")
        Log.d(TAG, "║ Session buffers: ${sessionBuffers.keys}")
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════")
    }
}
