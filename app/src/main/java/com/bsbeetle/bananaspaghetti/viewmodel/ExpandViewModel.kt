package com.bsbeetle.bananaspaghetti.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel

class ExpandViewModel : ViewModel() {
    val expandedFolders = mutableStateMapOf<String, Boolean>()
}