package com.bsbeetle.bananaspaghetti.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Black,
    secondary = Black,
    tertiary = Black
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    secondary = Black,
    tertiary = Black
)

private val TextColorDark = TextTheme(
    textPrimary = White
)

private val TextColorLight = TextTheme(
    textPrimary = White
)

@Composable
fun BananaSpaghettiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Text theme
    val textColors = if(darkTheme) {
        TextColorDark
    } else {
        TextColorLight
    }

    CompositionLocalProvider(LocalTextTheme provides textColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }

}