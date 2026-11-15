package ru.lumen.launcher.data

import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Настроение персонажа: от него зависит поза и лицо. */
enum class AgentMood { IDLE, TALK, LISTEN, THINK }

/** Мимика: глаза, зрачки, брови, рот. Рисуется поверх 3D-тела. */
class FaceState {
    var blink = 0f
    var mouthOpen = 0f
    var mouthCurve = 0.4f
    var mouthWidth = 1f
    var pupilX = 0f
    var pupilY = 0f
    var brow = 0.15f
    var eyeOpen = 1f
    var cheek = 0f

    fun copyFrom(other: FaceState) {
        blink = other.blink
        mouthOpen = other.mouthOpen
        mouthCurve = other.mouthCurve
        mouthWidth = other.mouthWidth
        pupilX = other.pupilX
        pupilY = other.pupilY
        brow = other.brow
        eyeOpen = other.eyeOpen
        cheek = other.cheek
    }

    fun mixFrom(a: FaceState, b: FaceState, t: Float) {
        blink = Num.lerp(a.blink, b.blink, t)
        mouthOpen = Num.lerp(a.mouthOpen, b.mouthOpen, t)
        mouthCurve = Num.lerp(a.mouthCurve, b.mouthCurve, t)
        mouthWidth = Num.lerp(a.mouthWidth, b.mouthWidth, t)
        pupilX = Num.lerp(a.pupilX, b.pupilX, t)
        pupilY = Num.lerp(a.pupilY, b.pupilY, t)
        brow = Num.lerp(a.brow, b.brow, t)
        eyeOpen = Num.lerp(a.eyeOpen, b.eyeOpen, t)
        cheek = Num.lerp(a.cheek, b.cheek, t)
    }
}

/** Смешное движение: поза и лицо как функции фазы 0..1. */
class Animation(
    val id: String,
    val title: String,
    val duration: Float,
    val pose: (phase: Float, pose: Pose) -> Unit,
    val face: (phase: Float, face: FaceState) -> Unit = { _, _ -> },
)

/** Ключ к «живости»: пока чат бездействует, персонаж сам себя развлекает. */
object AgentAnimations {

    private const val TAU = 6.2831855f

