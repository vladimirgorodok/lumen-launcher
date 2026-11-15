package ru.lumen.launcher

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.lumen.launcher.data.AgentAnimations
import ru.lumen.launcher.data.AgentMood
import ru.lumen.launcher.data.AgentRenderer
import ru.lumen.launcher.data.AgentRig
import ru.lumen.launcher.data.Animation
import ru.lumen.launcher.data.Character
import ru.lumen.launcher.data.Characters
import ru.lumen.launcher.data.FaceFrame
import java.io.File
import kotlin.math.abs

/**
 * Тест рисует персонажа в PNG, чтобы геометрию можно было проверить глазами,
 * а заодно следит за базовыми инвариантами кадра.
 */
class AgentPreviewTest {

    private val width = 240
    private val height = 320

    private fun render(
        character: Character,
        time: Float,
        mood: AgentMood,
        forced: Animation? = null,
        phase: Float = 0f,
    ): Raster {
        val rig = AgentRig(character)
        val renderer = AgentRenderer(rig)
        renderer.build(width.toFloat(), height.toFloat(), time, mood, 7, 0f, forced, phase)
        val frame = renderer.frame()

        assertTrue("треугольников слишком мало: ${frame.triangles}", frame.triangles > 120)
        for (i in 0 until frame.triangles * 6) {
            assertTrue("координата не число", frame.xy[i].isFinite())
        }

        val raster = Raster(width, height)
        raster.clear(0xFF121620.toInt())

        if (frame.shadowVisible) {
            val alpha = (frame.shadowAlpha * 255).toInt().coerceIn(0, 255)
            raster.ellipse(
                frame.shadowX, frame.shadowY, frame.shadowRx, frame.shadowRy, 0f,
                (alpha shl 24) or 0x000000,
            )
        }

        for (i in 0 until frame.triangles) {
            val o = i * 6
            raster.triangle(
                frame.xy[o], frame.xy[o + 1],
                frame.xy[o + 2], frame.xy[o + 3],
                frame.xy[o + 4], frame.xy[o + 5],
                frame.color[i],
            )
        }

        drawFace(raster, frame.face)
        return raster
    }

    /** Копия отрисовки лица из AgentView — чтобы проверять мимику. */
    private fun drawFace(raster: Raster, face: FaceFrame) {
        if (!face.visible) return
        val eyeR = face.scale * 0.26f
        val dark = 0xF0181C28.toInt()

        for (index in 0 until face.eyeCount) {
            val cx = face.eyeX[index]
            val cy = face.eyeY[index]
            val rx = eyeR * face.squash
            val ry = eyeR * face.eyeOpen * (1f - face.blink)
            if (face.blink > 0.82f || ry < 0.6f) {
                raster.line(
                    cx - rx, cy, cx + rx, cy, eyeR * 0.22f, dark,
                )
                continue
            }
            raster.ellipse(cx, cy, rx, ry, face.roll, 0xFFF4FAFF.toInt())
            val pupilR = eyeR * 0.44f
            val px = cx + face.pupilX * eyeR * 0.34f
            val py = cy + face.pupilY * eyeR * 0.30f
            raster.ellipse(px, py, pupilR, pupilR, 0f, dark)
            raster.ellipse(
                px - pupilR * 0.55f, py - pupilR * 0.55f,
                pupilR * 0.42f, pupilR * 0.42f, 0f, 0xDCFFFFFF.toInt(),
            )
        }

        for (index in 0 until face.browCount) {
            val cx = face.browX[index]
            val cy = face.browY[index]
            val half = eyeR * 0.85f
            val lift = face.brow * eyeR * 0.55f
            val dir = if (index == 0) -1f else 1f
            raster.line(
                cx - half, cy - lift * dir,
                cx + half, cy + lift * dir,
                eyeR * 0.26f, 0xD0181C28.toInt(),
            )
        }

        val mx = face.mouthX
        val my = face.mouthY
        val half = face.scale * 0.30f * face.mouthWidth * face.squash
        val open = face.mouthOpen
        if (open < 0.12f) {
            val curve = face.mouthCurve * half * 0.75f
            raster.curve(
                mx - half, my - curve * 0.25f,
                mx, my + curve,
                mx + half, my - curve * 0.25f,
                face.scale * 0.055f, 0xE0181C28.toInt(),
            )
        } else {
            val mouthH = face.scale * (0.05f + 0.20f * open)
            raster.ellipse(mx, my, half, mouthH * 0.5f, face.roll, 0xFF2A101C.toInt())
            if (open > 0.35f) {
                raster.ellipse(
                    mx, my + mouthH * 0.28f, half * 0.6f, mouthH * 0.24f, face.roll,
                    0xFFFF5F91.toInt(),
                )
            }
        }

        if (face.cheek > 0.05f) {
            val alpha = (face.cheek * 150).toInt().coerceIn(0, 255)
            val cr = face.scale * 0.20f
            for (side in listOf(-1f, 1f)) {
                raster.ellipse(
                    face.cheekX * side, face.cheekY, cr, cr * 0.7f, 0f,
                    (alpha shl 24) or 0xFF7A9E,
                )
            }
        }
    }

