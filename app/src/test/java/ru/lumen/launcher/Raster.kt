package ru.lumen.launcher

import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Крошечный растеризатор для тестов: рисует треугольники, эллипсы и линии
 * с 2× супервыборкой и сохраняет PNG. Нужен только чтобы глазами проверять
 * геометрию персонажа — в приложении всё рисует Compose.
 */
class Raster(val width: Int, val height: Int, private val scale: Int = 2) {

    private val w = width * scale
    private val h = height * scale
    private val pixels = IntArray(w * h)

    fun clear(color: Int) {
        java.util.Arrays.fill(pixels, color)
    }

    private fun blend(index: Int, color: Int) {
        val a = (color ushr 24) and 0xFF
        if (a == 0) return
        if (a == 255) {
            pixels[index] = color
            return
        }
        val dst = pixels[index]
        val alpha = a / 255f
        val inv = 1f - alpha
        val r = (((color shr 16) and 0xFF) * alpha + ((dst shr 16) and 0xFF) * inv).toInt()
        val g = (((color shr 8) and 0xFF) * alpha + ((dst shr 8) and 0xFF) * inv).toInt()
        val b = ((color and 0xFF) * alpha + (dst and 0xFF) * inv).toInt()
        pixels[index] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun triangle(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        cx: Float, cy: Float,
        color: Int,
    ) {
        val x0 = ax * scale
        val y0 = ay * scale
        val x1 = bx * scale
        val y1 = by * scale
        val x2 = cx * scale
        val y2 = cy * scale
        val area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if (abs(area) < 0.0001f) return
        val minX = max(0, floor(min(x0, min(x1, x2))).toInt())
        val maxX = min(w - 1, ceil(max(x0, max(x1, x2))).toInt())
        val minY = max(0, floor(min(y0, min(y1, y2))).toInt())
        val maxY = min(h - 1, ceil(max(y0, max(y1, y2))).toInt())
        if (minX > maxX || minY > maxY) return
        for (y in minY..maxY) {
            val py = y + 0.5f
            val row = y * w
            for (x in minX..maxX) {
                val px = x + 0.5f
                val w0 = ((x1 - x0) * (py - y0) - (px - x0) * (y1 - y0)) / area
                val w1 = ((px - x0) * (y2 - y0) - (x2 - x0) * (py - y0)) / area
                if (w0 < 0f || w1 < 0f || w0 + w1 > 1f) continue
                blend(row + x, color)
            }
        }
    }

    fun ellipse(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        rotation: Float,
        color: Int,
        segments: Int = 28,
    ) {
        val cosR = kotlin.math.cos(rotation.toDouble()).toFloat()
        val sinR = kotlin.math.sin(rotation.toDouble()).toFloat()
        val cxs = cx * scale
        val cys = cy * scale
        val rxs = rx * scale
        val rys = ry * scale
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 6.2831855f
            val a1 = ((i + 1).toFloat() / segments) * 6.2831855f
            val x0 = kotlin.math.cos(a0.toDouble()).toFloat() * rxs
            val y0 = kotlin.math.sin(a0.toDouble()).toFloat() * rys
            val x1 = kotlin.math.cos(a1.toDouble()).toFloat() * rxs
            val y1 = kotlin.math.sin(a1.toDouble()).toFloat() * rys
            triangle(
                cxs / scale, cys / scale,
                (cxs + x0 * cosR - y0 * sinR) / scale, (cys + x0 * sinR + y0 * cosR) / scale,
                (cxs + x1 * cosR - y1 * sinR) / scale, (cys + x1 * sinR + y1 * cosR) / scale,
                color,
            )
        }
    }

    fun line(x0: Float, y0: Float, x1: Float, y1: Float, thickness: Float, color: Int) {
        val dx = x1 - x0
        val dy = y1 - y0
        val length = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (length < 0.001f) return
        val nx = -dy / length * thickness / 2f
        val ny = dx / length * thickness / 2f
        quad(x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, color)
    }

    fun quad(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        cx: Float, cy: Float,
        dx: Float, dy: Float,
        color: Int,
    ) {
        triangle(ax, ay, bx, by, cx, cy, color)
        triangle(ax, ay, cx, cy, dx, dy, color)
    }

    fun curve(
        x0: Float, y0: Float,
        cx: Float, cy: Float,
        x1: Float, y1: Float,
        thickness: Float,
        color: Int,
        steps: Int = 14,
    ) {
        var px = x0
        var py = y0
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val inv = 1f - t
            val x = inv * inv * x0 + 2f * inv * t * cx + t * t * x1
            val y = inv * inv * y0 + 2f * inv * t * cy + t * t * y1
            line(px, py, x, y, thickness, color)
            px = x
            py = y
        }
    }

    /** Уменьшает буфер до размеров картинки (супервыборка как сглаживание). */
    fun toArgb(): IntArray {
        val out = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                for (sy in 0 until scale) {
                    val row = (y * scale + sy) * w
                    for (sx in 0 until scale) {
                        val pixel = pixels[row + x * scale + sx]
                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                    }
                }
                val count = scale * scale
                out[y * width + x] = (0xFF shl 24) or
                    ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }
        return out
    }

    /** Копирует картинку в этот буфер (он должен быть без супервыборки). */
    fun blit(source: Raster, dx: Int, dy: Int) {
        val argb = source.toArgb()
        for (y in 0 until source.height) {
            val py = dy + y
            if (py < 0 || py >= height) continue
            for (x in 0 until source.width) {
                val px = dx + x
                if (px < 0 || px >= width) continue
                pixels[py * w + px * scale] = argb[y * source.width + x]
            }
        }
    }

    fun save(file: File) {
        writePng(file, toArgb(), width, height)
    }

    private fun writePng(file: File, argb: IntArray, width: Int, height: Int) {
        val raw = ByteArray(height * (width * 4 + 1))
        var index = 0
        for (y in 0 until height) {
            raw[index++] = 0
            for (x in 0 until width) {
                val pixel = argb[y * width + x]
                raw[index++] = ((pixel shr 16) and 0xFF).toByte()
                raw[index++] = ((pixel shr 8) and 0xFF).toByte()
                raw[index++] = (pixel and 0xFF).toByte()
                raw[index++] = ((pixel ushr 24) and 0xFF).toByte()
            }
        }
        val compressed = ByteArray(raw.size + 64)
        val deflater = Deflater(Deflater.BEST_SPEED)
        deflater.setInput(raw)
        deflater.finish()
        val size = deflater.deflate(compressed)
        deflater.end()

        val output = FileOutputStream(file)
        output.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        val header = ByteArray(13)
        writeInt(header, 0, width)
        writeInt(header, 4, height)
        header[8] = 8
        header[9] = 6
        output.write(chunk("IHDR", header))
        output.write(chunk("IDAT", compressed.copyOf(size)))
        output.write(chunk("IEND", ByteArray(0)))
        output.close()
    }

    private fun chunk(type: String, data: ByteArray): ByteArray {
        val result = ByteArray(data.size + 12)
        writeInt(result, 0, data.size)
        val name = type.toByteArray(Charsets.US_ASCII)
        name.copyInto(result, 4)
        data.copyInto(result, 8)
        val crc = CRC32()
        crc.update(result, 4, data.size + 4)
        writeInt(result, data.size + 8, crc.value.toInt())
        return result
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }
}
