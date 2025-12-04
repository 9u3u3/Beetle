package com.bsbeetle.bananaspaghetti.ui.Extension

data class ExtensionModel(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String,
    val size: String,
    val rating: Double,
    val installed: Boolean
)