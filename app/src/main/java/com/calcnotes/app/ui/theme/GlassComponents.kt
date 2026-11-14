package com.calcnotes.app.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tintColor: Color? = null,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val supportsRealBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseFill = if (supportsRealBlur) {
        glass.surfaceTranslucent
    } else {
        glass.surfaceFallback
    }

    val surfaceBrush = Brush.linearGradient(
        colors = listOf(
            tintColor?.copy(alpha = 0.22f) ?: baseFill,
            baseFill.copy(alpha = (baseFill.alpha * 0.78f).coerceIn(0.1f, 0.96f))
        )
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            glass.borderLight,
            glass.borderLight.copy(alpha = 0.12f)
        )
    )

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, spotColor = glass.accent.copy(alpha = 0.25f))
            .clip(shape)
            .background(brush = surfaceBrush)
            .border(BorderStroke(1.dp, borderBrush), shape = shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        content = content
    )
}

@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(50)
    val bg = if (selected) glass.accent.copy(alpha = 0.28f) else glass.surfaceTranslucent
    val border = if (selected) glass.accent else glass.borderLight

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        content = content
    )
}
