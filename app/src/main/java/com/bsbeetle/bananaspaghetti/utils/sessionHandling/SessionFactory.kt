package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.content.Context

/**
 * SessionFactory - Creates editor sessions.
 * Note: Notebook sessions are created separately via ZoomOut screen.
 * Files are no longer auto-routed to NotebookSession based on extension.
 */
object SessionFactory {
    
    /**
     * Create an EditorSession for the given file.
     * @param context Application context
     * @param fileName Name of the file being opened
     * @param filePath Optional file path for the session
     * @return EditorSession
     */
    fun createSession(
        context: Context,
        fileName: String,
        filePath: String? = null
    ): Session {
        // Always create EditorSession - Notebook is a separate session type
        return EditorSession(context, fileName)
    }
    
    /**
     * Check if a file is a Python file based on its extension.
     */
    fun isPythonFile(fileName: String): Boolean {
        return fileName.endsWith(".py", ignoreCase = true) ||
               fileName.endsWith(".pyw", ignoreCase = true)
    }
}

