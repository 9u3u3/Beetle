package com.bsbeetle.bananaspaghetti.data.sidebar


data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isExpanded: Boolean = false,
    val level: Int = 0,
    val children: List<FileItem> = emptyList()
)