    val all: List<Animation> = listOf(
        Animation("dance", "Танец", 4.4f, { p, pose ->
            val s = sin(p * TAU * 3f)
            val s2 = sin(p * TAU * 3f + 1.1f)
            pose.add(Joint.ROOT, 0f, s * 9f, s2 * 6f)
            pose.move(Joint.ROOT, s * 0.035f, abs(s) * 0.02f, 0f)
            pose.add(Joint.SPINE, 0f, -s * 5f, 0f)
            pose.add(Joint.CHEST, 0f, s * 7f, s2 * 4f)
            pose.add(Joint.HEAD, -6f, s * 12f, s2 * 7f)
            pose.set(Joint.ARM_L, -70f - s * 45f, 0f, -55f - s2 * 25f)
            pose.set(Joint.FOREARM_L, -35f + s * 30f, 0f, 0f)
            pose.set(Joint.ARM_R, -70f + s * 45f, 0f, 55f + s2 * 25f)
            pose.set(Joint.FOREARM_R, -35f - s * 30f, 0f, 0f)
            pose.set(Joint.THIGH_L, -s * 12f, 0f, -3f)
            pose.set(Joint.SHIN_L, abs(s) * 22f, 0f, 0f)
            pose.set(Joint.THIGH_R, s * 12f, 0f, 3f)
            pose.set(Joint.SHIN_R, abs(s2) * 22f, 0f, 0f)
        }, { p, f ->
            f.mouthCurve = 0.9f
            f.mouthOpen = 0.25f + 0.3f * abs(sin(p * TAU * 3f))
            f.brow = 0.5f
        }),

        Animation("jump", "Прыжок", 1.6f, { p, pose ->
            val crouch = when {
                p < 0.22f -> Num.smooth(p / 0.22f)
                p < 0.62f -> 0f
                else -> 1f - Num.smooth((p - 0.62f) / 0.38f)
            }
            val air = when {
                p < 0.22f -> 0f
                p < 0.62f -> sin((p - 0.22f) / 0.40f * Math.PI.toFloat())
                else -> 0f
            }
            pose.move(Joint.ROOT, 0f, air * 0.24f - crouch * 0.10f, 0f)
            pose.add(Joint.THIGH_L, -crouch * 45f + air * 35f, 0f, 0f)
            pose.add(Joint.SHIN_L, crouch * 75f - air * 55f, 0f, 0f)
            pose.add(Joint.THIGH_R, -crouch * 45f + air * 35f, 0f, 0f)
            pose.add(Joint.SHIN_R, crouch * 75f - air * 55f, 0f, 0f)
            pose.add(Joint.SPINE, crouch * 12f, 0f, 0f)
            pose.set(Joint.ARM_L, -crouch * 30f - air * 150f, 0f, -20f - air * 25f)
            pose.set(Joint.ARM_R, -crouch * 30f - air * 150f, 0f, 20f + air * 25f)
            pose.set(Joint.FOREARM_L, -15f - air * 20f, 0f, 0f)
            pose.set(Joint.FOREARM_R, -15f - air * 20f, 0f, 0f)
        }, { p, f ->
            f.mouthOpen = if (p in 0.25f..0.6f) 0.75f else 0.1f
            f.mouthCurve = 0.8f
            f.brow = if (p in 0.2f..0.65f) 0.9f else 0.1f
            f.eyeOpen = if (p in 0.22f..0.6f) 1.25f else 1f
        }),

        Animation("wave", "Приветствие", 2.6f, { p, pose ->
            val w = sin(p * TAU * 3.5f)
            pose.set(Joint.ARM_R, -145f, 0f, 22f)
            pose.set(Joint.FOREARM_R, -25f, 0f, w * 35f)
            pose.set(Joint.HAND_R, 0f, 0f, w * 20f)
            pose.set(Joint.ARM_L, 4f, 0f, -10f)
            pose.add(Joint.HEAD, -4f, -8f, -6f)
            pose.add(Joint.CHEST, 0f, 4f, 0f)
        }, { p, f ->
            f.mouthCurve = 0.85f
            f.mouthOpen = 0.15f + 0.2f * abs(sin(p * TAU * 3f))
            f.brow = 0.4f
            f.pupilX = 0.35f
        }),

        Animation("robot", "Робот", 3.2f, { p, pose ->
            val step = if ((p * 8f).toInt() % 2 == 0) 1f else -1f
            val snap = 1f - 0.35f * abs(sin(p * TAU * 8f))
            pose.add(Joint.ROOT, 0f, step * 6f, 0f)
            pose.set(Joint.ARM_L, -90f, 0f, -90f * snap)
            pose.set(Joint.FOREARM_L, -90f * snap, 0f, 0f)
            pose.set(Joint.ARM_R, -90f, 0f, 90f * snap)
            pose.set(Joint.FOREARM_R, -90f * snap, 0f, 0f)
            pose.add(Joint.HEAD, 0f, step * 14f, 0f)
            pose.add(Joint.CHEST, 0f, -step * 5f, 0f)
        }, { _, f ->
            f.mouthCurve = 0f
            f.mouthWidth = 0.8f
            f.brow = -0.1f
            f.eyeOpen = 0.9f
        }),

        Animation("bow", "Поклон", 2.8f, { p, pose ->
            val deep = when {
                p < 0.3f -> Num.smooth(p / 0.3f)
                p < 0.7f -> 1f
                else -> 1f - Num.smooth((p - 0.7f) / 0.3f)
            }
            pose.add(Joint.SPINE, -deep * 24f, 0f, 0f)
            pose.add(Joint.CHEST, -deep * 14f, 0f, 0f)
            pose.add(Joint.HEAD, -deep * 6f, 0f, 0f)
            pose.add(Joint.THIGH_L, deep * 12f, 0f, 0f)
            pose.add(Joint.THIGH_R, deep * 12f, 0f, 0f)
            pose.set(Joint.ARM_L, 20f * deep, 0f, -14f)
            pose.set(Joint.ARM_R, 20f * deep, 0f, 14f)
            pose.set(Joint.FOREARM_L, -30f * deep, 0f, 0f)
            pose.set(Joint.FOREARM_R, -30f * deep, 0f, 0f)
        }, { p, f ->
            f.mouthCurve = 0.6f
            f.brow = 0.3f
            f.pupilY = if (p in 0.3f..0.7f) -0.6f else 0f
        }),

        Animation("laugh", "Смех", 2.8f, { p, pose ->
            val b = sin(p * TAU * 4f)
            pose.move(Joint.ROOT, 0f, abs(b) * 0.035f, 0f)
            pose.add(Joint.SPINE, 6f + b * 5f, 0f, 0f)
            pose.add(Joint.CHEST, 8f + b * 6f, 0f, 0f)
            pose.add(Joint.HEAD, -26f + b * 8f, 0f, b * 4f)
            pose.set(Joint.ARM_L, -50f + b * 18f, 0f, -30f)
            pose.set(Joint.FOREARM_L, -80f, 0f, 0f)
            pose.set(Joint.ARM_R, -50f + b * 18f, 0f, 30f)
            pose.set(Joint.FOREARM_R, -80f, 0f, 0f)
        }, { p, f ->
            f.mouthOpen = 0.55f + 0.4f * abs(sin(p * TAU * 4f))
            f.mouthCurve = 1f
            f.mouthWidth = 1.25f
            f.eyeOpen = 0.75f
            f.brow = 0.5f
        }),

        Animation("stretch", "Потягивание", 3.4f, { p, pose ->
            val up = when {
                p < 0.25f -> Num.smooth(p / 0.25f)
                p < 0.72f -> 1f
                else -> 1f - Num.smooth((p - 0.72f) / 0.28f)
            }
            pose.set(Joint.ARM_L, -165f * up, 0f, -25f * up)
            pose.set(Joint.ARM_R, -165f * up, 0f, 25f * up)
            pose.set(Joint.FOREARM_L, -12f, 0f, 0f)
            pose.set(Joint.FOREARM_R, -12f, 0f, 0f)
            pose.add(Joint.SPINE, 6f * up, 0f, 0f)
            pose.add(Joint.HEAD, -12f * up, 0f, 0f)
            pose.move(Joint.ROOT, 0f, 0.02f * up, 0f)
        }, { p, f ->
            f.mouthOpen = if (p in 0.3f..0.8f) 0.7f else 0.1f
            f.mouthCurve = 0.2f
            f.eyeOpen = 0.55f
            f.brow = 0.35f
        }),

        Animation("squat", "Приседания", 3.0f, { p, pose ->
            val k = (1f - abs(sin(p * TAU * 2f))) * 0.9f
            pose.move(Joint.ROOT, 0f, -k * 0.22f, 0f)
            pose.add(Joint.THIGH_L, -k * 55f, 0f, -6f)
            pose.add(Joint.SHIN_L, k * 95f, 0f, 0f)
            pose.add(Joint.THIGH_R, -k * 55f, 0f, 6f)
            pose.add(Joint.SHIN_R, k * 95f, 0f, 0f)
            pose.add(Joint.SPINE, k * 14f, 0f, 0f)
            pose.set(Joint.ARM_L, -85f * k, 0f, -18f)
            pose.set(Joint.ARM_R, -85f * k, 0f, 18f)
            pose.set(Joint.FOREARM_L, -20f, 0f, 0f)
            pose.set(Joint.FOREARM_R, -20f, 0f, 0f)
        }, { p, f ->
            f.mouthOpen = 0.35f * (1f - abs(sin(p * TAU * 2f)))
            f.mouthCurve = 0.4f
        }),

        Animation("spin", "Пируэт", 2.4f, { p, pose ->
            pose.add(Joint.ROOT, 0f, p * 360f, 0f)
            pose.set(Joint.ARM_L, -20f, 0f, -78f)
            pose.set(Joint.ARM_R, -20f, 0f, 78f)
            pose.set(Joint.FOREARM_L, -10f, 0f, 0f)
            pose.set(Joint.FOREARM_R, -10f, 0f, 0f)
            pose.add(Joint.HEAD, 0f, 0f, -6f)
        }, { _, f ->
            f.mouthCurve = 0.7f
            f.mouthOpen = 0.3f
            f.brow = 0.6f
            f.eyeOpen = 1.1f
        }),

        Animation("moonwalk", "Лунная походка", 3.2f, { p, pose ->
            val s = sin(p * TAU * 2.5f)
            pose.add(Joint.SPINE, 8f, 0f, 0f)
            pose.add(Joint.CHEST, -4f, 0f, s * 5f)
            pose.add(Joint.HEAD, 4f, s * 8f, 0f)
            pose.set(Joint.THIGH_L, -s * 30f, 0f, -4f)
            pose.set(Joint.SHIN_L, (0.5f + s * 0.5f) * 40f, 0f, 0f)
            pose.set(Joint.FOOT_L, -s * 20f, 0f, 0f)
            pose.set(Joint.THIGH_R, s * 30f, 0f, 4f)
            pose.set(Joint.SHIN_R, (0.5f - s * 0.5f) * 40f, 0f, 0f)
            pose.set(Joint.FOOT_R, s * 20f, 0f, 0f)
            pose.set(Joint.ARM_L, -30f - s * 25f, 0f, -35f)
            pose.set(Joint.ARM_R, -30f + s * 25f, 0f, 35f)
            pose.set(Joint.FOREARM_L, -45f, 0f, 0f)
            pose.set(Joint.FOREARM_R, -45f, 0f, 0f)
        }, { _, f ->
            f.mouthCurve = 0.75f
            f.brow = 0.4f
            f.pupilX = 0.3f
        }),

        Animation("peekaboo", "Ку-ку", 2.6f, { p, pose ->
            val cover = when {
                p < 0.3f -> Num.smooth(p / 0.3f)
                p < 0.6f -> 1f
                else -> 1f - Num.smooth((p - 0.6f) / 0.4f)
            }
            pose.set(Joint.ARM_L, -130f * cover, 0f, -18f * cover)
            pose.set(Joint.ARM_R, -130f * cover, 0f, 18f * cover)
            pose.set(Joint.FOREARM_L, -95f * cover, 0f, 0f)
            pose.set(Joint.FOREARM_R, -95f * cover, 0f, 0f)
            pose.add(Joint.HEAD, 5f * cover, 0f, 0f)
        }, { p, f ->
            val hidden = p in 0.32f..0.58f
            f.eyeOpen = if (hidden) 0.05f else 1.2f
            f.mouthOpen = if (hidden) 0.05f else 0.45f
            f.mouthCurve = 0.8f
            f.brow = if (hidden) 0f else 0.9f
        }),

        Animation("flex", "Силач", 2.8f, { p, pose ->
            val t = sin(p * TAU * 2f)
            pose.set(Joint.ARM_L, -35f, 0f, -100f)
            pose.set(Joint.FOREARM_L, -130f, 0f, 0f)
            pose.set(Joint.ARM_R, -35f, 0f, 100f)
            pose.set(Joint.FOREARM_R, -130f, 0f, 0f)
            pose.add(Joint.CHEST, -4f, 0f, t * 3f)
            pose.add(Joint.HEAD, -6f, t * 6f, 0f)
        }, { p, f ->
            f.mouthCurve = 0.55f
            f.mouthWidth = 0.85f
            f.brow = -0.35f
            f.eyeOpen = 0.85f
            f.cheek = 0.4f + 0.3f * abs(sin(p * TAU * 2f))
        }),

        Animation("clap", "Аплодисменты", 2.6f, { p, pose ->
            val clap = (sin(p * TAU * 4f) + 1f) / 2f
            pose.set(Joint.ARM_L, -80f, 0f, -18f - clap * 12f)
            pose.set(Joint.ARM_R, -80f, 0f, 18f + clap * 12f)
            pose.set(Joint.FOREARM_L, -75f, 0f, -clap * 20f)
            pose.set(Joint.FOREARM_R, -75f, 0f, clap * 20f)
            pose.add(Joint.HEAD, -4f - clap * 4f, 0f, 0f)
            pose.move(Joint.ROOT, 0f, clap * 0.012f, 0f)
        }, { p, f ->
            f.mouthCurve = 0.85f
            f.mouthOpen = 0.2f + 0.25f * ((sin(p * TAU * 4f) + 1f) / 2f)
            f.brow = 0.45f
        }),

        Animation("shy", "Смущение", 3.0f, { p, pose ->
            val cover = when {
                p < 0.28f -> Num.smooth(p / 0.28f)
                p < 0.75f -> 1f
                else -> 1f - Num.smooth((p - 0.75f) / 0.25f)
            }
            pose.set(Joint.ARM_L, -120f * cover, 0f, -30f * cover)
            pose.set(Joint.FOREARM_L, -120f * cover, 0f, 0f)
            pose.set(Joint.ARM_R, -120f * cover, 0f, 30f * cover)
            pose.set(Joint.FOREARM_R, -120f * cover, 0f, 0f)
            pose.add(Joint.HEAD, 14f * cover, -6f * cover, 0f)
            pose.add(Joint.CHEST, 6f * cover, 0f, 0f)
        }, { p, f ->
            f.eyeOpen = 0.6f
            f.pupilY = -0.5f
            f.mouthCurve = 0.25f
            f.cheek = 0.9f
            f.brow = 0.25f
        }),
    )

