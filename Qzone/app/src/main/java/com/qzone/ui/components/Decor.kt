package com.qzone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke

fun Modifier.qzoneScreenBackground(): Modifier = composed {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.2f
    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF020203),
            Color(0xFF16171F),
            Color(0xFF2C2E37)
        )
    } else {
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE9EBF1),
            Color(0xFFD4D7DF)
        )
    }
    background(Brush.verticalGradient(gradientColors))
}

@Composable
fun QzoneElevatedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    tonalElevation: Dp = 6.dp,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        tonalElevation = tonalElevation,
        shadowElevation = tonalElevation,
        border = BorderStroke(1.dp, borderColor),
        content = content,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    )
}

@Composable
fun QzoneTag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    contentColor: Color = MaterialTheme.colorScheme.primary,
    emphasize: Boolean = false
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .background(
                color = if (emphasize) MaterialTheme.colorScheme.primary else containerColor,
                shape = shape
            )
            .border(
                width = if (emphasize) 0.dp else 1.dp,
                color = if (emphasize) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (emphasize) MaterialTheme.colorScheme.onPrimary else contentColor,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

