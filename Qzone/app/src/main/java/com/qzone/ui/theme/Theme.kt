package com.qzone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = QzonePrimary,
    onPrimary = QzoneOnPrimary,
    secondary = QzoneSecondary,
    onSecondary = QzoneOnSecondary,
    background = QzoneBackground,
    surface = QzoneSurface,
    surfaceVariant = Color(0xFFF0EFF4),
    onSurface = Color(0xFF1C1C24),
    outline = Color(0xFFD4D5DA)
)

private val DarkColors = darkColorScheme(
    primary = QzonePrimary,
    onPrimary = QzoneOnPrimary,
    secondary = QzoneSecondary,
    background = Color(0xFF101018),
    surface = Color(0xFF181824)
)

@Composable
fun QzoneTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QzoneTypography,
        content = content
    )
}