    /**
     * Что персонаж делает сейчас, пока чат бездействует. Расписание считается
     * из времени и зерна, поэтому не нуждается в состоянии и не повторяется.
     */
    fun funnyAt(seed: Int, idleStart: Float, now: Float): Pair<Animation, Float>? {
        if (now <= idleStart) return null
        var cursor = idleStart
        var index = 0
        while (index < 96) {
            val random = Random(seed + index * 7919)
            val gap = 12f + random.nextInt(26)
            val start = cursor + gap
            val animation = all[random.nextInt(all.size)]
            if (now >= start && now <= start + animation.duration) {
                return animation to ((now - start) / animation.duration)
            }
            if (start > now) return null
            cursor = start + animation.duration
            index += 1
        }
        return null
    }

    /** Мигание: два несинхронных «века», чтобы взгляд был живым. */
    fun blinkAt(t: Float): Float {
        val first = blinkPulse(t, 3.7f, 0.93f)
        val second = blinkPulse(t + 1.9f, 6.1f, 0.955f)
        return maxOf(first, second)
    }

    private fun blinkPulse(t: Float, period: Float, start: Float): Float {
        val phase = (t % period) / period
        if (phase < start) return 0f
        val local = (phase - start) / (1f - start)
        return sin(local * Math.PI.toFloat())
    }

