package ru.lumen.launcher.data

/** Экранные данные лица: рисуется поверх 3D-тела. */
class FaceFrame {
    var visible = false
    var alpha = 0f
    var scale = 0f
    var squash = 1f
    var roll = 0f
    var blink = 0f
    var eyeOpen = 1f
    var brow = 0f
    var mouthOpen = 0f
    var mouthCurve = 0.4f
    var mouthWidth = 1f
    var pupilX = 0f
    var pupilY = 0f
    var cheek = 0f
    var eyeCount = 0
    val eyeX = FloatArray(4)
    val eyeY = FloatArray(4)
    val eyeSquash = FloatArray(4) { 1f }
    var browCount = 0
    val browX = FloatArray(2)
    val browY = FloatArray(2)
    var mouthX = 0f
    var mouthY = 0f
    var cheekX = 0f
    var cheekY = 0f
}

/** Кадр: отсортированные треугольники в экранных координатах. */
class AgentFrame(val maxTriangles: Int) {
    val xy = FloatArray(maxTriangles * 6)
    val color = IntArray(maxTriangles)
    var triangles = 0
    val face = FaceFrame()
    var shadowVisible = false
    var shadowX = 0f
    var shadowY = 0f
    var shadowRx = 0f
    var shadowRy = 0f
    var shadowAlpha = 0f
}

/**
 * Программный рендер персонажа: перспектива, отсечение задних граней,
 * плоское освещение и сортировка по глубине. Всё считается на телефоне,
 * модель строится кодом — ни одного внешнего файла.
 */
class AgentRenderer(private val rig: AgentRig) {

    companion object {
        const val MAX_TRIANGLES = 1600

        /** Плоскость пола: на этом уровне стоят ступни. */
        private const val FLOOR_Y = -0.63f
        private const val VIEW_HEIGHT = 2.0f
        private const val VIEW_WIDTH = 1.25f
        private const val CAMERA_DISTANCE = 4.2f
        private const val CAMERA_TARGET_Y = 0.08f
        private const val CAMERA_PITCH = 6.5f
    }

    private val pose = Pose()
    private val face = FaceState()
    private val world = Array(JointCount) { FloatArray(16) }

    private var camX = 0f
    private var camY = 0f
    private var camZ = 0f
    private var focal = 1f
    private var centerX = 0f
    private var centerY = 0f

    private val world3 = FloatArray(9)
    private val view3 = FloatArray(9)
    private val screenX = FloatArray(3)
    private val screenY = FloatArray(3)
    private val depth = FloatArray(3)
    private val normal = FloatArray(3)
    private val edgeA = FloatArray(3)
    private val edgeB = FloatArray(3)

    private val frame = AgentFrame(MAX_TRIANGLES)
    private val order = IntArray(MAX_TRIANGLES)
    private val triDepth = FloatArray(MAX_TRIANGLES)
    private val sortedXy = FloatArray(MAX_TRIANGLES * 6)
    private val sortedColor = IntArray(MAX_TRIANGLES)

    fun frame(): AgentFrame = frame

