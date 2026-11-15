package ru.lumen.launcher.data

import androidx.compose.ui.graphics.toArgb


/** Кости персонажа: скелет, который анимируется. */
enum class Joint {
    ROOT, SPINE, CHEST, NECK, HEAD, CREST,
    SHOULDER_L, ARM_L, FOREARM_L, HAND_L,
    SHOULDER_R, ARM_R, FOREARM_R, HAND_R,
    HIP_L, THIGH_L, SHIN_L, FOOT_L,
    HIP_R, THIGH_R, SHIN_R, FOOT_R,
}

val JointCount = Joint.entries.size

/**
 * Поза: повороты, смещения и масштабы каждой кости. Массивы плоские,
 * чтобы позы дешево смешивать между собой.
 */
class Pose {
    val rot = FloatArray(JointCount * 3)
    val offset = FloatArray(JointCount * 3)
    val scale = FloatArray(JointCount * 3) { 1f }

    fun set(joint: Joint, rx: Float = 0f, ry: Float = 0f, rz: Float = 0f) {
        val i = joint.ordinal * 3
        rot[i] = rx
        rot[i + 1] = ry
        rot[i + 2] = rz
    }

    fun add(joint: Joint, rx: Float = 0f, ry: Float = 0f, rz: Float = 0f) {
        val i = joint.ordinal * 3
        rot[i] += rx
        rot[i + 1] += ry
        rot[i + 2] += rz
    }

    fun move(joint: Joint, x: Float = 0f, y: Float = 0f, z: Float = 0f) {
        val i = joint.ordinal * 3
        offset[i] = x
        offset[i + 1] = y
        offset[i + 2] = z
    }

    fun resize(joint: Joint, x: Float = 1f, y: Float = 1f, z: Float = 1f) {
        val i = joint.ordinal * 3
        scale[i] = x
        scale[i + 1] = y
        scale[i + 2] = z
    }

    fun copyFrom(other: Pose) {
        other.rot.copyInto(rot)
        other.offset.copyInto(offset)
        other.scale.copyInto(scale)
    }

    companion object {
        fun mix(a: Pose, b: Pose, t: Float, out: Pose) {
            for (i in a.rot.indices) {
                out.rot[i] = Num.lerp(a.rot[i], b.rot[i], t)
                out.offset[i] = Num.lerp(a.offset[i], b.offset[i], t)
                out.scale[i] = Num.lerp(a.scale[i], b.scale[i], t)
            }
        }
    }
}

/** Треугольная часть тела, привязанная к кости. */
class MeshPart(val joint: Joint, val vertices: FloatArray, val colors: IntArray) {
    val triangles: Int get() = colors.size
}

/** Якоря лица в системе координат головы (для 2D-мимики поверх 3D-тела). */
class FaceAnchors(
    val eyes: List<FloatArray>,
    val brows: List<FloatArray>,
    val mouth: FloatArray,
    val cheeks: List<FloatArray>,
)

/** Скелет + меш персонажа. Строится кодом, без внешних моделей. */
class AgentRig(val character: Character) {

    private val bodyColor = character.body.toArgb()
    private val accentColor = character.accent.toArgb()
    private val glowColor = character.glow.toArgb()
    private val darkColor = Num.shade(bodyColor, 0.72f)
    private val deeperColor = Num.shade(bodyColor, 0.55f)
    private val lightColor = Num.shade(bodyColor, 1.18f)

    val parts: List<MeshPart> = buildParts()
    val anchors: FaceAnchors = buildFaceAnchors()
    val eyeCount: Int = character.eyeCount

    // ---------- скелет ----------

