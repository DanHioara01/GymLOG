package com.example.gymlog2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.gymlog2.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    secondary = AccentPurple,
    tertiary = AccentPurple,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = TextWarmWhite,
    onSurface = TextWarmWhite,
    onSurfaceVariant = TextGrayRed,
    outline = DarkDivider,
    error = AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = AccentRed,
    secondary = AccentPurple,
    tertiary = AccentPurple,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightDividerGray,
    error = AccentRed
)

@Composable
fun GymLOGTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