    /** Собирает кадр для заданного времени и состояния персонажа. */
    fun build(
        width: Float,
        height: Float,
        time: Float,
        mood: AgentMood,
        seed: Int,
        idleStart: Float,
        forced: Animation? = null,
        forcedPhase: Float = 0f,
    ): Animation? {
        val animation = AgentAnimations.poseAt(
            time, mood, seed, idleStart, pose, face, forced, forcedPhase,
        )
        rig.solve(pose, world)

        val pitch = Math.toRadians(CAMERA_PITCH.toDouble()).toFloat()
        camX = 0f
        camY = CAMERA_TARGET_Y + Math.sin(pitch.toDouble()).toFloat() * CAMERA_DISTANCE
        camZ = Math.cos(pitch.toDouble()).toFloat() * CAMERA_DISTANCE
        centerX = width / 2f
        centerY = height * 0.52f
        focal = minOf(
            height * CAMERA_DISTANCE / VIEW_HEIGHT,
            width * CAMERA_DISTANCE / VIEW_WIDTH,
        )

        frame.triangles = 0
        var count = 0
        for (part in rig.parts) {
            val matrix = world[part.joint.ordinal]
            val vertices = part.vertices
            val triangles = part.colors.size
            for (index in 0 until triangles) {
                val base = index * 9
                for (vertex in 0 until 3) {
                    val offset = base + vertex * 3
                    val x = vertices[offset]
                    val y = vertices[offset + 1]
                    val z = vertices[offset + 2]
                    world3[vertex * 3] = matrix[0] * x + matrix[1] * y + matrix[2] * z + matrix[3]
                    world3[vertex * 3 + 1] = matrix[4] * x + matrix[5] * y + matrix[6] * z + matrix[7]
                    world3[vertex * 3 + 2] = matrix[8] * x + matrix[9] * y + matrix[10] * z + matrix[11]
                }

                // нормаль в мировых координатах и отсечение задних граней
                edgeA[0] = world3[3] - world3[0]
                edgeA[1] = world3[4] - world3[1]
                edgeA[2] = world3[5] - world3[2]
                edgeB[0] = world3[6] - world3[0]
                edgeB[1] = world3[7] - world3[1]
                edgeB[2] = world3[8] - world3[2]
                normal[0] = edgeA[1] * edgeB[2] - edgeA[2] * edgeB[1]
                normal[1] = edgeA[2] * edgeB[0] - edgeA[0] * edgeB[2]
                normal[2] = edgeA[0] * edgeB[1] - edgeA[1] * edgeB[0]
                val toCameraX = camX - world3[0]
                val toCameraY = camY - world3[1]
                val toCameraZ = camZ - world3[2]
                if (normal[0] * toCameraX + normal[1] * toCameraY + normal[2] * toCameraZ <= 0f) {
                    continue
                }

                var depthSum = 0f
                var clipped = false
                for (vertex in 0 until 3) {
                    val x = world3[vertex * 3] - camX
                    val y = world3[vertex * 3 + 1] - camY
                    val z = world3[vertex * 3 + 2] - camZ
                    // вид: поворот камеры вокруг X
                    val vy = Math.cos(pitch.toDouble()).toFloat() * y -
                        Math.sin(pitch.toDouble()).toFloat() * z
                    val vz = Math.sin(pitch.toDouble()).toFloat() * y +
                        Math.cos(pitch.toDouble()).toFloat() * z
                    val vertexDepth = -vz
                    if (vertexDepth < 0.25f) {
                        clipped = true
                        break
                    }
                    depth[vertex] = vertexDepth
                    depthSum += vertexDepth
                    screenX[vertex] = centerX + x * focal / vertexDepth
                    screenY[vertex] = centerY - vy * focal / vertexDepth
                }
                if (clipped) continue

                val length = Math.sqrt(
                    (normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]).toDouble()
                ).toFloat()
                var light = 0.42f
                if (length > 0.0001f) {
                    val nx = normal[0] / length
                    val ny = normal[1] / length
                    val nz = normal[2] / length
                    val dot = nx * LIGHT_X + ny * LIGHT_Y + nz * LIGHT_Z
                    light += 0.58f * if (dot > 0f) dot else 0f
                }

                val slot = count
                if (slot >= MAX_TRIANGLES) break
                val target = slot * 6
                frame.xy[target] = screenX[0]
                frame.xy[target + 1] = screenY[0]
                frame.xy[target + 2] = screenX[1]
                frame.xy[target + 3] = screenY[1]
                frame.xy[target + 4] = screenX[2]
                frame.xy[target + 5] = screenY[2]
                frame.color[slot] = Num.shade(part.colors[index], light)
                triDepth[slot] = depthSum / 3f
                order[slot] = slot
                count += 1
            }
        }

