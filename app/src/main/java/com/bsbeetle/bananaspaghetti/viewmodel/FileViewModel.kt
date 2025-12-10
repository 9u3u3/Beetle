package com.bsbeetle.bananaspaghetti.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class FileViewModel : ViewModel() {
    var selectedUri by mutableStateOf<Uri?>(value = null) // Current file being edited
    var selectedFolderUri by mutableStateOf<Uri?>(value = null) // Root folder for sidebar
    var currentFilePath by mutableStateOf<String?>(value = null) // Path of current file for sidebar highlighting
    
    // Counter to trigger file tree refresh when files/folders are created
    var fileTreeRefreshCounter by mutableIntStateOf(0)
        private set
    
    fun refreshFileTree() {
        fileTreeRefreshCounter++
    }
}