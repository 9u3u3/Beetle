package com.bsbeetle.bananaspaghetti.ui.Extension

import kotlinx.serialization.Serializable

/** One language extension. The list and the files come from `assets/plugins` inside the app. */
@Serializable
data class ExtensionModel(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String,
    /** Name of an SVG file in `assets/file_icons`, without the extension. */
    @kotlinx.serialization.SerialName("icon") val iconName: String,
    val version: String = "1.0.0",
    val author: String = "Beetle",

    // Local state fields (not from the asset list)
    var installed: Boolean = false,
    var isInstalling: Boolean = false
) {
    val icon: String
        get() = "file:///android_asset/file_icons/$iconName.svg"
}