    private fun parentOf(joint: Joint): Joint? = when (joint) {
        Joint.ROOT -> null
        Joint.SPINE -> Joint.ROOT
        Joint.CHEST -> Joint.SPINE
        Joint.NECK -> Joint.CHEST
        Joint.HEAD -> Joint.NECK
        Joint.CREST -> Joint.HEAD
        Joint.SHOULDER_L, Joint.SHOULDER_R -> Joint.CHEST
        Joint.ARM_L, Joint.ARM_R -> if (joint == Joint.ARM_L) Joint.SHOULDER_L else Joint.SHOULDER_R
        Joint.FOREARM_L, Joint.FOREARM_R ->
            if (joint == Joint.FOREARM_L) Joint.ARM_L else Joint.ARM_R
        Joint.HAND_L, Joint.HAND_R ->
            if (joint == Joint.HAND_L) Joint.FOREARM_L else Joint.FOREARM_R
        Joint.HIP_L, Joint.HIP_R -> Joint.ROOT
        Joint.THIGH_L, Joint.THIGH_R -> if (joint == Joint.THIGH_L) Joint.HIP_L else Joint.HIP_R
        Joint.SHIN_L, Joint.SHIN_R -> if (joint == Joint.SHIN_L) Joint.THIGH_L else Joint.THIGH_R
        Joint.FOOT_L, Joint.FOOT_R -> if (joint == Joint.FOOT_L) Joint.SHIN_L else Joint.SHIN_R
    }

    private fun offsetOf(joint: Joint): FloatArray = when (joint) {
        Joint.ROOT -> floatArrayOf(0f, 0f, 0f)
        Joint.SPINE -> floatArrayOf(0f, 0.16f, 0f)
        Joint.CHEST -> floatArrayOf(0f, 0.14f, 0f)
        Joint.NECK -> floatArrayOf(0f, 0.14f, 0f)
        Joint.HEAD -> floatArrayOf(0f, 0.10f, 0f)
        Joint.CREST -> floatArrayOf(0f, 0.19f, 0f)
        Joint.SHOULDER_L -> floatArrayOf(-0.155f, 0.09f, 0f)
        Joint.SHOULDER_R -> floatArrayOf(0.155f, 0.09f, 0f)
        Joint.ARM_L -> floatArrayOf(-0.035f, -0.02f, 0f)
        Joint.ARM_R -> floatArrayOf(0.035f, -0.02f, 0f)
        Joint.FOREARM_L, Joint.FOREARM_R -> floatArrayOf(0f, -0.20f, 0f)
        Joint.HAND_L, Joint.HAND_R -> floatArrayOf(0f, -0.19f, 0f)
        Joint.HIP_L -> floatArrayOf(-0.085f, -0.06f, 0f)
        Joint.HIP_R -> floatArrayOf(0.085f, -0.06f, 0f)
        Joint.THIGH_L, Joint.THIGH_R -> floatArrayOf(0f, 0f, 0f)
        Joint.SHIN_L, Joint.SHIN_R -> floatArrayOf(0f, -0.26f, 0f)
        Joint.FOOT_L, Joint.FOOT_R -> floatArrayOf(0f, -0.26f, 0f)
    }

    /** Прямая кинематика: мировые матрицы костей для заданной позы. */
    fun solve(pose: Pose, out: Array<FloatArray>) {
        val rotTmpA = FloatArray(16)
        val rotTmpB = FloatArray(16)
        val local = FloatArray(16)
        val scaleM = FloatArray(16)
        val transM = FloatArray(16)
        val tmp = FloatArray(16)

        for (joint in Joint.entries) {
            val index = joint.ordinal
            val parent = parentOf(joint)
            val offset = offsetOf(joint)
            val o = index * 3

            M4.translation(
                offset[0] + pose.offset[o],
                offset[1] + pose.offset[o + 1],
                offset[2] + pose.offset[o + 2],
                transM,
            )
            M4.rotation(
                pose.rot[o],
                pose.rot[o + 1],
                pose.rot[o + 2],
                rotTmpA,
                tmp,
                rotTmpB,
            )
            M4.scale(pose.scale[o], pose.scale[o + 1], pose.scale[o + 2], scaleM)
            M4.multiply(transM, rotTmpA, local)
            M4.multiply(local, scaleM, tmp)
            tmp.copyInto(local)

            val world = out[index]
            if (parent == null) {
                local.copyInto(world)
            } else {
                M4.multiply(out[parent.ordinal], local, world)
            }
        }
    }

    // ---------- меш ----------

