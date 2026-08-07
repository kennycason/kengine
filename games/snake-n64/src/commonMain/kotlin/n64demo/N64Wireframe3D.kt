package n64demo

import com.kengine.render.RenderContext

class N64Wireframe3D {
    private var render: RenderContext? = null
    private var targetX = 0
    private var targetY = 0
    private var targetZ = 0
    private var yawCos = TRIG_SCALE
    private var yawSin = 0
    private var pitchCos = TRIG_SCALE
    private var pitchSin = 0
    private var cameraDistance = WORLD_SCALE
    private var projectionDistance = 122
    private var centerX = 160
    private var centerY = 120
    private var projectedX = 0
    private var projectedY = 0
    private var worldX = 0
    private var worldY = 0
    private var worldZ = 0

    fun configure(
        render: RenderContext,
        targetX: Int,
        targetY: Int,
        targetZ: Int,
        yaw: Int,
        pitch: Int,
        cameraDistance: Int,
        projectionDistance: Int,
        centerX: Int,
        centerY: Int
    ): N64Wireframe3D {
        this.render = render
        this.targetX = targetX
        this.targetY = targetY
        this.targetZ = targetZ
        this.yawCos = cosAngle(yaw)
        this.yawSin = sinAngle(yaw)
        this.pitchCos = cosAngle(pitch)
        this.pitchSin = sinAngle(pitch)
        this.cameraDistance = cameraDistance
        this.projectionDistance = projectionDistance
        this.centerX = centerX
        this.centerY = centerY
        return this
    }

    fun drawGroundGrid(halfSize: Int, step: Int, color: Int, axisColor: Int) {
        var value = -halfSize
        while (value <= halfSize) {
            val lineColor = if (value == 0) axisColor else color
            drawLine3D(-halfSize, 0, value, halfSize, 0, value, lineColor)
            drawLine3D(value, 0, -halfSize, value, 0, halfSize, lineColor)
            value += step
        }
    }

    fun drawArenaBounds(halfSize: Int, color: Int) {
        drawLine3D(-halfSize, 0, -halfSize, halfSize, 0, -halfSize, color)
        drawLine3D(halfSize, 0, -halfSize, halfSize, 0, halfSize, color)
        drawLine3D(halfSize, 0, halfSize, -halfSize, 0, halfSize, color)
        drawLine3D(-halfSize, 0, halfSize, -halfSize, 0, -halfSize, color)
    }

    fun drawShape(
        type: Int,
        centerX: Int,
        centerY: Int,
        centerZ: Int,
        size: Int,
        rotationX: Int,
        rotationY: Int,
        rotationZ: Int,
        color: Int
    ) {
        when (type) {
            ShapeType.PYRAMID -> drawShape(PYRAMID_VERTICES, PYRAMID_EDGES, centerX, centerY, centerZ, size, rotationX, rotationY, rotationZ, color)
            ShapeType.DIAMOND -> drawShape(DIAMOND_VERTICES, DIAMOND_EDGES, centerX, centerY, centerZ, size, rotationX, rotationY, rotationZ, color)
            else -> drawShape(CUBE_VERTICES, CUBE_EDGES, centerX, centerY, centerZ, size, rotationX, rotationY, rotationZ, color)
        }
    }

    fun drawLine3D(
        startX: Int,
        startY: Int,
        startZ: Int,
        endX: Int,
        endY: Int,
        endZ: Int,
        color: Int
    ) {
        val output = render ?: return
        if (!projectToScreen(startX, startY, startZ)) return
        val ax = projectedX
        val ay = projectedY
        if (!projectToScreen(endX, endY, endZ)) return
        val bx = projectedX
        val by = projectedY

        if (isFarOutside(ax, ay, output.width, output.height) &&
            isFarOutside(bx, by, output.width, output.height)
        ) {
            return
        }

        output.drawLine(ax, ay, bx, by, color)
    }

    private fun drawShape(
        vertices: IntArray,
        edges: IntArray,
        centerX: Int,
        centerY: Int,
        centerZ: Int,
        size: Int,
        rotationX: Int,
        rotationY: Int,
        rotationZ: Int,
        color: Int
    ) {
        val cosX = cosAngle(rotationX)
        val sinX = sinAngle(rotationX)
        val cosY = cosAngle(rotationY)
        val sinY = sinAngle(rotationY)
        val cosZ = cosAngle(rotationZ)
        val sinZ = sinAngle(rotationZ)

        var index = 0
        while (index < edges.size) {
            transformToWorld(vertices, edges[index], size, centerX, centerY, centerZ, cosX, sinX, cosY, sinY, cosZ, sinZ)
            val startX = worldX
            val startY = worldY
            val startZ = worldZ
            transformToWorld(vertices, edges[index + 1], size, centerX, centerY, centerZ, cosX, sinX, cosY, sinY, cosZ, sinZ)
            drawLine3D(startX, startY, startZ, worldX, worldY, worldZ, color)
            index += 2
        }
    }

