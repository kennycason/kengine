package mario64

import com.kengine.render.RenderContext

class Mario64WorldRenderer {
    var lastDrawnTriangles: Int = 0
        private set

    private var render: RenderContext? = null
    private var yawCos = TRIG_SCALE
    private var yawSin = 0
    private var pitchCos = TRIG_SCALE
    private var pitchSin = 0
    private var cameraX = 0
    private var cameraY = 0
    private var cameraZ = 0
    private var projectionDistance = 200
    private var centerX = 160
    private var centerY = 120
    private var projectedX = 0
    private var projectedY = 0
    private var projectedZ = 0
    private var preparedWorld: Mario64BakedWorld? = null
    private var preparedCameraX = Int.MIN_VALUE
    private var preparedCameraY = Int.MIN_VALUE
    private var preparedCameraZ = Int.MIN_VALUE
    private var preparedYaw = Int.MIN_VALUE
    private var preparedPitch = Int.MIN_VALUE
    private val preparedScreenX = IntArray(VERTEX_CAPACITY)
    private val preparedScreenY = IntArray(VERTEX_CAPACITY)
    private val preparedScreenZ = IntArray(VERTEX_CAPACITY)
    private val preparedVisible = IntArray(VERTEX_CAPACITY)
    private val depthBucketHeads = IntArray(DEPTH_BUCKET_COUNT)
    private val depthBucketTails = IntArray(DEPTH_BUCKET_COUNT)
    private val depthBucketLinks = IntArray(TRIANGLE_CAPACITY)
    private val visibleTriangleIndexes = IntArray(TRIANGLE_CAPACITY)
    private val visibleTriangleDepths = IntArray(TRIANGLE_CAPACITY)
    private val visibleTriangleAreas = IntArray(TRIANGLE_CAPACITY)

    fun configure(
        render: RenderContext,
        cameraX: Int,
        cameraY: Int,
        cameraZ: Int,
        yaw: Int,
        pitch: Int,
        projectionDistance: Int,
        centerX: Int,
        centerY: Int
    ): Mario64WorldRenderer {
        this.render = render
        this.cameraX = cameraX
        this.cameraY = cameraY
        this.cameraZ = cameraZ
        this.yawCos = cosAngle(yaw)
        this.yawSin = sinAngle(yaw)
        this.pitchCos = cosAngle(pitch)
        this.pitchSin = sinAngle(pitch)
        this.projectionDistance = projectionDistance
        this.centerX = centerX
        this.centerY = centerY
        lastDrawnTriangles = 0
        return this
    }

