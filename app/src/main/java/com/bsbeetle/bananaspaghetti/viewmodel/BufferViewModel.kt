package com.bsbeetle.bananaspaghetti.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel

class BufferViewModel : ViewModel() {
    val bufferLine = mutableStateMapOf<DocumentFile, String>()
    val filesOpenedList = mutableStateListOf<DocumentFile>()
}