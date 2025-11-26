package com.bsbeetle.bananaspaghetti.viewmodel


import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionList
import com.bsbeetle.bananaspaghetti.utils.sessionHandling.SessionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionViewModel : ViewModel() {

    // Map of session ID to bitmap snapshot
    val sessionSnapshots = mutableStateMapOf<Int?, Bitmap?>()

    /**
     * Captures snapshots of all active sessions
     */
    fun captureAllSnapshots() {
        viewModelScope.launch {
            SessionList.sessions.forEach { session ->
                captureSessionSnapshot(session.id)
            }
        }
    }

    /**
     * Captures a snapshot of a specific session
     */
    fun captureSessionSnapshot(sessionId: Int?) {
        if (sessionId == null) return

        viewModelScope.launch {
            try {
                val session = SessionList.sessions.find { it.id == sessionId } ?: return@launch

                val webView = when (session.type) {
                    SessionType.EDITOR -> session.editorView
                    SessionType.BROWSER -> session.browserView
                    SessionType.TERMINAL -> session.terminalView
                    SessionType.DEMO -> session.demoView
                }

                if (webView != null) {
                    withContext(Dispatchers.Main) {
                        try {
                            // Create bitmap with WebView dimensions
                            val bitmap = Bitmap.createBitmap(
                                if (webView.width > 0) webView.width else 800,
                                if (webView.height > 0) webView.height else 600,
                                Bitmap.Config.ARGB_8888
                            )

                            val canvas = Canvas(bitmap)
                            webView.draw(canvas)

                            // Store the snapshot
                            sessionSnapshots[sessionId] = bitmap

                            Log.d("SessionViewModel", "Captured snapshot for session $sessionId")
                        } catch (e: Exception) {
                            Log.e("SessionViewModel", "Failed to capture snapshot for session $sessionId", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error capturing snapshot", e)
            }
        }
    }

    /**
     * Removes snapshot when session is destroyed
     */
    fun removeSnapshot(sessionId: Int?) {
        sessionId?.let {
            sessionSnapshots[it]?.recycle()
            sessionSnapshots.remove(it)
        }
    }

    /**
     * Clear all snapshots
     */
    fun clearAllSnapshots() {
        sessionSnapshots.values.forEach { it?.recycle() }
        sessionSnapshots.clear()
    }

    override fun onCleared() {
        super.onCleared()
        clearAllSnapshots()
    }
}