    private fun buildParts(): List<MeshPart> = buildList {
        // таз
        add(
            part(Joint.ROOT) {
                prism(8, 0.115f, 0.10f, 0.06f, -0.09f, darkColor)
                sphere(0.10f, 6, 3, center = floatArrayOf(0f, -0.03f, 0f), color = deeperColor)
            }
        )
        // грудь и живот
        add(
            part(Joint.CHEST) {
                prism(8, 0.16f, 0.15f, 0.14f, -0.02f, bodyColor)
                prism(8, 0.15f, 0.125f, -0.02f, -0.25f, Num.shade(bodyColor, 0.92f))
                // светящийся значок на груди
                disc(0.035f, 10, bodyColor, center = floatArrayOf(0f, 0.05f, 0.152f), glow = true)
            }
        )
        // шея
        add(
            part(Joint.NECK) {
                prism(6, 0.052f, 0.058f, 0.10f, -0.03f, Num.shade(bodyColor, 0.85f))
            }
        )
        // голова
        add(
            part(Joint.HEAD) {
                sphere(0.21f, 9, 5, center = floatArrayOf(0f, 0.02f, 0f), color = bodyColor)
                // подбородок чуть светлее
                sphere(0.075f, 6, 3, center = floatArrayOf(0f, -0.13f, 0.10f), color = lightColor)
            }
        )
        // гребень или антенны
        add(
            part(Joint.CREST) {
                if (character.antennae) {
                    for (side in listOf(-1f, 1f)) {
                        tube(
                            side * 0.05f, -0.01f, 0f,
                            side * 0.115f, 0.14f, 0.012f,
                            0.014f, 0.008f, 5, accentColor,
                        )
                        sphere(
                            0.032f, 6, 3,
                            center = floatArrayOf(side * 0.12f, 0.165f, 0.014f),
                            color = glowColor,
                        )
                    }
                } else {
                    prism(3, 0.055f, 0.02f, 0.085f, -0.03f, accentColor)
                    sphere(0.03f, 6, 3, center = floatArrayOf(0f, 0.105f, 0f), color = glowColor)
                    tube(
                        -0.05f, -0.01f, 0f, -0.105f, 0.10f, 0f,
                        0.011f, 0.007f, 5, accentColor,
                    )
                    tube(
                        0.05f, -0.01f, 0f, 0.105f, 0.10f, 0f,
                        0.011f, 0.007f, 5, accentColor,
                    )
                }
            }
        )
        // руки
        for (side in listOf(-1f, 1f)) {
            val left = side < 0
            add(
                part(if (left) Joint.ARM_L else Joint.ARM_R) {
                    sphere(0.062f, 6, 3, center = floatArrayOf(0f, 0.01f, 0f), color = Num.shade(bodyColor, 0.88f))
                    prism(6, 0.055f, 0.046f, 0f, -0.20f, bodyColor)
                }
            )
            add(
                part(if (left) Joint.FOREARM_L else Joint.FOREARM_R) {
                    sphere(0.05f, 6, 3, center = floatArrayOf(0f, 0.005f, 0f), color = Num.shade(bodyColor, 0.9f))
                    prism(6, 0.046f, 0.04f, 0f, -0.19f, bodyColor)
                }
            )
            add(
                part(if (left) Joint.HAND_L else Joint.HAND_R) {
                    sphere(0.055f, 6, 3, center = floatArrayOf(0f, -0.035f, 0f), color = accentColor)
                }
            )
        }
        // ноги
        for (side in listOf(-1f, 1f)) {
            val left = side < 0
            add(
                part(if (left) Joint.THIGH_L else Joint.THIGH_R) {
                    sphere(0.08f, 6, 3, center = floatArrayOf(0f, -0.01f, 0f), color = Num.shade(bodyColor, 0.86f))
                    prism(6, 0.075f, 0.06f, 0f, -0.26f, bodyColor)
                }
            )
            add(
                part(if (left) Joint.SHIN_L else Joint.SHIN_R) {
                    sphere(0.062f, 6, 3, center = floatArrayOf(0f, 0.005f, 0f), color = Num.shade(bodyColor, 0.9f))
                    prism(6, 0.06f, 0.048f, 0f, -0.26f, bodyColor)
                }
            )
            add(
                part(if (left) Joint.FOOT_L else Joint.FOOT_R) {
                    box(0.10f, 0.06f, 0.17f, accentColor, center = floatArrayOf(0f, -0.01f, 0.035f))
                }
            )
        }
    }

