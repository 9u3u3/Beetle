import androidx.compose.ui.graphics.Color

fun getFileIconColor(fileName: String): Color {
    return when {
        fileName.endsWith(".js") -> Color(0xFFF7DF1E)
        fileName.endsWith(".kt") -> Color(0xFF7F52FF)
        fileName.endsWith(".json") -> Color(0xFFFCA311)
        else -> Color(0xFF858585)
    }
}
