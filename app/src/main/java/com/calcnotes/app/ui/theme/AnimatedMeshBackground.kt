package com.calcnotes.app.ui.theme

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedMeshBackground(
    blurRadius: Dp,
    animateEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glass = LocalGlassColors.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = (event == Lifecycle.Event.ON_RESUME)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldAnimate = animateEnabled && isResumed
    val infiniteTransition = rememberInfiniteTransition(label = "meshTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (shouldAnimate) (2f * Math.PI.toFloat()) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "meshPhase"
    )

    val baseBg = when {
        glass.isAmoled -> Color.Black
        glass.isDark -> Color(0xFF090D1A)
        else -> Color(0xFFEFF6FF)
    }

    val supportsRealBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = modifier.fillMaxSize().background(baseBg)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (supportsRealBlur && blurRadius > 0.dp) {
                        Modifier.blur(blurRadius)
                    } else {
                        Modifier
                    }
                )
        ) {
            val w = size.width
            val h = size.height
            val r = minOf(w, h) * 0.68f

            val c1 = Offset(
                x = w * (0.25f + 0.18f * cos(phase)),
                y = h * (0.22f + 0.14f * sin(phase))
            )
            val c2 = Offset(
                x = w * (0.78f - 0.18f * sin(phase * 0.8f)),
                y = h * (0.48f + 0.16f * cos(phase * 0.8f))
            )
            val c3 = Offset(
                x = w * (0.35f + 0.15f * sin(phase * 0.6f)),
                y = h * (0.82f - 0.12f * cos(phase * 0.6f))
            )

            val alpha = if (glass.isAmoled) 0.22f else if (glass.isDark) 0.36f else 0.28f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.accent.copy(alpha = alpha), Color.Transparent),
                    center = c1,
                    radius = r
                ),
                radius = r,
                center = c1
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.accentSecondary.copy(alpha = alpha), Color.Transparent),
                    center = c2,
                    radius = r * 1.1f
                ),
                radius = r * 1.1f,
                center = c2
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF14B8A6).copy(alpha = alpha * 0.85f), Color.Transparent),
                    center = c3,
                    radius = r * 0.95f
                ),
                radius = r * 0.95f,
                center = c3
            )
        }

        content()
    }
}
