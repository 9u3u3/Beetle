//package com.bsbeetle.bananaspaghetti.viewmodel
//
//import android.net.Uri
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.setValue
//import androidx.lifecycle.ViewModel
//
//class FileViewModel : ViewModel() {
//    var selectedUri by mutableStateOf<Uri?>(value = null)
//    var selectedFolderUri by mutableStateOf<Uri?>(value = null)
//}


package com.bsbeetle.bananaspaghetti.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class FileViewModel : ViewModel() {
    var selectedUri by mutableStateOf<Uri?>(value = null) // Current file being edited
    var selectedFolderUri by mutableStateOf<Uri?>(value = null) // Root folder for sidebar
    var currentFilePath by mutableStateOf<String?>(value = null) // Path of current file for sidebar highlighting
}