    private fun part(joint: Joint, build: MeshBuilder.() -> Unit): MeshPart {
        val builder = MeshBuilder()
        builder.build()
        return MeshPart(joint, builder.vertices.toFloatArray(), builder.colors.toIntArray())
    }

    // ---------- лицо ----------

    private fun buildFaceAnchors(): FaceAnchors {
        val eyes = mutableListOf<FloatArray>()
        val brows = mutableListOf<FloatArray>()
        when (character.eyeCount) {
            1 -> {
                eyes += floatArrayOf(0f, 0.03f, 0.195f)
                brows += floatArrayOf(0f, 0.105f, 0.185f)
            }
            2 -> {
                eyes += floatArrayOf(-0.078f, 0.03f, 0.192f)
                eyes += floatArrayOf(0.078f, 0.03f, 0.192f)
                brows += floatArrayOf(-0.078f, 0.105f, 0.183f)
                brows += floatArrayOf(0.078f, 0.105f, 0.183f)
            }
            3 -> {
                eyes += floatArrayOf(-0.082f, 0.005f, 0.192f)
                eyes += floatArrayOf(0.082f, 0.005f, 0.192f)
                eyes += floatArrayOf(0f, 0.125f, 0.165f)
                brows += floatArrayOf(-0.082f, 0.075f, 0.183f)
                brows += floatArrayOf(0.082f, 0.075f, 0.183f)
            }
            else -> {
                eyes += floatArrayOf(-0.088f, 0.045f, 0.19f)
                eyes += floatArrayOf(0.088f, 0.045f, 0.19f)
                eyes += floatArrayOf(-0.058f, -0.055f, 0.19f)
                eyes += floatArrayOf(0.058f, -0.055f, 0.19f)
                brows += floatArrayOf(-0.088f, 0.11f, 0.181f)
                brows += floatArrayOf(0.088f, 0.11f, 0.181f)
            }
        }
        return FaceAnchors(
            eyes = eyes,
            brows = brows,
            mouth = floatArrayOf(0f, -0.075f, 0.196f),
            cheeks = listOf(
                floatArrayOf(-0.13f, -0.045f, 0.165f),
                floatArrayOf(0.13f, -0.045f, 0.165f),
            ),
        )
    }
}

/** Сборщик треугольников в локальных координатах кости. */
class MeshBuilder {
    val vertices = ArrayList<Float>(512)
    val colors = ArrayList<Int>(64)

    private fun tri(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        color: Int,
    ) {
        vertices.add(ax); vertices.add(ay); vertices.add(az)
        vertices.add(bx); vertices.add(by); vertices.add(bz)
        vertices.add(cx); vertices.add(cy); vertices.add(cz)
        colors.add(color)
    }

    private fun quad(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        dx: Float, dy: Float, dz: Float,
        color: Int,
    ) {
        tri(ax, ay, az, bx, by, bz, cx, cy, cz, color)
        tri(ax, ay, az, cx, cy, cz, dx, dy, dz, color)
    }

    /** Усечённый цилиндр: сверху радиус rTop на высоте yTop, снизу rBottom на yBottom. */
    fun prism(
        sides: Int,
        rTop: Float,
        rBottom: Float,
        yTop: Float,
        yBottom: Float,
        color: Int,
    ) {
        for (i in 0 until sides) {
            val a0 = (i.toFloat() / sides) * (Math.PI * 2).toFloat()
            val a1 = ((i + 1).toFloat() / sides) * (Math.PI * 2).toFloat()
            val x0 = Math.cos(a0.toDouble()).toFloat()
            val z0 = Math.sin(a0.toDouble()).toFloat()
            val x1 = Math.cos(a1.toDouble()).toFloat()
            val z1 = Math.sin(a1.toDouble()).toFloat()
            quad(
                x0 * rTop, yTop, z0 * rTop,
                x1 * rTop, yTop, z1 * rTop,
                x1 * rBottom, yBottom, z1 * rBottom,
                x0 * rBottom, yBottom, z0 * rBottom,
                color,
            )
            tri(0f, yTop, 0f, x1 * rTop, yTop, z1 * rTop, x0 * rTop, yTop, z0 * rTop, color)
            tri(0f, yBottom, 0f, x0 * rBottom, yBottom, z0 * rBottom, x1 * rBottom, yBottom, z1 * rBottom, color)
        }
    }