        // художник: дальние треугольники рисуются первыми
        sortByDepth(count)
        frame.triangles = count
        buildFace()
        buildShadow()
        return animation
    }

    private fun sortByDepth(count: Int) {
        for (i in 1 until count) {
            val key = order[i]
            val keyDepth = triDepth[key]
            var j = i - 1
            while (j >= 0 && triDepth[order[j]] < keyDepth) {
                order[j + 1] = order[j]
                j -= 1
            }
            order[j + 1] = key
        }
        // раскладываем треугольники в порядке отрисовки
        for (i in 0 until count) {
            val source = order[i] * 6
            val target = i * 6
            for (k in 0 until 6) sortedXy[target + k] = frame.xy[source + k]
            sortedColor[i] = frame.color[order[i]]
        }
        for (i in 0 until count * 6) frame.xy[i] = sortedXy[i]
        for (i in 0 until count) frame.color[i] = sortedColor[i]
    }

    private fun buildFace() {
        val head = world[Joint.HEAD.ordinal]
        val centerDepth = project(head, 0f, 0.02f, 0f)
        val centerX = screenX[0]
        val centerY = screenY[0]
        project(head, 0.21f, 0.02f, 0f)
        val radiusX = distance(screenX[0], screenY[0], centerX, centerY)
        val roll = Math.atan2(
            (screenY[0] - centerY).toDouble(),
            (screenX[0] - centerX).toDouble(),
        ).toFloat()
        project(head, 0f, 0.23f, 0f)
        val radiusY = distance(screenX[0], screenY[0], centerX, centerY)
        // лицо видно, только если голова смотрит примерно на камеру
        val forwardX = head[2]
        val forwardY = head[6]
        val forwardZ = head[10]
        val pitchRad = Math.toRadians(CAMERA_PITCH.toDouble()).toFloat()
        val forwardViewZ = Math.sin(pitchRad.toDouble()).toFloat() * forwardY +
            Math.cos(pitchRad.toDouble()).toFloat() * forwardZ
        val output = frame.face
        output.alpha = Num.clamp((forwardViewZ - 0.48f) / 0.4f, 0f, 1f)
        output.visible = output.alpha > 0.25f && radiusY > 6f && centerDepth > 0f
        output.scale = radiusY
        output.squash = if (radiusY > 0.001f) Num.clamp(radiusX / radiusY, 0.12f, 1.2f) else 0.2f
        output.roll = roll
        output.blink = face.blink
        output.eyeOpen = face.eyeOpen
        output.brow = face.brow
        output.mouthOpen = face.mouthOpen
        output.mouthCurve = face.mouthCurve
        output.mouthWidth = face.mouthWidth
        output.pupilX = face.pupilX
        output.pupilY = face.pupilY
        output.cheek = face.cheek

        val anchors = rig.anchors
        output.eyeCount = anchors.eyes.size
        for (index in anchors.eyes.indices) {
            val anchor = anchors.eyes[index]
            project(head, anchor[0], anchor[1], anchor[2])
            output.eyeX[index] = screenX[0]
            output.eyeY[index] = screenY[0]
            output.eyeSquash[index] = Num.clamp(depth[0] / centerDepth, 0.6f, 1.05f)
        }
        output.browCount = anchors.brows.size
        for (index in anchors.brows.indices) {
            val anchor = anchors.brows[index]
            project(head, anchor[0], anchor[1], anchor[2])
            output.browX[index] = screenX[0]
            output.browY[index] = screenY[0]
        }
        project(head, anchors.mouth[0], anchors.mouth[1], anchors.mouth[2])
        output.mouthX = screenX[0]
        output.mouthY = screenY[0]
        val cheek = anchors.cheeks.first()
        project(head, cheek[0], cheek[1], cheek[2])
        output.cheekX = screenX[0]
        output.cheekY = screenY[0]
    }

    private fun buildShadow() {
        val rootY = world[Joint.ROOT.ordinal][7]
        project(IDENTITY, 0f, FLOOR_Y, 0f)
        val shadowX = screenX[0]
        val shadowY = screenY[0]
        project(IDENTITY, 0f, 0.86f, 0f)
        val topY = screenY[0]
        val bodyHeight = kotlin.math.abs(topY - shadowY)
        val output = frame
        output.shadowVisible = bodyHeight > 8f
        output.shadowX = shadowX
        output.shadowY = shadowY
        output.shadowRx = bodyHeight * 0.30f
        output.shadowRy = bodyHeight * 0.085f
        val lift = Num.clamp((rootY - 0.01f) / 0.30f, 0f, 1f)
        output.shadowAlpha = 0.30f * (1f - lift * 0.75f)
    }

    /** Проецирует точку из системы кости на экран; результат в screenX/screenY/depth. */
    private fun project(matrix: FloatArray, x: Float, y: Float, z: Float): Float {
        val wx = matrix[0] * x + matrix[1] * y + matrix[2] * z + matrix[3]
        val wy = matrix[4] * x + matrix[5] * y + matrix[6] * z + matrix[7]
        val wz = matrix[8] * x + matrix[9] * y + matrix[10] * z + matrix[11]
        val dx = wx - camX
        val dy = wy - camY
        val dz = wz - camZ
        val pitch = Math.toRadians(CAMERA_PITCH.toDouble()).toFloat()
        val vy = Math.cos(pitch.toDouble()).toFloat() * dy - Math.sin(pitch.toDouble()).toFloat() * dz
        val vz = Math.sin(pitch.toDouble()).toFloat() * dy + Math.cos(pitch.toDouble()).toFloat() * dz
        val vertexDepth = -vz
        val safeDepth = if (vertexDepth < 0.25f) 0.25f else vertexDepth
        screenX[0] = centerX + dx * focal / safeDepth
        screenY[0] = centerY - vy * focal / safeDepth
        depth[0] = vertexDepth
        return vertexDepth
    }

    private fun distance(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x0 - x1
        val dy = y0 - y1
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private val LIGHT_X = -0.34f
    private val LIGHT_Y = 0.78f
    private val LIGHT_Z = 0.52f

    private val IDENTITY = FloatArray(16).also { M4.identity(it) }
}
