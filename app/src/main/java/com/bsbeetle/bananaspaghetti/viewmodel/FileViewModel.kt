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
}