    fun drawWorld(world: Mario64BakedWorld, triangleBudget: Int): Int {
        val output = render ?: return 0
        if (triangleBudget <= 0 || world.vertexCount > VERTEX_CAPACITY) return 0

        prepareVertices(world)

        var bucketIndex = 0
        while (bucketIndex < DEPTH_BUCKET_COUNT) {
            depthBucketHeads[bucketIndex] = -1
            depthBucketTails[bucketIndex] = -1
            bucketIndex += 1
        }

        var depthMin = Int.MAX_VALUE
        var depthMax = Int.MIN_VALUE
        var vertexIndex = 0
        while (vertexIndex < world.vertexCount) {
            if (preparedVisible[vertexIndex] != 0) {
                val z = preparedScreenZ[vertexIndex]
                if (z < depthMin) depthMin = z
                if (z > depthMax) depthMax = z
            }
            vertexIndex += 1
        }
        if (depthMin == Int.MAX_VALUE) {
            lastDrawnTriangles = 0
            return 0
        }

        val depthSpan = maxOf(1, depthMax - depthMin)
        var visibleCount = 0
        var triangleIndex = 0
        while (triangleIndex < world.triangleCount && visibleCount < TRIANGLE_CAPACITY) {
            val triangleBase = triangleIndex * Mario64BakedWorld.TRIANGLE_FIELD_COUNT
            val a = world.triangles[triangleBase]
            val b = world.triangles[triangleBase + 1]
            val c = world.triangles[triangleBase + 2]

            if (isVertexVisible(a) && isVertexVisible(b) && isVertexVisible(c)) {
                val ax = preparedScreenX[a]
                val ay = preparedScreenY[a]
                val bx = preparedScreenX[b]
                val by = preparedScreenY[b]
                val cx = preparedScreenX[c]
                val cy = preparedScreenY[c]
                val area = triangleArea(ax, ay, bx, by, cx, cy)
                if (absInt(area) > DEGENERATE_AREA &&
                    !isTriangleFarOutside(ax, ay, bx, by, cx, cy, output.width, output.height)
                ) {
                    val depth = (preparedScreenZ[a] + preparedScreenZ[b] + preparedScreenZ[c]) / 3
                    val clampedDepth = clampInt(depth, depthMin, depthMax) - depthMin
                    val bucket = scaleValue(clampedDepth, DEPTH_BUCKET_COUNT - 1, depthSpan)
                    visibleTriangleIndexes[visibleCount] = triangleIndex
                    visibleTriangleDepths[visibleCount] = depth
                    visibleTriangleAreas[visibleCount] = area
                    appendDepthBucket(bucket, visibleCount)
                    visibleCount += 1
                }
            }
            triangleIndex += 1
        }

        var drawn = 0
        bucketIndex = 0
        while (bucketIndex < DEPTH_BUCKET_COUNT && drawn < triangleBudget) {
            var visibleIndex = depthBucketHeads[bucketIndex]
            while (visibleIndex >= 0 && drawn < triangleBudget) {
                if (drawTriangle(
                        world,
                        visibleTriangleIndexes[visibleIndex],
                        visibleTriangleDepths[visibleIndex],
                        visibleTriangleAreas[visibleIndex]
                    )
                ) {
                    drawn += 1
                }
                visibleIndex = depthBucketLinks[visibleIndex]
            }
            bucketIndex += 1
        }

        lastDrawnTriangles = drawn
        return drawn
    }

    fun drawGroundGrid(halfSize: Int, step: Int, color: Int) {
        var v = -halfSize
        while (v <= halfSize) {
            drawLine3D(-halfSize, 0, v, halfSize, 0, v, color)
            drawLine3D(v, 0, -halfSize, v, 0, halfSize, color)
            v += step
        }
    }

    private fun prepareVertices(world: Mario64BakedWorld) {
        val yaw = (this.yawCos * 10000 + this.yawSin)
        val pitch = (this.pitchCos * 10000 + this.pitchSin)
        if (preparedWorld === world &&
            preparedCameraX == cameraX &&
            preparedCameraY == cameraY &&
            preparedCameraZ == cameraZ &&
            preparedYaw == yaw &&
            preparedPitch == pitch
        ) return

        var vertexIndex = 0
        while (vertexIndex < world.vertexCount) {
            val base = vertexIndex * 3
            val wx = world.vertices[base]
            val wy = world.vertices[base + 1]
            val wz = world.vertices[base + 2]
            if (projectToScreen(wx, wy, wz) && isProjectedReasonable(projectedX, projectedY)) {
                preparedScreenX[vertexIndex] = projectedX
                preparedScreenY[vertexIndex] = projectedY
                preparedScreenZ[vertexIndex] = projectedZ
                preparedVisible[vertexIndex] = 1
            } else {
                preparedVisible[vertexIndex] = 0
            }
            vertexIndex += 1
        }

        preparedWorld = world
        preparedCameraX = cameraX
        preparedCameraY = cameraY
        preparedCameraZ = cameraZ
        preparedYaw = yaw
        preparedPitch = pitch
    }