    fun sphere(
        radius: Float,
        segments: Int,
        rings: Int,
        center: FloatArray = floatArrayOf(0f, 0f, 0f),
        color: Int,
    ) {
        for (ring in 0 until rings) {
            val p0 = (ring.toFloat() / rings) * Math.PI.toFloat()
            val p1 = ((ring + 1).toFloat() / rings) * Math.PI.toFloat()
            val y0 = Math.cos(p0.toDouble()).toFloat() * radius
            val y1 = Math.cos(p1.toDouble()).toFloat() * radius
            val r0 = Math.sin(p0.toDouble()).toFloat() * radius
            val r1 = Math.sin(p1.toDouble()).toFloat() * radius
            for (segment in 0 until segments) {
                val t0 = (segment.toFloat() / segments) * (Math.PI * 2).toFloat()
                val t1 = ((segment + 1).toFloat() / segments) * (Math.PI * 2).toFloat()
                val c0 = Math.cos(t0.toDouble()).toFloat()
                val s0 = Math.sin(t0.toDouble()).toFloat()
                val c1 = Math.cos(t1.toDouble()).toFloat()
                val s1 = Math.sin(t1.toDouble()).toFloat()
                quad(
                    center[0] + c0 * r0, center[1] + y0, center[2] + s0 * r0,
                    center[0] + c1 * r0, center[1] + y0, center[2] + s1 * r0,
                    center[0] + c1 * r1, center[1] + y1, center[2] + s1 * r1,
                    center[0] + c0 * r1, center[1] + y1, center[2] + s0 * r1,
                    color,
                )
            }
        }
    }

    /** Трубка между двумя точками: годится для антенн и наклонных деталей. */
    fun tube(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        radiusA: Float,
        radiusB: Float,
        sides: Int,
        color: Int,
    ) {
        val dx = bx - ax
        val dy = by - ay
        val dz = bz - az
        val length = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
        if (length < 0.0001f) return
        val nx = dx / length
        val ny = dy / length
        val nz = dz / length
        // произвольный перпендикуляр к оси
        var ux = -ny
        var uy = nx
        var uz = 0f
        if (Math.abs(ux) + Math.abs(uy) + Math.abs(uz) < 0.001f) {
            ux = 1f
            uy = 0f
            uz = 0f
        }
        val ul = Math.sqrt((ux * ux + uy * uy + uz * uz).toDouble()).toFloat()
        ux /= ul; uy /= ul; uz /= ul
        val vx = ny * uz - nz * uy
        val vy = nz * ux - nx * uz
        val vz = nx * uy - ny * ux

        for (i in 0 until sides) {
            val a0 = (i.toFloat() / sides) * (Math.PI * 2).toFloat()
            val a1 = ((i + 1).toFloat() / sides) * (Math.PI * 2).toFloat()
            val c0 = Math.cos(a0.toDouble()).toFloat()
            val s0 = Math.sin(a0.toDouble()).toFloat()
            val c1 = Math.cos(a1.toDouble()).toFloat()
            val s1 = Math.sin(a1.toDouble()).toFloat()
            val p0x = ax + (ux * c0 + vx * s0) * radiusA
            val p0y = ay + (uy * c0 + vy * s0) * radiusA
            val p0z = az + (uz * c0 + vz * s0) * radiusA
            val p1x = ax + (ux * c1 + vx * s1) * radiusA
            val p1y = ay + (uy * c1 + vy * s1) * radiusA
            val p1z = az + (uz * c1 + vz * s1) * radiusA
            val q0x = bx + (ux * c0 + vx * s0) * radiusB
            val q0y = by + (uy * c0 + vy * s0) * radiusB
            val q0z = bz + (uz * c0 + vz * s0) * radiusB
            val q1x = bx + (ux * c1 + vx * s1) * radiusB
            val q1y = by + (uy * c1 + vy * s1) * radiusB
            val q1z = bz + (uz * c1 + vz * s1) * radiusB
            outwardQuad(p0x, p0y, p0z, p1x, p1y, p1z, q1x, q1y, q1z, q0x, q0y, q0z, ax, ay, az, bx, by, bz, color)
            outwardTri(ax, ay, az, p1x, p1y, p1z, p0x, p0y, p0z, ax, ay, az, bx, by, bz, color)
            outwardTri(bx, by, bz, q0x, q0y, q0z, q1x, q1y, q1z, ax, ay, az, bx, by, bz, color)
        }
    }

