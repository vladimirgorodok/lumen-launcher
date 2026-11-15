package ru.lumen.launcher.data

import androidx.compose.ui.graphics.Color

/** Персонаж ИИ-вкладки: силуэт рисуется на Canvas, голос — системный TTS. */
data class Character(
    val id: String,
    val name: String,
    val tagline: String,
    val body: Color,
    val glow: Color,
    val accent: Color,
    val eyeCount: Int,
    val antennae: Boolean,
    val pitch: Float,
    val style: String,
)

val Characters: List<Character> = listOf(
    Character(
        id = "lumen",
        name = "Люмен",
        tagline = "спокойный хранитель времени",
        body = Color(0xFF2BD5E3),
        glow = Color(0xFF26CBD5),
        accent = Color(0xFFFF5F91),
        eyeCount = 2,
        antennae = false,
        pitch = 1.0f,
        style = "Говорит коротко и тепло, любит точность во времени и погоде.",
    ),
    Character(
        id = "zircon",
        name = "Циркон",
        tagline = "кристальный советник",
        body = Color(0xFF8B5CF6),
        glow = Color(0xFFA78BFA),
        accent = Color(0xFF22D3EE),
        eyeCount = 3,
        antennae = true,
        pitch = 0.86f,
        style = "Рассуждает спокойно и структурно, любит списки и порядок.",
    ),
    Character(
        id = "myata",
        name = "Мята",
        tagline = "живой и любопытный",
        body = Color(0xFF10B981),
        glow = Color(0xFF34D399),
        accent = Color(0xFFF2AD3D),
        eyeCount = 2,
        antennae = true,
        pitch = 1.24f,
        style = "Отвечает живо и просто, задаёт уточняющие вопросы.",
    ),
    Character(
        id = "nocturne",
        name = "Ноктюрн",
        tagline = "ночной наблюдатель",
        body = Color(0xFF6366F1),
        glow = Color(0xFF818CF8),
        accent = Color(0xFFF472B6),
        eyeCount = 4,
        antennae = false,
        pitch = 0.72f,
        style = "Говорит сдержанно и загадочно, но всегда по делу.",
    ),
)

fun characterById(id: String?): Character = Characters.firstOrNull { it.id == id } ?: Characters.first()