    private fun drawTriangle(
        world: Mario64BakedWorld,
        triangleIndex: Int,
        depth: Int,
        screenArea: Int
    ): Boolean {
        val output = render ?: return false
        val triangleBase = triangleIndex * Mario64BakedWorld.TRIANGLE_FIELD_COUNT
        val a = world.triangles[triangleBase]
        val b = world.triangles[triangleBase + 1]
        val c = world.triangles[triangleBase + 2]
        val colorIndex = world.triangles[triangleBase + 3]

        if (!isVertexVisible(a) || !isVertexVisible(b) || !isVertexVisible(c)) return false

        val ax = preparedScreenX[a]
        val ay = preparedScreenY[a]
        val bx = preparedScreenX[b]
        val by = preparedScreenY[b]
        val cx = preparedScreenX[c]
        val cy = preparedScreenY[c]

        if (absInt(triangleArea(ax, ay, bx, by, cx, cy)) <= DEGENERATE_AREA ||
            isTriangleFarOutside(ax, ay, bx, by, cx, cy, output.width, output.height)
        ) return false

        val baseColor = if (colorIndex in world.colors.indices) world.colors[colorIndex] else rgba(180, 180, 180)
        val light = triangleLight(a, b, c, screenArea, depth)
        val color = shadedColor(baseColor, light)
        output.drawTriangle(ax, ay, bx, by, cx, cy, color)
        return true
    }

    private fun projectToScreen(pointX: Int, pointY: Int, pointZ: Int): Boolean {
        val relX = pointX - cameraX
        val relY = pointY - cameraY
        val relZ = pointZ - cameraZ

        val viewX = trigMul(relX, yawCos) - trigMul(relZ, yawSin)
        val yawZ = trigMul(relX, yawSin) + trigMul(relZ, yawCos)
        val viewY = trigMul(relY, pitchCos) - trigMul(yawZ, pitchSin)
        val viewZ = trigMul(relY, pitchSin) + trigMul(yawZ, pitchCos)
        if (viewZ <= NEAR_PLANE) return false

        projectedX = centerX + scaleValue(viewX, projectionDistance, viewZ)
        projectedY = centerY - scaleValue(viewY, projectionDistance, viewZ)
        projectedZ = viewZ
        return true
    }

    private fun drawLine3D(
        startX: Int, startY: Int, startZ: Int,
        endX: Int, endY: Int, endZ: Int,
        color: Int
    ): Boolean {
        val output = render ?: return false
        if (!projectToScreen(startX, startY, startZ)) return false
        val ax = projectedX; val ay = projectedY
        if (!projectToScreen(endX, endY, endZ)) return false
        val bx = projectedX; val by = projectedY
        if (isFarOutside(ax, ay, output.width, output.height) &&
            isFarOutside(bx, by, output.width, output.height)
        ) return false
        output.drawLine(ax, ay, bx, by, color)
        return true
    }

    private fun appendDepthBucket(bucket: Int, visibleIndex: Int) {
        val b = clampInt(bucket, 0, DEPTH_BUCKET_COUNT - 1)
        depthBucketLinks[visibleIndex] = -1
        val tail = depthBucketTails[b]
        if (tail >= 0) {
            depthBucketLinks[tail] = visibleIndex
        } else {
            depthBucketHeads[b] = visibleIndex
        }
        depthBucketTails[b] = visibleIndex
    }

    private fun isVertexVisible(index: Int): Boolean {
        return index in 0 until VERTEX_CAPACITY && preparedVisible[index] != 0
    }

    private fun isProjectedReasonable(x: Int, y: Int): Boolean {
        return x > -COORD_LIMIT && x < COORD_LIMIT && y > -COORD_LIMIT && y < COORD_LIMIT
    }

    private fun isFarOutside(x: Int, y: Int, w: Int, h: Int): Boolean {
        return x < -OFFSCREEN || x > w + OFFSCREEN || y < -OFFSCREEN || y > h + OFFSCREEN
    }