    /** Кладёт четырёхугольник так, чтобы нормаль смотрела наружу от оси. */
    private fun outwardQuad(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        dx: Float, dy: Float, dz: Float,
        lx0: Float, ly0: Float, lz0: Float,
        lx1: Float, ly1: Float, lz1: Float,
        color: Int,
    ) {
        if (facesOutward(ax, ay, az, bx, by, bz, cx, cy, cz, lx0, ly0, lz0, lx1, ly1, lz1)) {
            quad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, color)
        } else {
            quad(dx, dy, dz, cx, cy, cz, bx, by, bz, ax, ay, az, color)
        }
    }

    private fun outwardTri(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        lx0: Float, ly0: Float, lz0: Float,
        lx1: Float, ly1: Float, lz1: Float,
        color: Int,
    ) {
        if (facesOutward(ax, ay, az, bx, by, bz, cx, cy, cz, lx0, ly0, lz0, lx1, ly1, lz1)) {
            tri(ax, ay, az, bx, by, bz, cx, cy, cz, color)
        } else {
            tri(cx, cy, cz, bx, by, bz, ax, ay, az, color)
        }
    }

    /** Нормаль треугольника должна смотреть в сторону от оси трубки. */
    private fun facesOutward(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        lx0: Float, ly0: Float, lz0: Float,
        lx1: Float, ly1: Float, lz1: Float,
    ): Boolean {
        val abx = bx - ax
        val aby = by - ay
        val abz = bz - az
        val acx = cx - ax
        val acy = cy - ay
        val acz = cz - az
        val nx = aby * acz - abz * acy
        val ny = abz * acx - abx * acz
        val nz = abx * acy - aby * acx
        val mx = (ax + bx + cx) / 3f
        val my = (ay + by + cy) / 3f
        val mz = (az + bz + cz) / 3f
        val dx = lx1 - lx0
        val dy = ly1 - ly0
        val dz = lz1 - lz0
        val length = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
        if (length < 0.0001f) return true
        val t = ((mx - lx0) * dx + (my - ly0) * dy + (mz - lz0) * dz) / (length * length)
        val px = lx0 + dx * t
        val py = ly0 + dy * t
        val pz = lz0 + dz * t
        return nx * (mx - px) + ny * (my - py) + nz * (mz - pz) > 0f
    }

    fun disc(
        radius: Float,
        segments: Int,
        color: Int,
        center: FloatArray = floatArrayOf(0f, 0f, 0f),
        glow: Boolean = false,
    ) {
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (Math.PI * 2).toFloat()
            val a1 = ((i + 1).toFloat() / segments) * (Math.PI * 2).toFloat()
            val shade = color
            tri(
                center[0], center[1], center[2],
                center[0] + Math.cos(a0.toDouble()).toFloat() * radius,
                center[1] + Math.sin(a0.toDouble()).toFloat() * radius,
                center[2],
                center[0] + Math.cos(a1.toDouble()).toFloat() * radius,
                center[1] + Math.sin(a1.toDouble()).toFloat() * radius,
                center[2],
                shade,
            )
        }
    }

    fun box(
        width: Float,
        height: Float,
        depth: Float,
        color: Int,
        center: FloatArray = floatArrayOf(0f, 0f, 0f),
    ) {
        val x0 = center[0] - width / 2f
        val x1 = center[0] + width / 2f
        val y0 = center[1] - height / 2f
        val y1 = center[1] + height / 2f
        val z0 = center[2] - depth / 2f
        val z1 = center[2] + depth / 2f
        quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, color)
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color)
        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, color)
        quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, color)
        quad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, color)
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, color)
    }
}
