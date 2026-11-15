package ru.lumen.launcher.data

/**
 * Маленькая матричная математика для 3D-персонажа. Матрица 4×4 хранится
 * построчно: m[строка * 4 + столбец]. Никаких зависимостей — всё считается
 * на телефоне, модель строится кодом.
 */
object M4 {

    fun identity(out: FloatArray) {
        java.util.Arrays.fill(out, 0f)
        out[0] = 1f
        out[5] = 1f
        out[10] = 1f
        out[15] = 1f
    }

    /** out = a × b (b применяется к точке первой). */
    fun multiply(a: FloatArray, b: FloatArray, out: FloatArray) {
        for (row in 0 until 4) {
            val r = row * 4
            for (col in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += a[r + k] * b[k * 4 + col]
                }
                out[r + col] = sum
            }
        }
    }

    fun translation(x: Float, y: Float, z: Float, out: FloatArray) {
        identity(out)
        out[3] = x
        out[7] = y
        out[11] = z
    }

    fun scale(x: Float, y: Float, z: Float, out: FloatArray) {
        identity(out)
        out[0] = x
        out[5] = y
        out[10] = z
    }

    fun rotationX(degrees: Float, out: FloatArray) {
        val a = Math.toRadians(degrees.toDouble())
        val c = Math.cos(a).toFloat()
        val s = Math.sin(a).toFloat()
        identity(out)
        out[5] = c
        out[6] = -s
        out[9] = s
        out[10] = c
    }

    fun rotationY(degrees: Float, out: FloatArray) {
        val a = Math.toRadians(degrees.toDouble())
        val c = Math.cos(a).toFloat()
        val s = Math.sin(a).toFloat()
        identity(out)
        out[0] = c
        out[2] = s
        out[8] = -s
        out[10] = c
    }

    fun rotationZ(degrees: Float, out: FloatArray) {
        val a = Math.toRadians(degrees.toDouble())
        val c = Math.cos(a).toFloat()
        val s = Math.sin(a).toFloat()
        identity(out)
        out[0] = c
        out[1] = -s
        out[4] = s
        out[5] = c
    }

    /** Поворот по трём осям: M = Ry × Rx × Rz. */
    fun rotation(rx: Float, ry: Float, rz: Float, out: FloatArray, tmpA: FloatArray, tmpB: FloatArray) {
        rotationX(rx, tmpA)
        rotationZ(rz, tmpB)
        multiply(tmpA, tmpB, out)
        rotationY(ry, tmpA)
        multiply(tmpA, out, tmpB)
        tmpB.copyInto(out)
    }

    fun transformPoint(m: FloatArray, x: Float, y: Float, z: Float, out: FloatArray) {
        val px = m[0] * x + m[1] * y + m[2] * z + m[3]
        val py = m[4] * x + m[5] * y + m[6] * z + m[7]
        val pz = m[8] * x + m[9] * y + m[10] * z + m[11]
        out[0] = px
        out[1] = py
        out[2] = pz
    }

    fun transformDirection(m: FloatArray, x: Float, y: Float, z: Float, out: FloatArray) {
        val px = m[0] * x + m[1] * y + m[2] * z
        val py = m[4] * x + m[5] * y + m[6] * z
        val pz = m[8] * x + m[9] * y + m[10] * z
        out[0] = px
        out[1] = py
        out[2] = pz
    }
}

/** Небольшие помощники для чисел, чтобы код анимаций читался. */
object Num {
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun clamp(value: Float, min: Float, max: Float): Float =
        if (value < min) min else if (value > max) max else value

    fun smooth(t: Float): Float {
        val x = clamp(t, 0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /** Плавное появление и исчезновение в начале и конце отрезка. */
    fun easeEdges(phase: Float, edge: Float = 0.12f): Float =
        smooth(clamp(phase / edge, 0f, 1f)) * smooth(clamp((1f - phase) / edge, 0f, 1f))

    fun mixColor(a: Int, b: Int, t: Float): Int {
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val r = lerp(ar.toFloat(), br.toFloat(), t).toInt().coerceIn(0, 255)
        val g = lerp(ag.toFloat(), bg.toFloat(), t).toInt().coerceIn(0, 255)
        val bl = lerp(ab.toFloat(), bb.toFloat(), t).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    fun shade(color: Int, factor: Float): Int {
        val r = (((color shr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = (((color shr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
