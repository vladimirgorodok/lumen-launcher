package ru.lumen.launcher.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ru.lumen.launcher.util.DayPeriod
import ru.lumen.launcher.util.Season

data class LumenPalette(
    val base: Color,
    val glow: Color,
    val glow2: Color,
    val accent: Color,
    val soft: Color,
    val isNight: Boolean,
    val textPrimary: Color,
    val textSecondary: Color,
    val panel: Color,
    val panelBorder: Color,
    val accentText: Color,
)

private val MORNING = LumenPalette(
    base = Color(0xFFFFFAF7),
    glow = Color(0xFFFF9B87),
    glow2 = Color(0xFF33D7DF),
    accent = Color(0xFF20B987),
    soft = Color(0xFFC9F4DF),
    isNight = false,
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    panel = Color(0x99FFFFFF),
    panelBorder = Color(0xB3FFFFFF),
    accentText = Color(0xFF0E7490),
)

private val DAY = MORNING.copy(
    base = Color(0xFFF8FDFF),
    glow = Color(0xFF26CBD5),
    glow2 = Color(0xFFFF7096),
    accent = Color(0xFF13B8CA),
    soft = Color(0xFFC8F3F5),
    textPrimary = Color(0xFF0F172A),
)

private val EVENING = MORNING.copy(
    base = Color(0xFFFFF8FB),
    glow = Color(0xFFFF5F91),
    glow2 = Color(0xFF36D6C1),
    accent = Color(0xFFFF715F),
    soft = Color(0xFFFFD9D1),
)

private val NIGHT = LumenPalette(
    base = Color(0xFF061117),
    glow = Color(0xFF2BD5E3),
    glow2 = Color(0xFFF05AAB),
    accent = Color(0xFF18B8CF),
    soft = Color(0xFF123742),
    isNight = true,
    textPrimary = Color(0xFFEAF6FA),
    textSecondary = Color(0xFF8FAAB6),
    panel = Color(0x14FFFFFF),
    panelBorder = Color(0x24FFFFFF),
    accentText = Color(0xFF67E8F9),
)

private val SEASON_ACCENTS = mapOf(
    Season.SPRING to (Color(0xFF20B987) to Color(0xFFC9F4DF)),
    Season.SUMMER to (Color(0xFF13B8CA) to Color(0xFFC8F3F5)),
    Season.AUTUMN to (Color(0xFFFF715F) to Color(0xFFFFD9D1)),
    Season.WINTER to (Color(0xFF18B8CF) to Color(0xFFD7F3F8)),
)

fun paletteFor(period: DayPeriod, season: Season, nightMode: Boolean): LumenPalette {
    val basePalette = when (period) {
        DayPeriod.MORNING -> MORNING
        DayPeriod.DAY -> DAY
        DayPeriod.EVENING -> EVENING
        DayPeriod.NIGHT -> NIGHT
    }
    val palette = if (nightMode && period != DayPeriod.NIGHT) {
        basePalette.copy(
            isNight = true,
            textPrimary = NIGHT.textPrimary,
            textSecondary = NIGHT.textSecondary,
            panel = NIGHT.panel,
            panelBorder = NIGHT.panelBorder,
            accentText = NIGHT.accentText,
        )
    } else {
        basePalette
    }
    val (accent, soft) = SEASON_ACCENTS.getValue(season)
    return palette.copy(accent = accent, soft = soft)
}

val LocalLumenPalette = staticCompositionLocalOf { DAY }

val Cyan = Color(0xFF26CBD5)
val Fuchsia = Color(0xFFFF5F91)
val Coral = Color(0xFFFF6F61)

val WebAppColors = listOf(
    Color(0xFF10B8C8),
    Color(0xFF10B981),
    Color(0xFFE24B9A),
    Color(0xFFFF6F61),
    Color(0xFF8B5CF6),
    Color(0xFFF2AD3D),
)