package com.qzone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color


private val LightColors = lightColorScheme(
    primary = QzonePrimary,
    onPrimary = QzoneOnPrimary,
    primaryContainer = QzonePrimaryContainer,
    onPrimaryContainer = QzoneOnPrimaryContainer,
    secondary = QzoneSecondary,
    onSecondary = QzoneOnSecondary,
    secondaryContainer = QzoneSecondaryContainer,
    onSecondaryContainer = QzoneOnSecondaryContainer,
    tertiary = QzoneTertiary,
    onTertiary = QzoneOnTertiary,
    tertiaryContainer = QzoneTertiaryContainer,
    onTertiaryContainer = QzoneOnTertiaryContainer,
    background = QzoneBackground,
    surface = QzoneSurface,
    surfaceVariant = QzoneSurfaceVariant,
    onSurface = QzoneOnSurface,
    onSurfaceVariant = QzoneOnSurfaceVariant,
    outline = QzoneOutline,
    outlineVariant = QzoneOutlineVariant,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = QzoneDarkPrimary,
    onPrimary = QzoneDarkOnPrimary,
    primaryContainer = QzoneDarkPrimaryContainer,
    onPrimaryContainer = QzoneDarkOnPrimaryContainer,
    secondary = QzoneDarkSecondary,
    onSecondary = QzoneDarkOnSecondary,
    secondaryContainer = QzoneDarkSecondaryContainer,
    onSecondaryContainer = QzoneDarkOnSecondaryContainer,
    tertiary = QzoneDarkTertiary,
    onTertiary = QzoneDarkOnTertiary,
    tertiaryContainer = QzoneDarkTertiaryContainer,
    onTertiaryContainer = QzoneDarkOnTertiaryContainer,
    background = QzoneDarkBackground,
    surface = QzoneDarkSurface,
    surfaceVariant = QzoneDarkSurfaceVariant,
    onSurface = QzoneDarkOnSurface,
    onSurfaceVariant = QzoneDarkOnSurfaceVariant,
    outline = QzoneDarkOutline,
    outlineVariant = QzoneDarkOutlineVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
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
        shapes = QzoneShapes,
        content = content
    )
}
