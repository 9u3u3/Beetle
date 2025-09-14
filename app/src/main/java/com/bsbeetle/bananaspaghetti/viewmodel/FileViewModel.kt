package com.bsbeetle.bananaspaghetti.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class FileViewModel : ViewModel() {
    var selectedUri by mutableStateOf<Uri?>(null)
}