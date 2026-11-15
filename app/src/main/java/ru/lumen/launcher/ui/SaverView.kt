package ru.lumen.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.random.Random

/**
 * Системная заставка (DreamService) — тот же главный экран «Люмена»:
 * часы, дата и медленный процедурный фон. Рисуется обычным Canvas,
 * чтобы не тащить Compose в сервис заставки.
 */
class SaverView(context: Context) : View(context) {

    var onExit: (() -> Unit)? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    private val random = Random(17)
    private val particles = List(44) {
        Particle(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radius = 1.5f + random.nextFloat() * 5f,
            alpha = 0.12f + random.nextFloat() * 0.35f,
            speed = 0.05f + random.nextFloat() * 0.18f,
        )
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val ticker = object : Runnable {
        override fun run() {
            invalidate()
            postDelayed(this, 1000)
        }
    }

    init {
        setBackgroundColor(Color.parseColor("#061117"))
        isClickable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postDelayed(ticker, 0)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            performClick()
            onExit?.invoke()
            return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val now = LocalDateTime.now()
        val seconds = now.second.toFloat()

        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height,
            intArrayOf(Color.parseColor("#061117"), Color.parseColor("#0A1E26"), Color.parseColor("#04090D")),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)

        glowPaint.shader = RadialGradient(
            width * 0.22f,
            height * 0.24f,
            width * 0.85f,
            intArrayOf(Color.argb(70, 43, 213, 227), Color.argb(0, 43, 213, 227)),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(width * 0.22f, height * 0.24f, width * 0.85f, glowPaint)

        glowPaint.shader = RadialGradient(
            width * 0.82f,
            height * 0.86f,
            width * 0.7f,
            intArrayOf(Color.argb(52, 240, 90, 171), Color.argb(0, 240, 90, 171)),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(width * 0.82f, height * 0.86f, width * 0.7f, glowPaint)

        particles.forEach { particle ->
            val travel = (particle.y + seconds * particle.speed * 0.08f) % 1f
            glowPaint.shader = null
            glowPaint.color = Color.argb(
                (particle.alpha * 255).toInt().coerceIn(0, 255),
                152,
                232,
                240,
            )
            canvas.drawCircle(particle.x * width, travel * height, particle.radius, glowPaint)
        }

        val timeSize = min(width * 0.155f, height * 0.2f)
        val secondsSize = timeSize * 0.66f
        textPaint.textSize = timeSize

        val hours = String.format("%02d", now.hour)
        val minutes = String.format("%02d", now.minute)
        val secondsText = String.format("%02d", now.second)

        val hoursWidth = textPaint.measureText(hours)
        val minutesWidth = textPaint.measureText(minutes)
        textPaint.textSize = secondsSize
        val secondsWidth = textPaint.measureText(secondsText)
        val colonWidth = textPaint.measureText(":")

        val gap = timeSize * 0.12f
        val totalWidth = hoursWidth + minutesWidth + secondsWidth + colonWidth * 2 + gap * 4
        var cursor = (width - totalWidth) / 2f
        val baseline = height * 0.5f

        textPaint.textSize = timeSize
        textPaint.color = Color.parseColor("#EAF6FA")
        canvas.drawText(hours, cursor, baseline, textPaint)
        cursor += hoursWidth + gap

        textPaint.color = Color.parseColor("#5DE0EC")
        canvas.drawText(":", cursor, baseline, textPaint)
        cursor += colonWidth + gap

        textPaint.color = Color.parseColor("#EAF6FA")
        canvas.drawText(minutes, cursor, baseline, textPaint)
        cursor += minutesWidth + gap

        textPaint.color = Color.parseColor("#F05AAB")
        canvas.drawText(":", cursor, baseline, textPaint)
        cursor += colonWidth + gap

        textPaint.textSize = secondsSize
        textPaint.color = Color.parseColor("#9FC4CF")
        canvas.drawText(secondsText, cursor, baseline, textPaint)

        labelPaint.textSize = timeSize * 0.24f
        labelPaint.color = Color.parseColor("#8FAAB6")
        labelPaint.letterSpacing = 0.12f
        canvas.drawText(
            "ЛЮМЕН · ЗАСТАВКА",
            width / 2f,
            baseline - timeSize * 1.15f,
            labelPaint,
        )

        labelPaint.textSize = timeSize * 0.3f
        labelPaint.color = Color.parseColor("#CFE7ED")
        labelPaint.letterSpacing = 0.08f
        canvas.drawText(
            now.format(dateFormatter),
            width / 2f,
            baseline + timeSize * 0.7f,
            labelPaint,
        )

        labelPaint.textSize = timeSize * 0.14f
        labelPaint.color = Color.parseColor("#5E7C88")
        labelPaint.letterSpacing = 0.06f
        canvas.drawText(
            "коснитесь экрана, чтобы выйти",
            width / 2f,
            height * 0.9f,
            labelPaint,
        )
    }

    private data class Particle(
        val x: Float,
        val y: Float,
        val radius: Float,
        val alpha: Float,
        val speed: Float,
    )
}