package com.bsbeetle.bananaspaghetti.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

import android.content.Context
import com.bsbeetle.bananaspaghetti.utils.fileHandling.RecentFile
import com.bsbeetle.bananaspaghetti.utils.fileHandling.RecentFilesManager

class FileViewModel : ViewModel() {
    var selectedUri by mutableStateOf<Uri?>(value = null) // Current file being edited
    var selectedFolderUri by mutableStateOf<Uri?>(value = null) // Root folder for sidebar
    var currentFilePath by mutableStateOf<String?>(value = null) // Path of current file for sidebar highlighting

    // ========== Remote (SSH/SFTP) Mode ==========
    var isRemoteMode by mutableStateOf(false)
    var currentRemotePath by mutableStateOf<String?>(null) // Currently open remote file path
    var remoteBasePath by mutableStateOf<String?>(null)    // Root folder path on remote host
    
    // Recent Files State
    var recentFiles by mutableStateOf<List<RecentFile>>(emptyList())
        private set

    fun loadRecentFiles(context: Context) {
        recentFiles = RecentFilesManager.getRecentFiles(context)
    }

    fun clearRecentFiles(context: Context) {
        android.util.Log.d("FileViewModel", "Clearing recent files...")
        RecentFilesManager.clear(context)
        recentFiles = emptyList()
        android.util.Log.d("FileViewModel", "Recent files state updated to empty list")
    }
    
    // Counter to trigger file tree refresh when files/folders are created
    var fileTreeRefreshCounter by mutableIntStateOf(0)
        private set
    
    fun refreshFileTree() {
        fileTreeRefreshCounter++
    }
    
    // ========== Multi-Select State ==========
    
    // Set of currently selected file/folder paths (URIs as strings)
    var selectedFilePaths by mutableStateOf<Set<String>>(emptySet())
        private set
    
    // Whether multi-select mode is currently active
    var isMultiSelectMode by mutableStateOf(false)
        private set
    
    /**
     * Enter multi-select mode and select the initial file/folder
     */
    fun enterMultiSelectMode(initialPath: String) {
        isMultiSelectMode = true
        selectedFilePaths = setOf(initialPath)
        android.util.Log.d("FileViewModel", "Entered multi-select mode with: $initialPath")
    }
    
    /**
     * Toggle a file/folder in the selection
     */
    fun toggleFileSelection(path: String) {
        selectedFilePaths = if (selectedFilePaths.contains(path)) {
            selectedFilePaths - path
        } else {
            selectedFilePaths + path
        }
        android.util.Log.d("FileViewModel", "Toggled selection: $path, now selected: ${selectedFilePaths.size}")
        
        // Exit multi-select if no files selected
        if (selectedFilePaths.isEmpty()) {
            exitMultiSelectMode()
        }
    }
    
    /**
     * Clear selection and exit multi-select mode
     */
    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedFilePaths = emptySet()
        android.util.Log.d("FileViewModel", "Exited multi-select mode")
    }
    
    /**
     * Select all files in the provided list
     */
    fun selectAll(paths: List<String>) {
        selectedFilePaths = paths.toSet()
        android.util.Log.d("FileViewModel", "Selected all: ${selectedFilePaths.size} files")
    }
}