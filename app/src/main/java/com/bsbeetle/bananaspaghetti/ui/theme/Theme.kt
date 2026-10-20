package com.bsbeetle.bananaspaghetti.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Feed the app's real palette (AppColors) into Material so stock components — buttons,
// text fields, sheets, dialogs — match the rest of the VS Code-dark UI instead of
// rendering with placeholder/system colors.
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.AccentBlue,
    onPrimary = AppColors.TextPrimary,
    secondary = AppColors.AccentGreen,
    tertiary = AppColors.AccentPurple,
    background = AppColors.BgPrimary,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.BgSecondary,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.BgTertiary,
    onSurfaceVariant = AppColors.TextSecondary,
    error = AppColors.AccentRed,
    outline = AppColors.Border
)

private val TextColorDark = TextTheme(
    textPrimary = White
)

/**
 * Beetle is a dark-only app: [AppColors] is a VS Code-dark palette and the UI still carries several
 * hundred dark-tuned color literals, so a light Material scheme has nothing coherent to resolve
 * against. Following the system setting used to hand light-mode devices a stub light scheme paired
 * with white text — i.e. white on a light surface. Until a real light palette exists, pin the dark scheme regardless of the system setting.
 *
 * [darkTheme] and [dynamicColor] are kept so existing call sites compile, but neither can currently
 * select a light or wallpaper-derived scheme.
 */
@Composable
fun BeetleTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTextTheme provides TextColorDark) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}