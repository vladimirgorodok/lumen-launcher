package ru.lumen.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import ru.lumen.launcher.data.AgentFrame
import ru.lumen.launcher.data.AgentMood
import ru.lumen.launcher.data.AgentRenderer
import ru.lumen.launcher.data.AgentRig
import ru.lumen.launcher.data.Animation
import ru.lumen.launcher.data.Character
import ru.lumen.launcher.data.FaceFrame

/**
 * Живой 3D-персонаж: рисуется своим мини-движком (меш, скелет, мимика),
 * без внешних моделей и библиотек. Пока чат бездействует, персонаж сам
 * придумывает смешные движения.
 */
@Composable
fun AgentView(
    character: Character,
    mood: AgentMood,
    activityTick: Int,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    forced: Animation? = null,
    forcedPhase: Float = 0f,
) {
    val rig = remember(character.id) { AgentRig(character) }
    val renderer = remember(rig) { AgentRenderer(rig) }
    val runtime = remember(rig) { AgentRuntime() }
    val clock = remember { mutableFloatStateOf(0f) }

    if (animated) {
        LaunchedEffect(rig) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last == 0L) {
                        last = now
                    } else {
                        val delta = now - last
                        // ~30 кадров в секунду: плавно и без лишней нагрузки
                        if (delta >= 33_000_000L) {
                            clock.value += delta / 1_000_000_000f
                            last = now
                        }
                    }
                }
            }
        }
    }

    val glow = character.glow
    Canvas(modifier = modifier) {
        if (size.width < 4f || size.height < 4f) return@Canvas
        val time = if (animated) clock.value else 1.7f
        if (runtime.tick != activityTick) {
            runtime.tick = activityTick
            runtime.idleStart = time
            runtime.seed = activityTick * 2654435761L.toInt() xor 0x2F1A
        }
        renderer.build(
            size.width,
            size.height,
            time,
            mood,
            runtime.seed,
            runtime.idleStart,
            forced,
            forcedPhase,
        )
        drawAgentFrame(renderer.frame(), glow)
    }
}

/** Запоминает, когда персонаж последний раз «общались», чтобы он заскучал. */
private class AgentRuntime {
    var tick = Int.MIN_VALUE
    var idleStart = 0f
    var seed = 7
}

private fun DrawScope.drawAgentFrame(frame: AgentFrame, glow: Color) {
    val centerX = size.width / 2f
    val glowRadius = size.minDimension * 0.62f
    val glowCenter = Offset(centerX, size.height * 0.42f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glow.copy(alpha = 0.22f),
                glow.copy(alpha = 0.07f),
                Color.Transparent,
            ),
            center = glowCenter,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = glowCenter,
    )

    if (frame.shadowVisible && frame.shadowRx > 1f) {
        drawOval(
            color = Color.Black.copy(alpha = frame.shadowAlpha),
            topLeft = Offset(frame.shadowX - frame.shadowRx, frame.shadowY - frame.shadowRy),
            size = Size(frame.shadowRx * 2f, frame.shadowRy * 2f),
        )
    }

    val path = Path()
    for (index in 0 until frame.triangles) {
        val offset = index * 6
        val x0 = frame.xy[offset]
        val y0 = frame.xy[offset + 1]
        val x1 = frame.xy[offset + 2]
        val y1 = frame.xy[offset + 3]
        val x2 = frame.xy[offset + 4]
        val y2 = frame.xy[offset + 5]
        // чуть раздуваем треугольник от центра: это убирает швы
        // между соседними гранями при сглаживании
        val cx = (x0 + x1 + x2) / 3f
        val cy = (y0 + y1 + y2) / 3f
        path.reset()
        path.moveTo(inflate(x0, y0, cx, cy, 0), inflate(x0, y0, cx, cy, 1))
        path.lineTo(inflate(x1, y1, cx, cy, 0), inflate(x1, y1, cx, cy, 1))
        path.lineTo(inflate(x2, y2, cx, cy, 0), inflate(x2, y2, cx, cy, 1))
        path.close()
        drawPath(path, Color(frame.color[index]), style = Fill)
    }

    drawAgentFace(frame.face)
}

/** Сдвигает вершину от центра треугольника на доли пикселя. */
private fun inflate(x: Float, y: Float, cx: Float, cy: Float, axis: Int): Float {
    val dx = x - cx
    val dy = y - cy
    val length = kotlin.math.sqrt(dx * dx + dy * dy)
    if (length < 0.01f) return if (axis == 0) x else y
    val scale = (length + 0.4f) / length
    return if (axis == 0) cx + dx * scale else cy + dy * scale
}

