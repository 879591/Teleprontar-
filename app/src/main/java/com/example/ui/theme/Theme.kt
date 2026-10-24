package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = StudioCyanContainer,
    onPrimaryContainer = OnStudioCyanContainer,
    secondary = StudioCyanLight,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF132D46),
    onSecondaryContainer = StudioCyanLight,
    tertiary = StudioAmber,
    onTertiary = Color.Black,
    background = StudioBlack,
    onBackground = TextPrimary,
    surface = StudioDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = StudioCardBorder,
    error = StudioRedRecord,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007A8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4EBF0),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF4A6267),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to studio dark theme for teleprompter & camera
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
