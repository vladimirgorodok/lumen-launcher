package ru.lumen.launcher.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.sin

/** Аналоговые часы из «Люмена»: пунктирные кольца, цветные 12/3/6/9 и три стрелки. */
@Composable
fun AnalogClock(
    now: LocalDateTime,
    palette: LumenPalette,
    modifier: Modifier = Modifier,
) {
    val seconds = now.second
    val minutes = now.minute + seconds / 60f
    val hours = (now.hour % 12) + minutes / 60f

    val tickColor = if (palette.isNight) Color(0xFFB7D3DC) else Color(0xFF33414A)
    val ringColor = if (palette.isNight) Color(0x33FFFFFF) else Color(0x291F2937)
    val paint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f
        val unit = 1.dp.toPx()

        drawCircle(
            color = if (palette.isNight) Color(0x14FFFFFF) else Color(0x38FFFFFF),
            radius = radius,
        )
        drawCircle(
            color = ringColor,
            radius = radius - unit,
            style = Stroke(width = 1.2f * unit),
        )
        drawCircle(
            color = palette.accent.copy(alpha = 0.24f),
            radius = radius * 0.92f,
            style = Stroke(
                width = unit,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * unit, 5f * unit)),
            ),
        )
        drawCircle(
            color = palette.glow2.copy(alpha = 0.16f),
            radius = radius * 0.78f,
            style = Stroke(width = unit),
        )

        for (index in 0 until 60) {
            val angle = Math.toRadians((index * 6 - 90).toDouble())
            val major = index % 5 == 0
            val outer = radius * 0.96f
            val inner = if (major) radius * 0.87f else radius * 0.925f
            val start = Offset(
                center.x + (cos(angle) * inner).toFloat(),
                center.y + (sin(angle) * inner).toFloat(),
            )
            val end = Offset(
                center.x + (cos(angle) * outer).toFloat(),
                center.y + (sin(angle) * outer).toFloat(),
            )
            drawLine(
                color = tickColor.copy(alpha = if (major) 0.72f else 0.28f),
                start = start,
                end = end,
                strokeWidth = if (major) 2f * unit else unit,
            )
        }

        val numberSize = radius * 0.17f
        paint.textSize = numberSize
        paint.textAlign = Paint.Align.CENTER
        val numbers = listOf(
            Triple("12", 0f, -radius * 0.66f),
            Triple("3", radius * 0.66f, 0f),
            Triple("6", 0f, radius * 0.66f),
            Triple("9", -radius * 0.66f, 0f),
        )
        val numberColors = listOf(
            if (palette.isNight) Color(0xFFD7E9EF) else Color(0xFF3B4A52),
            Coral,
            Color(0xFF10B981),
            Fuchsia,
        )
        numbers.forEachIndexed { index, (text, dx, dy) ->
            paint.color = numberColors[index].copy(alpha = 0.92f).toArgb()
            val baseline = center.y + dy - (paint.ascent() + paint.descent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(text, center.x + dx, baseline, paint)
        }

        fun hand(angleDegrees: Float, length: Float, width: Float, color: Color, tail: Float = 0f) {
            val angle = Math.toRadians((angleDegrees - 90).toDouble())
            val start = Offset(
                center.x - (cos(angle) * tail).toFloat(),
                center.y - (sin(angle) * tail).toFloat(),
            )
            val end = Offset(
                center.x + (cos(angle) * length).toFloat(),
                center.y + (sin(angle) * length).toFloat(),
            )
            drawLine(color = color, start = start, end = end, strokeWidth = width, cap = StrokeCap.Round)
        }

        hand(
            hours * 30f,
            radius * 0.46f,
            4.2f * unit,
            if (palette.isNight) Color(0xFFE8F4F8) else Color(0xFF202A31),
        )
        hand(
            minutes * 6f,
            radius * 0.64f,
            2.8f * unit,
            if (palette.isNight) Color(0xFF5DE0EC) else Color(0xFF0891B2),
        )
        hand(seconds * 6f, radius * 0.78f, 1.4f * unit, Fuchsia, tail = radius * 0.1f)

        drawCircle(color = Coral, radius = radius * 0.045f, center = center)
        drawCircle(
            color = if (palette.isNight) Color(0xFF061117) else Color.White,
            radius = radius * 0.02f,
            center = center,
        )
    }
}