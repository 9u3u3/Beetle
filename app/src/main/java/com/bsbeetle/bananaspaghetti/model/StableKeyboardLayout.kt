package com.bsbeetle.bananaspaghetti.model

// You can create a new file or add this to Key.kt

import androidx.compose.runtime.Immutable

@Immutable // This tells Compose this class is 100% stable
data class StableKeyboardLayout(
    val rows: List<List<Key>>
)