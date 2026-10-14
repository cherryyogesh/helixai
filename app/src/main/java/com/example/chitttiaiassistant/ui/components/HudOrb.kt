package com.example.chitttiaiassistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chitttiaiassistant.model.AssistantState
import com.example.chitttiaiassistant.ui.theme.AccentError
import com.example.chitttiaiassistant.ui.theme.BrightCyan
import com.example.chitttiaiassistant.ui.theme.ElectricBlue
import com.example.chitttiaiassistant.ui.theme.NeonCyan
import com.example.chitttiaiassistant.ui.theme.SurfaceDark

@Composable
fun HudOrb(
    state: AssistantState,
    audioLevel: Float = 0f,
    size: Dp = 180.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val (coreColor, glowColor) = when (state) {
        AssistantState.IDLE, AssistantState.DISABLED -> Pair(NeonCyan, ElectricBlue)
        AssistantState.PERMISSION_REQUIRED -> Pair(Color(0xFFFFB703), Color(0x88FFB703))
        AssistantState.LISTENING, AssistantState.WAKE_WORD_DETECTED -> Pair(NeonCyan, BrightCyan)
        AssistantState.THINKING, AssistantState.PROCESSING -> Pair(ElectricBlue, NeonCyan)
        AssistantState.SPEAKING -> Pair(BrightCyan, ElectricBlue)
        AssistantState.ERROR -> Pair(AccentError, Color(0xFFFF758F))
    }

    Box(
        modifier = Modifier
            .size(size)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Aura
        Box(
            modifier = Modifier
                .size(size * if (state == AssistantState.LISTENING || state == AssistantState.WAKE_WORD_DETECTED) pulseScale else 1f)
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    ambientColor = glowColor,
                    spotColor = coreColor
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.35f),
                            coreColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Outer Rotating HUD Dashed Ring
        Canvas(
            modifier = Modifier
                .size(size * 0.95f)
                .rotate(if (state == AssistantState.THINKING || state == AssistantState.PROCESSING) rotation * 2 else rotation)
        ) {
            val strokeWidth = 3.dp.toPx()
            val dashPattern = floatArrayOf(20f, 15f, 40f, 15f)
            drawCircle(
                color = coreColor.copy(alpha = 0.7f),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(dashPattern, 0f)
                )
            )
        }

        // Inner Core Ring
        Box(
            modifier = Modifier
                .size(size * 0.7f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = 0.8f),
                            glowColor.copy(alpha = 0.5f),
                            SurfaceDark
                        )
                    )
                )
                .border(2.dp, coreColor.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Audio Waveform Bar Visualizer inside core
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.toPx() * 0.35f
                val barCount = 7
                val barWidth = 6.dp.toPx()
                val spacing = 8.dp.toPx()
                val startX = (size.toPx() * 0.7f - (barCount * barWidth + (barCount - 1) * spacing)) / 2

                for (i in 0 until barCount) {
                    val factor = when (i) {
                        0, 6 -> 0.3f
                        1, 5 -> 0.6f
                        2, 4 -> 0.8f
                        else -> 1.0f
                    }
                    val amplitude = if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING || state == AssistantState.WAKE_WORD_DETECTED) {
                        (audioLevel.coerceIn(1f, 30f) / 30f) * factor * 40.dp.toPx()
                    } else {
                        10.dp.toPx() * factor
                    }

                    val x = startX + i * (barWidth + spacing)
                    val top = centerY - amplitude / 2
                    val bottom = centerY + amplitude / 2

                    drawLine(
                        color = TextPrimaryColor,
                        start = androidx.compose.ui.geometry.Offset(x, top),
                        end = androidx.compose.ui.geometry.Offset(x, bottom),
                        strokeWidth = barWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

private val TextPrimaryColor = Color(0xFFFFFFFF)