    private fun isTriangleFarOutside(
        ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int, w: Int, h: Int
    ): Boolean {
        return (ax < -OFFSCREEN && bx < -OFFSCREEN && cx < -OFFSCREEN) ||
            (ax > w + OFFSCREEN && bx > w + OFFSCREEN && cx > w + OFFSCREEN) ||
            (ay < -OFFSCREEN && by < -OFFSCREEN && cy < -OFFSCREEN) ||
            (ay > h + OFFSCREEN && by > h + OFFSCREEN && cy > h + OFFSCREEN)
    }

    private fun triangleArea(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int): Int {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
    }

    private fun triangleLight(a: Int, b: Int, c: Int, screenArea: Int, depth: Int): Int {
        val avgY = (preparedScreenY[a] + preparedScreenY[b] + preparedScreenY[c]) / 3
        val heightLight = clampInt((centerY - avgY) / 3, -30, 40)
        val areaLight = clampInt(absInt(screenArea) / 200, 0, 40)
        val windingLight = if (screenArea >= 0) 12 else -12
        return 80 + heightLight + areaLight + windingLight
    }

    private fun shadedColor(color: Int, light: Int): Int {
        val shade = clampInt(light, 40, 255)
        return rgba(
            red = ((color and 0xff) * shade) / 255,
            green = (((color ushr 8) and 0xff) * shade) / 255,
            blue = (((color ushr 16) and 0xff) * shade) / 255,
            alpha = (color ushr 24) and 0xff
        )
    }

    companion object {
        const val WORLD_SCALE = 256
        const val TRIG_SCALE = 4096
        const val ANGLE_FULL = 1024
        const val ANGLE_RIGHT = ANGLE_FULL / 4

        private const val NEAR_PLANE = 20
        private const val OFFSCREEN = 128
        private const val COORD_LIMIT = 2048
        private const val VERTEX_CAPACITY = 3200
        private const val TRIANGLE_CAPACITY = 2200
        private const val DEPTH_BUCKET_COUNT = 64
        private const val DEGENERATE_AREA = 2
    }
}

internal fun sinAngle(angle: Int): Int {
    val wrapped = angle and (Mario64WorldRenderer.ANGLE_FULL - 1)
    return when {
        wrapped < Mario64WorldRenderer.ANGLE_RIGHT -> quarterSin(wrapped)
        wrapped < Mario64WorldRenderer.ANGLE_RIGHT * 2 -> quarterSin(Mario64WorldRenderer.ANGLE_RIGHT * 2 - wrapped)
        wrapped < Mario64WorldRenderer.ANGLE_RIGHT * 3 -> -quarterSin(wrapped - Mario64WorldRenderer.ANGLE_RIGHT * 2)
        else -> -quarterSin(Mario64WorldRenderer.ANGLE_FULL - wrapped)
    }
}

internal fun cosAngle(angle: Int): Int = sinAngle(angle + Mario64WorldRenderer.ANGLE_RIGHT)

internal fun trigMul(value: Int, trig: Int): Int {
    return (value * trig) / Mario64WorldRenderer.TRIG_SCALE
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
    val bounded = clampInt(value, 0, Mario64WorldRenderer.ANGLE_RIGHT)
    val numerator = bounded * (Mario64WorldRenderer.ANGLE_RIGHT * 2 - bounded)
    return scaleValue(numerator, Mario64WorldRenderer.TRIG_SCALE, Mario64WorldRenderer.ANGLE_RIGHT * Mario64WorldRenderer.ANGLE_RIGHT)
}

internal fun clampInt(value: Int, min: Int, max: Int): Int {
    return when {
        value < min -> min
        value > max -> max
        else -> value
    }
}

internal fun absInt(value: Int): Int = if (value < 0) -value else value

internal fun wrapAngle(angle: Int): Int = angle and (Mario64WorldRenderer.ANGLE_FULL - 1)

internal fun rgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
    return clampInt(red, 0, 255) or
        (clampInt(green, 0, 255) shl 8) or
        (clampInt(blue, 0, 255) shl 16) or
        (clampInt(alpha, 0, 255) shl 24)
}