    @Test
    fun previewContactSheet() {
        val dir = File("/tmp/opencode/agent")
        dir.mkdirs()

        val items = mutableListOf<Pair<String, Raster>>()
        val lumen = Characters.first()
        items += "стойка" to render(lumen, 0.4f, AgentMood.IDLE)
        items += "говорит" to render(lumen, 2.1f, AgentMood.TALK)
        items += "слушает" to render(lumen, 2.1f, AgentMood.LISTEN)
        items += "думает" to render(lumen, 2.1f, AgentMood.THINK)
        for (animation in AgentAnimations.all) {
            items += animation.title to render(lumen, 2.1f, AgentMood.IDLE, animation, 0.5f)
        }
        for (character in Characters) {
            items += character.name to render(character, 1.3f, AgentMood.IDLE)
        }

        println("ПОРЯДОК КАДРОВ: " + items.joinToString(" | ") { it.first })

        val columns = 6
        val rows = (items.size + columns - 1) / columns
        val sheet = Raster(columns * width, rows * height, 1)
        sheet.clear(0xFF0A0D14.toInt())
        items.forEachIndexed { index, (_, image) ->
            val column = index % columns
            val row = index / columns
            sheet.blit(image, column * width, row * height)
        }
        sheet.save(File(dir, "sheet.png"))

        for (character in Characters) {
            render(character, 1.7f, AgentMood.IDLE)
                .save(File(dir, "char-${character.id}.png"))
        }

        val motion = Raster(width * 4, height, 1)
        motion.clear(0xFF0A0D14.toInt())
        listOf(0.0f, 0.8f, 1.6f, 2.4f).forEachIndexed { index, t ->
            motion.blit(render(lumen, t, AgentMood.IDLE), index * width, 0)
        }
        motion.save(File(dir, "motion.png"))
    }

    @Test
    fun trianglesStayInFrame() {
        val rig = AgentRig(Characters.first())
        val renderer = AgentRenderer(rig)
        var previous = FloatArray(0)
        var moved = false
        for (step in 0 until 40) {
            val time = step * 0.12f
            renderer.build(width.toFloat(), height.toFloat(), time, AgentMood.IDLE, 3, 0f)
            val frame = renderer.frame()
            val current = FloatArray(6) { frame.xy[it] }
            if (previous.isNotEmpty() && abs(current[0] - previous[0]) > 0.01f) moved = true
            previous = current
            for (i in 0 until frame.triangles * 6) {
                val value = frame.xy[i]
                assertTrue("персонаж улетел за кадр", value > -6000f && value < 6000f)
            }
        }
        assertTrue("персонаж не двигается", moved)
    }

    @Test
    fun funnyScheduleHappens() {
        var found = 0
        var time = 0f
        while (time < 240f) {
            if (AgentAnimations.funnyAt(5, 0f, time) != null) found += 1
            time += 0.25f
        }
        assertTrue("смешные движения не появляются", found > 40)
    }

    @Test
    fun blinkAndTalkAnimate() {
        val closed = (0 until 400).map { AgentAnimations.blinkAt(it * 0.02f) }.max()
        assertTrue("персонаж не моргает", closed > 0.9f)
        var moving = false
        var previous = 0f
        for (step in 0 until 200) {
            val value = kotlin.math.sin(step * 0.31f)
            if (abs(value - previous) > 0.001f) moving = true
            previous = value
        }
        assertTrue(moving)
    }
}
