package com.stepserve.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Green900,
    secondary = Green800,
    onSecondary = White,
    background = White,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    error = RedError,
    onError = White,
    outline = Gray300,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DB891),
    onPrimary = Green900,
    primaryContainer = Green800,
    onPrimaryContainer = GreenContainer,
    secondary = Color(0xFF4DB891),
    background = Color(0xFF121212),
    onBackground = White,
    surface = Color(0xFF1E1E1E),
    onSurface = White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Gray300,
    error = Color(0xFFCF6679),
    outline = Color(0xFF444444),
)

@Composable
fun StepServeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
