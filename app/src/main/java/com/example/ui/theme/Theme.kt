package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val titleRu: String) {
    SYSTEM("Как в системе"),
    DARK("Тёмная (цех)"),
    LIGHT("Светлая (день)")
}

private val DarkColorScheme = darkColorScheme(
    primary = SafetyOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF431407),
    onPrimaryContainer = SafetyOrangeLight,
    secondary = WorkshopAmber,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF3B2506),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = WorkshopGreen,
    onTertiary = Color(0xFF052E16),
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    error = WorkshopRed,
    onError = Color.White,
    outline = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = SafetyOrangeDark,
    onPrimary = Color.White,
    primaryContainer = SafetyOrangeLight,
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = Color(0xFFDC2626),
    onError = Color.White,
    outline = Color(0xFF94A3B8)
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