/** Мимика рисуется поверх тела: глаза, зрачки, брови, рот, румянец. */
private fun DrawScope.drawAgentFace(face: FaceFrame) {
    if (!face.visible) return
    val eyeRadius = face.scale * 0.26f
    val dark = Color(0xFF181C28)
    val rollDegrees = Math.toDegrees(face.roll.toDouble()).toFloat()

    fun eye(index: Int) {
        val cx = face.eyeX[index]
        val cy = face.eyeY[index]
        val rx = eyeRadius * face.squash
        val ry = eyeRadius * face.eyeOpen * (1f - face.blink)
        rotate(rollDegrees, Offset(cx, cy)) {
            if (face.blink > 0.82f || ry < 0.6f) {
                drawLine(
                    color = dark,
                    start = Offset(cx - rx, cy),
                    end = Offset(cx + rx, cy),
                    strokeWidth = eyeRadius * 0.22f,
                    cap = StrokeCap.Round,
                )
                return@rotate
            }
            drawOval(
                color = Color(0xFFF4FAFF),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2f, ry * 2f),
            )
            val pupilRadius = eyeRadius * 0.44f
            val px = cx + face.pupilX * eyeRadius * 0.34f
            val py = cy + face.pupilY * eyeRadius * 0.30f
            drawCircle(dark, pupilRadius, Offset(px, py))
            drawCircle(
                Color.White.copy(alpha = 0.86f),
                pupilRadius * 0.42f,
                Offset(px - pupilRadius * 0.55f, py - pupilRadius * 0.55f),
            )
        }
    }

    for (index in 0 until face.eyeCount) eye(index)

    val browColor = Color(0xFF181C28).copy(alpha = 0.82f)
    for (index in 0 until face.browCount) {
        val cx = face.browX[index]
        val cy = face.browY[index]
        val half = eyeRadius * 0.85f
        val lift = face.brow * eyeRadius * 0.55f
        val direction = if (index == 0) -1f else 1f
        rotate(rollDegrees, Offset(cx, cy)) {
            drawLine(
                color = browColor,
                start = Offset(cx - half, cy - lift * direction),
                end = Offset(cx + half, cy + lift * direction),
                strokeWidth = eyeRadius * 0.26f,
                cap = StrokeCap.Round,
            )
        }
    }

    val mouthX = face.mouthX
    val mouthY = face.mouthY
    val half = face.scale * 0.30f * face.mouthWidth * face.squash
    rotate(rollDegrees, Offset(mouthX, mouthY)) {
        if (face.mouthOpen < 0.12f) {
            val curve = face.mouthCurve * half * 0.75f
            val mouth = Path()
            mouth.moveTo(mouthX - half, mouthY - curve * 0.25f)
            mouth.quadraticTo(mouthX, mouthY + curve, mouthX + half, mouthY - curve * 0.25f)
            drawPath(
                mouth,
                Color(0xFF181C28).copy(alpha = 0.9f),
                style = Stroke(width = face.scale * 0.055f, cap = StrokeCap.Round),
            )
        } else {
            val height = face.scale * (0.05f + 0.20f * face.mouthOpen)
            drawOval(
                color = Color(0xFF2A101C),
                topLeft = Offset(mouthX - half, mouthY - height * 0.5f),
                size = Size(half * 2f, height),
            )
            if (face.mouthOpen > 0.35f) {
                drawOval(
                    color = Color(0xFFFF5F91),
                    topLeft = Offset(mouthX - half * 0.6f, mouthY + height * 0.05f),
                    size = Size(half * 1.2f, height * 0.5f),
                )
            }
        }
    }

    if (face.cheek > 0.05f) {
        val radius = face.scale * 0.20f
        val color = Color(0xFFFF7A9E).copy(alpha = (face.cheek * 0.6f).coerceIn(0f, 1f))
        for (side in listOf(-1f, 1f)) {
            drawOval(
                color = color,
                topLeft = Offset(face.cheekX * side - radius, face.cheekY - radius * 0.7f),
                size = Size(radius * 2f, radius * 1.4f),
            )
        }
    }
}

/** Режим для настроек: показать персонажа в покое, без расписания шуток. */
@Composable
fun AgentPreview(
    character: Character,
    modifier: Modifier = Modifier,
    mood: AgentMood = AgentMood.IDLE,
) {
    AgentView(
        character = character,
        mood = mood,
        activityTick = 0,
        modifier = modifier,
        animated = true,
    )
}
