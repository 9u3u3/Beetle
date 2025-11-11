package com.bsbeetle.bananaspaghetti.data.sidebar


import androidx.compose.ui.graphics.Color

object FileUtils {
    fun getFileIconColor(fileName: String): Color {
        return when {
            fileName.endsWith(".js") -> Color(0xFFF7DF1E)
            fileName.endsWith(".kt") -> Color(0xFF7F52FF)
            fileName.endsWith(".json") -> Color(0xFFFCA311)
            fileName.endsWith(".md") -> Color(0xFF42A5F5)
            fileName.endsWith(".env") -> Color(0xFFEDD652)
            fileName.endsWith(".css") -> Color(0xFF2965F1)
            fileName.endsWith(".html") -> Color(0xFFE34C26)
            fileName.endsWith(".py") -> Color(0xFF3776AB)
            else -> Color(0xFF858585)
        }
    }
}