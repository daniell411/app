package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ApuntaIndigo
import com.example.ui.theme.ApuntaIndigoLight
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isListening: Boolean,
    amplitude: Float, // 0..1
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f, // 2*PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val barCount = 28
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3f)
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.9f

        val gradient = Brush.verticalGradient(
            colors = listOf(ApuntaIndigoLight, ApuntaIndigo, ApuntaIndigoLight),
            startY = 0f,
            endY = size.height
        )

        for (i in 0 until barCount) {
            val x = i * (barWidth + spacing)
            val normalizedIndex = i.toFloat() / barCount.toFloat()

            // Dynamic sine wave height + amplitude reaction
            val wave = if (isListening) {
                val base = (sin(normalizedIndex * 5.0 + phase) * 0.5 + 0.5).toFloat()
                val ampBoost = amplitude.coerceIn(0.15f, 1.0f)
                (base * 0.6f + ampBoost * 0.4f).coerceIn(0.12f, 1.0f)
            } else {
                0.15f
            }

            val barHeight = (maxBarHeight * wave).coerceAtLeast(6f)
            val top = centerY - (barHeight / 2f)

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
