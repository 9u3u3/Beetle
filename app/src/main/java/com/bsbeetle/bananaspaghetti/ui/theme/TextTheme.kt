package com.bsbeetle.bananaspaghetti.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class TextTheme(
    val textPrimary: Color
)

val LocalTextTheme = staticCompositionLocalOf {
    TextTheme(White)
}