    /** Живая стойка: дыхание, покачивание, взгляд по сторонам. */
    fun idlePose(t: Float, out: Pose) {
        val breathe = sin(t * 1.6f)
        val sway = sin(t * 0.63f)
        val shift = sin(t * 0.29f)
        val look = sin(t * 0.37f)

        out.set(Joint.ROOT, 0f, shift * 3f, sway * 1.2f)
        out.move(Joint.ROOT, 0f, breathe * 0.006f, 0f)
        out.set(Joint.SPINE, 2f + breathe * 1.5f, shift * 2f, 0f)
        out.set(Joint.CHEST, -1f - breathe * 1.2f, shift * 3f, 0f)
        out.set(Joint.NECK, 1f + breathe * 0.8f, -shift * 3f, 0f)
        out.set(Joint.HEAD, -1f + breathe * 1.5f, -shift * 5f + look * 3f, sin(t * 0.53f) * 2.5f)
        out.set(Joint.CREST, sin(t * 1.9f) * 4f, 0f, sin(t * 1.3f) * 5f)
        out.set(Joint.SHOULDER_L, 0f, 0f, -3f)
        out.set(Joint.SHOULDER_R, 0f, 0f, 3f)
        out.set(Joint.ARM_L, 3f + breathe * 1.5f, 0f, -7f)
        out.set(Joint.ARM_R, 3f + breathe * 1.5f, 0f, 7f)
        out.set(Joint.FOREARM_L, -13f, 0f, 0f)
        out.set(Joint.FOREARM_R, -13f, 0f, 0f)
        out.set(Joint.HAND_L, 0f, 0f, -4f)
        out.set(Joint.HAND_R, 0f, 0f, 4f)
        out.set(Joint.THIGH_L, 0f, 0f, -2f)
        out.set(Joint.THIGH_R, 0f, 0f, 2f)
        out.set(Joint.SHIN_L, 2f, 0f, 0f)
        out.set(Joint.SHIN_R, 2f, 0f, 0f)
    }

