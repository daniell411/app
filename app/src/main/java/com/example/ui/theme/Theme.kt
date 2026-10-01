package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ApuntaIndigoLight,
    onPrimary = Color.White,
    primaryContainer = ApuntaIndigoDark,
    onPrimaryContainer = Color.White,
    secondary = ApuntaOrange,
    onSecondary = Color.Black,
    tertiary = ApuntaGreen,
    background = ApuntaDarkBg,
    onBackground = ApuntaDarkText,
    surface = ApuntaDarkSurface,
    onSurface = ApuntaDarkText,
    surfaceVariant = ApuntaDarkSurfaceVariant,
    onSurfaceVariant = ApuntaDarkTextSecondary,
    outline = ApuntaDarkBorder,
    error = ApuntaCoral,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ApuntaIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = ApuntaIndigoDark,
    secondary = ApuntaOrange,
    onSecondary = Color.White,
    tertiary = ApuntaGreen,
    background = ApuntaLightBg,
    onBackground = ApuntaLightText,
    surface = ApuntaLightSurface,
    onSurface = ApuntaLightText,
    surfaceVariant = ApuntaLightSurfaceVariant,
    onSurfaceVariant = ApuntaLightTextSecondary,
    outline = ApuntaLightBorder,
    error = ApuntaCoral,
    onError = Color.White
)

@Composable
fun ApuntaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
