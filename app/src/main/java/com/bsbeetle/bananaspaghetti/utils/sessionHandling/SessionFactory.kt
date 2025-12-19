package com.bsbeetle.bananaspaghetti.utils.sessionHandling

import android.content.Context
import com.bsbeetle.bananaspaghetti.utils.preferences.EditorPreferences

/**
 * SessionFactory - Creates the appropriate session type based on file and settings.
 * Routes .py files to NotebookSession when Python REPL mode is enabled.
 */
object SessionFactory {
    
    /**
     * Create an appropriate session for the given file.
     * @param context Application context
     * @param fileName Name of the file being opened
     * @param filePath Optional file path for the session
     * @return EditorSession or NotebookSession depending on the file type and settings
     */
    fun createSession(
        context: Context,
        fileName: String,
        filePath: String? = null
    ): Session {
        val prefs = EditorPreferences(context)
        
        // Check if Python REPL mode is enabled and this is a Python file
        if (prefs.pythonRepl && isPythonFile(fileName)) {
            val session = NotebookSession(context, fileName)
            filePath?.let { session.setFilePath(it) }
            return session
        }
        
        // Default: use regular EditorSession
        return EditorSession(context, fileName)
    }
    
    /**
     * Check if a file is a Python file based on its extension.
     */
    fun isPythonFile(fileName: String): Boolean {
        return fileName.endsWith(".py", ignoreCase = true) ||
               fileName.endsWith(".pyw", ignoreCase = true)
    }
    
    /**
     * Check if Python REPL mode is enabled in settings.
     */
    fun isPythonReplEnabled(context: Context): Boolean {
        return EditorPreferences(context).pythonRepl
    }
    
    /**
     * Determine if a file should open in notebook mode.
     */
    fun shouldUseNotebook(context: Context, fileName: String): Boolean {
        return isPythonReplEnabled(context) && isPythonFile(fileName)
    }
}