    private fun applyMood(t: Float, mood: AgentMood, pose: Pose, face: FaceState) {
        when (mood) {
            AgentMood.IDLE -> {
                face.mouthCurve = 0.42f
                face.brow = 0.15f
                face.pupilX = sin(t * 0.37f) * 0.45f
                face.pupilY = sin(t * 0.53f) * 0.3f
                face.mouthOpen = 0f
            }

            AgentMood.TALK -> {
                val g = sin(t * 3.1f)
                val g2 = sin(t * 3.1f + 1.7f)
                pose.add(Joint.CHEST, -2f, g * 3f, 0f)
                pose.add(Joint.HEAD, -3f + sin(t * 4f) * 2f, g * 5f, sin(t * 2.3f) * 3f)
                pose.set(Joint.ARM_L, -38f - g * 12f, 0f, -20f)
                pose.set(Joint.FOREARM_L, -58f + g * 16f, 0f, 0f)
                pose.set(Joint.ARM_R, -32f - g2 * 14f, 0f, 20f)
                pose.set(Joint.FOREARM_R, -62f + g2 * 18f, 0f, 0f)
                pose.set(Joint.HAND_L, 0f, 0f, -10f)
                pose.set(Joint.HAND_R, 0f, 0f, 10f)
                face.mouthOpen = 0.12f + 0.72f * (0.5f + 0.5f * sin(t * 13.7f)) *
                    (0.55f + 0.45f * sin(t * 5.1f))
                face.mouthCurve = 0.5f
                face.brow = 0.3f + 0.2f * sin(t * 2.7f)
                face.pupilX = g * 0.3f
                face.pupilY = sin(t * 1.7f) * 0.2f
            }

            AgentMood.LISTEN -> {
                pose.add(Joint.SPINE, -5f, 0f, 0f)
                pose.add(Joint.HEAD, 5f, -13f, 6f)
                pose.set(Joint.ARM_R, -118f, 0f, 30f)
                pose.set(Joint.FOREARM_R, -105f, 0f, -12f)
                pose.set(Joint.HAND_R, 0f, 0f, 12f)
                pose.set(Joint.ARM_L, 6f, 0f, -12f)
                face.eyeOpen = 1.15f
                face.brow = 0.65f
                face.mouthCurve = 0.35f
                face.mouthOpen = 0f
                face.pupilX = 0f
                face.pupilY = 0f
            }

            AgentMood.THINK -> {
                pose.set(Joint.ARM_R, -100f, 0f, 14f)
                pose.set(Joint.FOREARM_R, -125f, 0f, -14f)
                pose.set(Joint.HAND_R, 0f, 0f, 10f)
                pose.add(Joint.HEAD, 7f, 9f, -4f)
                pose.add(Joint.CHEST, 0f, 5f, 0f)
                face.pupilX = -0.35f
                face.pupilY = 0.5f
                face.brow = -0.15f
                face.mouthCurve = 0.15f
                face.mouthOpen = 0f
            }
        }
    }

