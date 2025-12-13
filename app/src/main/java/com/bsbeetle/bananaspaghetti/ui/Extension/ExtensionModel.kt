package com.bsbeetle.bananaspaghetti.ui.Extension

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtensionModel(
    val id: Int,
    @SerialName("created_at") val createdAt: String,
    val name: String,
    val slug: String,
    val version: String,
    val description: String,
    val icon: String, // URL
    @SerialName("js_module") val jsModule: String, // URL
    @SerialName("lsp_script") val lspScript: String, // URL
    val author: String,
    
    // Local state fields (not from API)
    var installed: Boolean = false,
    var isInstalling: Boolean = false,
    var downloads: String = "0",
    var stars: Int = 0
)