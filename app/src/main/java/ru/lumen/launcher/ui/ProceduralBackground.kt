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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import ru.lumen.launcher.util.DayPeriod
import ru.lumen.launcher.util.Season
import ru.lumen.launcher.util.WeatherKind
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val phase: Float,
    val speed: Float,
)

/**
 * Процедурный фон «Люмена»: градиент по времени суток, два цветных свечения
 * и медленно дрейфующие частицы. Считается на Canvas, без тяжёлых слоёв.
 */
@Composable
fun ProceduralBackground(
    period: DayPeriod,
    season: Season,
    weather: WeatherKind,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val palette = paletteFor(period, season, nightMode = period == DayPeriod.NIGHT)
    val transition = rememberInfiniteTransition(label = "lumen-background")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    val random = remember(seed, weather) { Random(seed * 97 + 13 + weather.ordinal) }
    val sunX = remember(seed, weather) { 0.18f + random.nextFloat() * 0.64f }
    val sunY = remember(seed, weather) { 0.12f + random.nextFloat() * 0.32f }
    val particleCount = when (weather) {
        WeatherKind.RAIN, WeatherKind.SNOW, WeatherKind.STORM -> 34
        WeatherKind.CLEAR -> 18
        else -> 26
    }
    val particles = remember(seed, weather, particleCount) {
        List(particleCount) {
            Particle(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 1.4f + random.nextFloat() * 5.2f,
                alpha = 0.16f + random.nextFloat() * 0.4f,
                phase = random.nextFloat() * 6.28f,
                speed = 0.18f + random.nextFloat() * 0.5f,
            )
        }
    }

    val night = period == DayPeriod.NIGHT
    val glowStrength = if (night) 0.34f else 0.5f

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxSide = maxOf(width, height)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.base,
                    lerp(palette.base, palette.soft, if (night) 0.16f else 0.3f),
                ),
                startY = 0f,
                endY = height,
            )
        )

        val sunCenter = Offset(sunX * width, sunY * height)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.glow.copy(alpha = glowStrength),
                    palette.glow.copy(alpha = 0f),
                ),
                center = sunCenter,
                radius = maxSide * 0.78f,
            )
        )

        val secondCenter = Offset((1f - sunX) * width, (0.92f - breathe * 0.06f) * height)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.glow2.copy(alpha = glowStrength * 0.8f),
                    palette.glow2.copy(alpha = 0f),
                ),
                center = secondCenter,
                radius = maxSide * 0.62f,
            )
        )

        val baseAlpha = if (night) 1f else 0.9f
        particles.forEach { particle ->
            val travel = (particle.y + drift * particle.speed) % 1f
            val sway = sin(drift * 6.28f + particle.phase) * 0.012f
            val x = ((particle.x + sway) % 1f) * width
            val y = travel * height
            val radius = particle.radius * (maxSide / 900f).coerceIn(0.7f, 2.4f)
            val twinkle = 0.75f + 0.25f * sin(drift * 12f + particle.phase)
            drawCircle(
                color = palette.glow.copy(alpha = particle.alpha * twinkle * baseAlpha),
                radius = radius,
                center = Offset(x, y),
            )
        }

        // Тонкая световая линия у нижней кромки — как в оригинальном фоне.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, palette.accent.copy(alpha = if (night) 0.18f else 0.12f)),
                startY = height * 0.82f,
                endY = height,
            )
        )
    }
}

@Suppress("unused")
private fun offsetOnCircle(angleRad: Float, radius: Float, center: Offset): Offset =
    Offset(center.x + cos(angleRad) * radius, center.y + sin(angleRad) * radius)