    /**
     * Итоговая поза и мимика кадра: стойка + настроение + случайное смешное
     * движение, если чат давно бездействует.
     */
    fun poseAt(
        t: Float,
        mood: AgentMood,
        seed: Int,
        idleStart: Float,
        out: Pose,
        face: FaceState,
        forced: Animation? = null,
        forcedPhase: Float = 0f,
    ): Animation? {
        idlePose(t, out)
        face.blink = blinkAt(t)
        applyMood(t, mood, out, face)

        if (forced != null) {
            applyFunny(forced, forcedPhase, out, face)
            return forced
        }
        if (mood != AgentMood.IDLE) return null

        val funny = funnyAt(seed, idleStart, t) ?: return null
        applyFunny(funny.first, funny.second, out, face)
        return funny.first
    }

    /** Накладывает смешное движение на текущую позу с плавным входом и выходом. */
    private fun applyFunny(animation: Animation, phase: Float, pose: Pose, face: FaceState) {
        val animated = Pose()
        val animatedFace = FaceState()
        animated.copyFrom(pose)
        animatedFace.copyFrom(face)
        animation.pose(phase, animated)
        animation.face(phase, animatedFace)
        val blend = Num.easeEdges(phase, 0.2f)
        Pose.mix(pose, animated, blend, pose)
        face.mixFrom(face, animatedFace, blend)
    }
}
