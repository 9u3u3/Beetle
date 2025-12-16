package com.bsbeetle.bananaspaghetti.data.sidebar


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.bsbeetle.bananaspaghetti.R

enum class SidebarTab(
    val icon: ImageVector? = null,
    val imageRes: Int? = null,
    val label: String
) {
    FILES(imageRes = R.drawable.computer_folder_open_icon, label = "Files"),
    GIT(icon = Icons.Default.AccountTree, label = "Source Control")
}