    private fun transformToWorld(
        vertices: IntArray,
        vertexIndex: Int,
        size: Int,
        centerX: Int,
        centerY: Int,
        centerZ: Int,
        cosX: Int,
        sinX: Int,
        cosY: Int,
        sinY: Int,
        cosZ: Int,
        sinZ: Int
    ) {
        val base = vertexIndex * 3
        var x = scaleValue(vertices[base], size, SIZE_SCALE)
        val y = scaleValue(vertices[base + 1], size, SIZE_SCALE)
        var z = scaleValue(vertices[base + 2], size, SIZE_SCALE)

        val yAfterX = trigMul(y, cosX) - trigMul(z, sinX)
        z = trigMul(y, sinX) + trigMul(z, cosX)

        val xAfterY = trigMul(x, cosY) + trigMul(z, sinY)
        val zAfterY = -trigMul(x, sinY) + trigMul(z, cosY)
        x = xAfterY

        worldX = centerX + trigMul(x, cosZ) - trigMul(yAfterX, sinZ)
        worldY = centerY + trigMul(x, sinZ) + trigMul(yAfterX, cosZ)
        worldZ = centerZ + zAfterY
    }

    private fun projectToScreen(pointX: Int, pointY: Int, pointZ: Int): Boolean {
        val relX = pointX - targetX
        val relY = pointY - targetY
        val relZ = pointZ - targetZ

        val viewX = trigMul(relX, yawCos) - trigMul(relZ, yawSin)
        val yawZ = trigMul(relX, yawSin) + trigMul(relZ, yawCos)
        val viewY = trigMul(relY, pitchCos) - trigMul(yawZ, pitchSin)
        val viewZ = trigMul(relY, pitchSin) + trigMul(yawZ, pitchCos) + cameraDistance
        if (viewZ <= NEAR_PLANE) return false

        projectedX = centerX + scaleValue(viewX, projectionDistance, viewZ)
        projectedY = centerY - scaleValue(viewY, projectionDistance, viewZ)
        return true
    }

    private fun isFarOutside(x: Int, y: Int, width: Int, height: Int): Boolean {
        return x < -OFFSCREEN_MARGIN ||
            x > width + OFFSCREEN_MARGIN ||
            y < -OFFSCREEN_MARGIN ||
            y > height + OFFSCREEN_MARGIN
    }

    companion object {
        const val WORLD_SCALE = 256
        const val SIZE_SCALE = 256
        const val TRIG_SCALE = 4096
        const val ANGLE_FULL = 1024
        const val ANGLE_RIGHT = ANGLE_FULL / 4

        private const val NEAR_PLANE = WORLD_SCALE / 5
        private const val OFFSCREEN_MARGIN = 96

        private val CUBE_VERTICES = intArrayOf(
            -128, -128, -128,
            128, -128, -128,
            128, 128, -128,
            -128, 128, -128,
            -128, -128, 128,
            128, -128, 128,
            128, 128, 128,
            -128, 128, 128
        )

        private val CUBE_EDGES = intArrayOf(
            0, 1, 1, 2, 2, 3, 3, 0,
            4, 5, 5, 6, 6, 7, 7, 4,
            0, 4, 1, 5, 2, 6, 3, 7
        )

        private val PYRAMID_VERTICES = intArrayOf(
            -141, -115, -141,
            141, -115, -141,
            141, -115, 141,
            -141, -115, 141,
            0, 166, 0
        )

        private val PYRAMID_EDGES = intArrayOf(
            0, 1, 1, 2, 2, 3, 3, 0,
            0, 4, 1, 4, 2, 4, 3, 4
        )

        private val DIAMOND_VERTICES = intArrayOf(
            0, 192, 0,
            166, 0, 0,
            0, 0, 166,
            -166, 0, 0,
            0, 0, -166,
            0, -192, 0
        )

        private val DIAMOND_EDGES = intArrayOf(
            0, 1, 0, 2, 0, 3, 0, 4,
            5, 1, 5, 2, 5, 3, 5, 4,
            1, 2, 2, 3, 3, 4, 4, 1
        )
    }
}

object ShapeType {
    const val CUBE = 0
    const val PYRAMID = 1
    const val DIAMOND = 2
}

internal fun sinAngle(angle: Int): Int {
    val wrapped = angle and (N64Wireframe3D.ANGLE_FULL - 1)
    return when {
        wrapped < N64Wireframe3D.ANGLE_RIGHT -> quarterSin(wrapped)
        wrapped < N64Wireframe3D.ANGLE_RIGHT * 2 -> quarterSin(N64Wireframe3D.ANGLE_RIGHT * 2 - wrapped)
        wrapped < N64Wireframe3D.ANGLE_RIGHT * 3 -> -quarterSin(wrapped - N64Wireframe3D.ANGLE_RIGHT * 2)
        else -> -quarterSin(N64Wireframe3D.ANGLE_FULL - wrapped)
    }
}

internal fun cosAngle(angle: Int): Int = sinAngle(angle + N64Wireframe3D.ANGLE_RIGHT)

internal fun trigMul(value: Int, trig: Int): Int {
    return (value * trig) / N64Wireframe3D.TRIG_SCALE
}

internal fun scaleValue(value: Int, numerator: Int, denominator: Int): Int {
    if (denominator == 0) return 0
    val scaled = value * numerator
    return if (scaled >= 0) {
        (scaled + denominator / 2) / denominator
    } else {
        (scaled - denominator / 2) / denominator
    }
}

private fun quarterSin(value: Int): Int {
    val bounded = clampInt(value, 0, N64Wireframe3D.ANGLE_RIGHT)
    val numerator = bounded * (N64Wireframe3D.ANGLE_RIGHT * 2 - bounded)
    return scaleValue(numerator, N64Wireframe3D.TRIG_SCALE, N64Wireframe3D.ANGLE_RIGHT * N64Wireframe3D.ANGLE_RIGHT)
}

internal fun clampInt(value: Int, min: Int, max: Int): Int {
    return when {
        value < min -> min
        value > max -> max
        else -> value
    }
}
