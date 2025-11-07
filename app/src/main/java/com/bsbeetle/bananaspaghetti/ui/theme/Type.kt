package com.bsbeetle.bananaspaghetti.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bsbeetle.bananaspaghetti.R

// Importing Iosevka Font
val IosevkaFont = FontFamily(
    Font(R.font.iosevka_regular, FontWeight.Normal),
    Font(R.font.iosevka_bold, FontWeight.Bold),
    Font(R.font.iosevka_thin, FontWeight.Thin)
)

// Importing karmatic font
val KarmaticFont = FontFamily(
    Font(R.font.karmatic_arcade_regular)
)

val born2bsporty = FontFamily(
    Font(R.font.born2bsporty_fs_regular)
)

val codaRegular = FontFamily(
    Font(R.font.coda_regular)
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = IosevkaFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = IosevkaFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = IosevkaFont,
        fontWeight = FontWeight.Thin,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    // New Karmatic text style
    displayLarge = TextStyle(
        fontFamily = KarmaticFont,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 1.sp
    )
)