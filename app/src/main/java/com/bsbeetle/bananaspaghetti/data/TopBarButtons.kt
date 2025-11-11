package com.bsbeetle.bananaspaghetti.data

import androidx.compose.ui.graphics.vector.ImageVector

data class TopBarButtons(
    val title: String? = null,
    val icon: ImageVector? = null,
    val imageRes: Int? = null,
    val onClick: () -> Unit
)
