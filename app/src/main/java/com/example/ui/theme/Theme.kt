package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeobrutalYellow,
    onPrimary = NeobrutalBlack,
    secondary = NeobrutalBlue,
    onSecondary = NeobrutalWhite,
    tertiary = NeobrutalPink,
    background = NeobrutalBgDark,
    surface = Color(0xFF1E1E1E),
    onBackground = NeobrutalWhite,
    onSurface = NeobrutalWhite,
    outline = NeobrutalWhite
)

private val LightColorScheme = lightColorScheme(
    primary = NeobrutalYellow,
    onPrimary = NeobrutalBlack,
    secondary = NeobrutalBlue,
    onSecondary = NeobrutalWhite,
    tertiary = NeobrutalGreen,
    background = NeobrutalBgLight,
    surface = NeobrutalCardBg,
    onBackground = NeobrutalBlack,
    onSurface = NeobrutalBlack,
    outline = NeobrutalBlack
)

@Composable
fun BrutalDialTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
