package ru.lumen.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ru.lumen.launcher.data.Character
import kotlin.math.cos
import kotlin.math.sin

/**
 * Говорящий инопланетянин: тело, глаза, антенны и рот, который двигается,
 * пока персонаж говорит, плюс волны, когда он слушает.
 */
@Composable
fun AlienCharacter(
    character: Character,
    speaking: Boolean,
    listening: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "alien")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val idle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "idle",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = 1.dp.toPx()
        val center = Offset(w / 2f, h / 2f)
        val bodyWidth = w * 0.62f
        val bodyHeight = h * 0.66f

        // Свечение и волны прослушивания.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(character.glow.copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = w * 0.55f,
            ),
            radius = w * 0.55f,
            center = center,
        )
        if (listening) {
            for (ring in 0..2) {
                val progress = (pulse + ring / 3f) % 1f
                drawCircle(
                    color = character.accent.copy(alpha = (1f - progress) * 0.5f),
                    radius = w * (0.3f + progress * 0.26f),
                    center = center,
                    style = Stroke(width = 2f * unit),
                )
            }
        }

        // Антенны.
        if (character.antennae) {
            val leftAntenna = Offset(center.x - bodyWidth * 0.28f, center.y - bodyHeight * 0.42f)
            val rightAntenna = Offset(center.x + bodyWidth * 0.28f, center.y - bodyHeight * 0.42f)
            listOf(leftAntenna to -1f, rightAntenna to 1f).forEach { (base, direction) ->
                val tip = Offset(
                    base.x + direction * w * 0.05f,
                    base.y - h * 0.14f + sin(idle * 6.28f) * h * 0.01f,
                )
                drawLine(
                    color = character.body.copy(alpha = 0.85f),
                    start = base,
                    end = tip,
                    strokeWidth = 3f * unit,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = character.accent, radius = 5f * unit, center = tip)
            }
        }

        // Тело.
        val bodyTopLeft = Offset(center.x - bodyWidth / 2f, center.y - bodyHeight / 2f)
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(character.body, character.body.copy(alpha = 0.72f)),
                startY = bodyTopLeft.y,
                endY = bodyTopLeft.y + bodyHeight,
            ),
            topLeft = bodyTopLeft,
            size = Size(bodyWidth, bodyHeight),
        )
        drawOval(
            color = character.glow.copy(alpha = 0.5f),
            topLeft = Offset(bodyTopLeft.x + bodyWidth * 0.12f, bodyTopLeft.y + bodyHeight * 0.08f),
            size = Size(bodyWidth * 0.76f, bodyHeight * 0.4f),
        )

        // Глаза.
        val eyeY = center.y - bodyHeight * 0.12f
        val eyeRadius = bodyWidth * 0.075f
        val spacing = bodyWidth * 0.19f
        val eyeOffsets = when (character.eyeCount) {
            2 -> listOf(-spacing, spacing)
            3 -> listOf(-spacing * 1.3f, 0f, spacing * 1.3f)
            else -> listOf(-spacing * 1.5f, -spacing * 0.5f, spacing * 0.5f, spacing * 1.5f)
        }
        eyeOffsets.forEach { dx ->
            drawCircle(color = Color.White.copy(alpha = 0.94f), radius = eyeRadius, center = Offset(center.x + dx, eyeY))
            val look = if (listening) cos(pulse * 6.28f) * eyeRadius * 0.25f else 0f
            drawCircle(
                color = Color(0xFF0B1A20),
                radius = eyeRadius * 0.5f,
                center = Offset(center.x + dx + look, eyeY),
            )
        }

        // Рот: открывается, когда персонаж говорит.
        val mouthWidth = bodyWidth * 0.34f
        val openAmount = when {
            speaking -> 0.4f + 0.6f * kotlin.math.abs(sin(pulse * 12.56f))
            listening -> 0.18f + 0.12f * sin(pulse * 6.28f)
            else -> 0.14f + 0.06f * idle
        }
        val mouthHeight = h * 0.11f * openAmount
        val mouthCenter = Offset(center.x, center.y + bodyHeight * 0.22f)
        drawOval(
            color = Color(0xFF0B1A20).copy(alpha = 0.9f),
            topLeft = Offset(mouthCenter.x - mouthWidth / 2f, mouthCenter.y - mouthHeight / 2f),
            size = Size(mouthWidth, mouthHeight),
        )
        drawOval(
            color = character.accent.copy(alpha = 0.85f),
            topLeft = Offset(
                mouthCenter.x - mouthWidth * 0.3f,
                mouthCenter.y + mouthHeight * 0.16f,
            ),
            size = Size(mouthWidth * 0.6f, mouthHeight * 0.34f),
        )
    }
}