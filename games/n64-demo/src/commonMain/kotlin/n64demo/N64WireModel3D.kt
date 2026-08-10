package n64demo

import com.kengine.render.RenderContext

class N64BakedWireModel3D(
    val name: String,
    val source: String,
    val license: String,
    val vertices: IntArray,
    val edges: IntArray,
    val triangles: IntArray,
    val colors: IntArray
) {
    val vertexCount: Int
        get() = vertices.size / 3

    val edgeCount: Int
        get() = edges.size / EDGE_FIELD_COUNT

    val triangleCount: Int
        get() = triangles.size / TRIANGLE_FIELD_COUNT

    companion object {
        const val EDGE_FIELD_COUNT = 5
        const val TRIANGLE_FIELD_COUNT = 4
    }
}

class N64WireModelRenderer3D {
    var lastDrawnEdges: Int = 0
        private set

    var lastDrawnTriangles: Int = 0
        private set

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
    private var projectedZ = 0
    private var worldX = 0
    private var worldY = 0
    private var worldZ = 0
    private var preparedModel: N64BakedWireModel3D? = null
    private var preparedCenterX = 0
    private var preparedCenterY = 0
    private var preparedCenterZ = 0
    private var preparedSize = 0
    private var preparedRotationX = 0
    private var preparedRotationY = 0
    private var preparedRotationZ = 0
    private var preparedDepthMin = 0
    private var preparedDepthMax = 0
    private var drawnTriangleStamp = 1
    private var selectedTriangleStamp = 1
    private var drawnEdgeStamp = 1
    private var drawnTriangleCountForEdges = 0
    private val preparedScreenX = IntArray(MODEL_VERTEX_CAPACITY)
    private val preparedScreenY = IntArray(MODEL_VERTEX_CAPACITY)
    private val preparedScreenZ = IntArray(MODEL_VERTEX_CAPACITY)
    private val preparedVisible = IntArray(MODEL_VERTEX_CAPACITY)
    private val drawnTriangleMarkers = IntArray(TRIANGLE_SORT_CAPACITY)
    private val selectedTriangleMarkers = IntArray(TRIANGLE_SORT_CAPACITY)
    private val drawnEdgeMarkers = IntArray(MODEL_EDGE_CAPACITY)
    private val visibleTriangleIndexes = IntArray(TRIANGLE_SORT_CAPACITY)
    private val visibleTriangleDepths = IntArray(TRIANGLE_SORT_CAPACITY)
    private val visibleTriangleAreas = IntArray(TRIANGLE_SORT_CAPACITY)
    private val selectedTriangleIndexes = IntArray(TRIANGLE_SORT_CAPACITY)
    private val selectedTriangleDepths = IntArray(TRIANGLE_SORT_CAPACITY)
    private val selectedTriangleAreas = IntArray(TRIANGLE_SORT_CAPACITY)

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
    ): N64WireModelRenderer3D {
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
        lastDrawnEdges = 0
        lastDrawnTriangles = 0
        drawnTriangleCountForEdges = 0
        preparedModel = null
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

    fun drawModelTriangles(
        model: N64BakedWireModel3D,
        centerX: Int,
        centerY: Int,
        centerZ: Int,
        size: Int,
        rotationX: Int,
        rotationY: Int,
        rotationZ: Int,
        triangleBudget: Int,
        overlayEdgeBudget: Int = 0
    ): Int {
        val output = render ?: return 0
        if (triangleBudget <= 0) {
            lastDrawnTriangles = 0
            if (overlayEdgeBudget > 0) lastDrawnEdges = 0
            drawnTriangleCountForEdges = 0
            return 0
        }

        if (!prepareModel(model, centerX, centerY, centerZ, size, rotationX, rotationY, rotationZ)) {
            lastDrawnTriangles = 0
            if (overlayEdgeBudget > 0) lastDrawnEdges = 0
            drawnTriangleCountForEdges = 0
            return 0
        }
        beginTriangleDrawTracking(overlayEdgeBudget > 0)

        var visibleCount = 0
        var triangleIndex = 0
        while (triangleIndex < model.triangleCount && visibleCount < TRIANGLE_SORT_CAPACITY) {
            val triangleBase = triangleIndex * N64BakedWireModel3D.TRIANGLE_FIELD_COUNT
            val a = model.triangles[triangleBase]
            val b = model.triangles[triangleBase + 1]
            val c = model.triangles[triangleBase + 2]

            if (isPreparedVertexVisible(a) && isPreparedVertexVisible(b) && isPreparedVertexVisible(c)) {
                val ax = preparedScreenX[a]
                val ay = preparedScreenY[a]
                val bx = preparedScreenX[b]
                val by = preparedScreenY[b]
                val cx = preparedScreenX[c]
                val cy = preparedScreenY[c]
                val area = triangleArea(ax, ay, bx, by, cx, cy)
                if (absInt(area) > DEGENERATE_TRIANGLE_AREA &&
                    !isTriangleFarOutside(ax, ay, bx, by, cx, cy, output.width, output.height)
                ) {
                    val depth = (preparedScreenZ[a] + preparedScreenZ[b] + preparedScreenZ[c]) / 3
                    visibleTriangleIndexes[visibleCount] = triangleIndex
                    visibleTriangleDepths[visibleCount] = depth
                    visibleTriangleAreas[visibleCount] = area
                    visibleCount += 1
                }
            }

            triangleIndex += 1
        }

        val selectionLimit = minOf(triangleBudget, visibleCount, TRIANGLE_SORT_CAPACITY)
        var selectedCount = 0
        while (selectedCount < selectionLimit) {
            var bestIndex = -1
            var bestDepth = Int.MAX_VALUE
            var visibleIndex = 0
            while (visibleIndex < visibleCount) {
                val depth = visibleTriangleDepths[visibleIndex]
                if (depth >= 0 && depth < bestDepth) {
                    bestDepth = depth
                    bestIndex = visibleIndex
                }
                visibleIndex += 1
            }
            if (bestIndex < 0) break

            selectedTriangleIndexes[selectedCount] = visibleTriangleIndexes[bestIndex]
            selectedTriangleDepths[selectedCount] = bestDepth
            selectedTriangleAreas[selectedCount] = visibleTriangleAreas[bestIndex]
            visibleTriangleDepths[bestIndex] = -1
            selectedCount += 1
        }

        if (overlayEdgeBudget > 0) {
            lastDrawnEdges = 0
            markSelectedTrianglesForFrame(selectedCount)
        }

        var drawn = 0
        var processed = 0
        while (processed < selectedCount) {
            var farthestIndex = -1
            var farthestDepth = -1
            var selectedIndex = 0
            while (selectedIndex < selectedCount) {
                val depth = selectedTriangleDepths[selectedIndex]
                if (depth >= 0 && depth > farthestDepth) {
                    farthestDepth = depth
                    farthestIndex = selectedIndex
                }
                selectedIndex += 1
            }
            if (farthestIndex < 0) break

            val triangleIndexToDraw = selectedTriangleIndexes[farthestIndex]
            if (drawModelTriangle(
                model = model,
                triangleIndex = triangleIndexToDraw,
                depth = farthestDepth,
                screenArea = selectedTriangleAreas[farthestIndex]
            )) {
                markTriangleDrawn(triangleIndexToDraw)
                drawn += 1
                if (overlayEdgeBudget > 0 && lastDrawnEdges < overlayEdgeBudget) {
                    lastDrawnEdges += drawPreparedModelEdgesForTriangle(model, triangleIndexToDraw, overlayEdgeBudget)
                }
            }
            selectedTriangleDepths[farthestIndex] = -1
            processed += 1
        }

        lastDrawnTriangles = drawn
        return drawn
    }

    fun drawModel(
        model: N64BakedWireModel3D,
        centerX: Int,
        centerY: Int,
        centerZ: Int,
        size: Int,
        rotationX: Int,
        rotationY: Int,
        rotationZ: Int,
        edgeBudget: Int
    ): Int {
        if (edgeBudget <= 0) {
            lastDrawnEdges = 0
            return 0
        }

        if (!prepareModel(model, centerX, centerY, centerZ, size, rotationX, rotationY, rotationZ)) {
            lastDrawnEdges = 0
            return 0
        }

        var drawn = 0
        var edgeIndex = 0
        while (edgeIndex < model.edgeCount && drawn < edgeBudget) {
            val edgeBase = edgeIndex * N64BakedWireModel3D.EDGE_FIELD_COUNT
            val start = model.edges[edgeBase]
            val end = model.edges[edgeBase + 1]
            val colorIndex = model.edges[edgeBase + 2]
            val firstTriangle = model.edges[edgeBase + 3]
            val secondTriangle = model.edges[edgeBase + 4]

            val color = if (colorIndex in model.colors.indices) {
                overlayColor(model.colors[colorIndex])
            } else {
                n64Rgba(156, 196, 224, 190)
            }
            if (drawPreparedModelEdge(start, end, firstTriangle, secondTriangle, color)) {
                drawn += 1
            }
            edgeIndex += 1
        }

        lastDrawnEdges = drawn
        return drawn
    }

    private fun drawModelTriangle(
        model: N64BakedWireModel3D,
        triangleIndex: Int,
        depth: Int,
        screenArea: Int
    ): Boolean {
        val output = render ?: return false
        val triangleBase = triangleIndex * N64BakedWireModel3D.TRIANGLE_FIELD_COUNT
        val a = model.triangles[triangleBase]
        val b = model.triangles[triangleBase + 1]
        val c = model.triangles[triangleBase + 2]
        val colorIndex = model.triangles[triangleBase + 3]

        if (!isPreparedVertexVisible(a) || !isPreparedVertexVisible(b) || !isPreparedVertexVisible(c)) {
            return false
        }

        val ax = preparedScreenX[a]
        val ay = preparedScreenY[a]
        val bx = preparedScreenX[b]
        val by = preparedScreenY[b]
        val cx = preparedScreenX[c]
        val cy = preparedScreenY[c]

        if (absInt(triangleArea(ax, ay, bx, by, cx, cy)) <= DEGENERATE_TRIANGLE_AREA ||
            isTriangleFarOutside(ax, ay, bx, by, cx, cy, output.width, output.height)
        ) {
            return false
        }

        val color = if (colorIndex in model.colors.indices) {
            shadedColor(model.colors[colorIndex], depth, triangleLight(a, b, c, screenArea))
        } else {
            n64Rgba(164, 196, 224)
        }
        output.drawTriangle(ax, ay, bx, by, cx, cy, color)
        return true
    }

    private fun prepareModel(
        model: N64BakedWireModel3D,
        centerX: Int,
        centerY: Int,
        centerZ: Int,
        size: Int,
        rotationX: Int,
        rotationY: Int,
        rotationZ: Int
    ): Boolean {
        if (model.vertexCount > MODEL_VERTEX_CAPACITY) {
            return false
        }

        if (preparedModel === model &&
            preparedCenterX == centerX &&
            preparedCenterY == centerY &&
            preparedCenterZ == centerZ &&
            preparedSize == size &&
            preparedRotationX == rotationX &&
            preparedRotationY == rotationY &&
            preparedRotationZ == rotationZ
        ) {
            return true
        }

        val cosX = cosAngle(rotationX)
        val sinX = sinAngle(rotationX)
        val cosY = cosAngle(rotationY)
        val sinY = sinAngle(rotationY)
        val cosZ = cosAngle(rotationZ)
        val sinZ = sinAngle(rotationZ)
        var depthMin = Int.MAX_VALUE
        var depthMax = Int.MIN_VALUE

        var vertexIndex = 0
        while (vertexIndex < model.vertexCount) {
            transformToWorld(model.vertices, vertexIndex, size, centerX, centerY, centerZ, cosX, sinX, cosY, sinY, cosZ, sinZ)
            if (projectToScreen(worldX, worldY, worldZ) && isProjectedVertexReasonable(projectedX, projectedY)) {
                preparedScreenX[vertexIndex] = projectedX
                preparedScreenY[vertexIndex] = projectedY
                preparedScreenZ[vertexIndex] = projectedZ
                preparedVisible[vertexIndex] = 1
                if (projectedZ < depthMin) depthMin = projectedZ
                if (projectedZ > depthMax) depthMax = projectedZ
            } else {
                preparedVisible[vertexIndex] = 0
            }
            vertexIndex += 1
        }

        preparedModel = model
        preparedCenterX = centerX
        preparedCenterY = centerY
        preparedCenterZ = centerZ
        preparedSize = size
        preparedRotationX = rotationX
        preparedRotationY = rotationY
        preparedRotationZ = rotationZ
        preparedDepthMin = if (depthMin == Int.MAX_VALUE) 0 else depthMin
        preparedDepthMax = if (depthMax == Int.MIN_VALUE) 0 else depthMax
        return true
    }

    private fun isProjectedVertexReasonable(x: Int, y: Int): Boolean {
        return x > -PROJECTED_COORD_LIMIT &&
            x < PROJECTED_COORD_LIMIT &&
            y > -PROJECTED_COORD_LIMIT &&
            y < PROJECTED_COORD_LIMIT
    }

    private fun drawPreparedModelEdge(
        start: Int,
        end: Int,
        firstTriangle: Int,
        secondTriangle: Int,
        color: Int,
        allowRearDepthOverlay: Boolean = false
    ): Boolean {
        val output = render ?: return false
        if (!isPreparedVertexVisible(start) || !isPreparedVertexVisible(end)) {
            return false
        }

        val ax = preparedScreenX[start]
        val ay = preparedScreenY[start]
        val bx = preparedScreenX[end]
        val by = preparedScreenY[end]
        if (!isPreparedEdgeReferencedByDrawnTriangle(firstTriangle, secondTriangle)) {
            return false
        }
        if (!allowRearDepthOverlay && isPreparedEdgeLikelyHidden(start, end)) {
            return false
        }
        if (isFarOutside(ax, ay, output.width, output.height) &&
            isFarOutside(bx, by, output.width, output.height)
        ) {
            return false
        }

        output.drawLine(ax, ay, bx, by, color)
        return true
    }

    private fun beginTriangleDrawTracking(trackEdges: Boolean) {
        drawnTriangleStamp += 1
        if (drawnTriangleStamp == Int.MAX_VALUE) {
            drawnTriangleMarkers.fill(0)
            drawnTriangleStamp = 1
        }
        selectedTriangleStamp += 1
        if (selectedTriangleStamp == Int.MAX_VALUE) {
            selectedTriangleMarkers.fill(0)
            selectedTriangleStamp = 1
        }
        if (trackEdges) {
            drawnEdgeStamp += 1
            if (drawnEdgeStamp == Int.MAX_VALUE) {
                drawnEdgeMarkers.fill(0)
                drawnEdgeStamp = 1
            }
        }
        drawnTriangleCountForEdges = 0
    }

    private fun markTriangleDrawn(triangleIndex: Int) {
        if (triangleIndex !in 0 until TRIANGLE_SORT_CAPACITY) {
            return
        }
        if (drawnTriangleMarkers[triangleIndex] != drawnTriangleStamp) {
            drawnTriangleMarkers[triangleIndex] = drawnTriangleStamp
            drawnTriangleCountForEdges += 1
        }
    }

    private fun isPreparedEdgeReferencedByDrawnTriangle(firstTriangle: Int, secondTriangle: Int): Boolean {
        if (drawnTriangleCountForEdges <= 0) {
            return true
        }
        return isTriangleMarkedDrawn(firstTriangle) || isTriangleMarkedDrawn(secondTriangle)
    }

    private fun isTriangleMarkedDrawn(triangleIndex: Int): Boolean {
        return triangleIndex in 0 until TRIANGLE_SORT_CAPACITY &&
            drawnTriangleMarkers[triangleIndex] == drawnTriangleStamp
    }

    private fun markSelectedTrianglesForFrame(selectedCount: Int) {
        var index = 0
        while (index < selectedCount) {
            val triangleIndex = selectedTriangleIndexes[index]
            if (triangleIndex in 0 until TRIANGLE_SORT_CAPACITY) {
                selectedTriangleMarkers[triangleIndex] = selectedTriangleStamp
            }
            index += 1
        }
    }

    private fun isTriangleSelectedForFrame(triangleIndex: Int): Boolean {
        return triangleIndex in 0 until TRIANGLE_SORT_CAPACITY &&
            selectedTriangleMarkers[triangleIndex] == selectedTriangleStamp
    }

    private fun drawPreparedModelEdgesForTriangle(
        model: N64BakedWireModel3D,
        triangleIndex: Int,
        edgeBudget: Int
    ): Int {
        var drawn = 0
        var edgeIndex = 0
        while (edgeIndex < model.edgeCount && lastDrawnEdges + drawn < edgeBudget) {
            val edgeBase = edgeIndex * N64BakedWireModel3D.EDGE_FIELD_COUNT
            val firstTriangle = model.edges[edgeBase + 3]
            val secondTriangle = model.edges[edgeBase + 4]
            if (!isEdgeMarkedDrawn(edgeIndex) &&
                shouldDrawEdgeForTriangleTurn(triangleIndex, firstTriangle, secondTriangle)
            ) {
                val start = model.edges[edgeBase]
                val end = model.edges[edgeBase + 1]
                val colorIndex = model.edges[edgeBase + 2]
                val color = if (colorIndex in model.colors.indices) {
                    overlayColor(model.colors[colorIndex])
                } else {
                    n64Rgba(156, 196, 224, 190)
                }
                if (drawPreparedModelEdge(start, end, firstTriangle, secondTriangle, color, allowRearDepthOverlay = true)) {
                    markEdgeDrawn(edgeIndex)
                    drawn += 1
                }
            }
            edgeIndex += 1
        }
        return drawn
    }

    private fun shouldDrawEdgeForTriangleTurn(currentTriangle: Int, firstTriangle: Int, secondTriangle: Int): Boolean {
        if (currentTriangle != firstTriangle && currentTriangle != secondTriangle) {
            return false
        }

        val otherTriangle = if (currentTriangle == firstTriangle) secondTriangle else firstTriangle
        if (!isTriangleSelectedForFrame(otherTriangle)) {
            return true
        }
        return isTriangleMarkedDrawn(otherTriangle)
    }

    private fun markEdgeDrawn(edgeIndex: Int) {
        if (edgeIndex in 0 until MODEL_EDGE_CAPACITY) {
            drawnEdgeMarkers[edgeIndex] = drawnEdgeStamp
        }
    }

    private fun isEdgeMarkedDrawn(edgeIndex: Int): Boolean {
        return edgeIndex in 0 until MODEL_EDGE_CAPACITY &&
            drawnEdgeMarkers[edgeIndex] == drawnEdgeStamp
    }

    private fun isPreparedEdgeLikelyHidden(start: Int, end: Int): Boolean {
        val depthSpan = preparedDepthMax - preparedDepthMin
        if (depthSpan <= WORLD_SCALE / 8) {
            return false
        }

        val edgeDepth = (preparedScreenZ[start] + preparedScreenZ[end]) / 2
        val rearOverlayLimit = preparedDepthMin + (depthSpan * EDGE_OVERLAY_DEPTH_PERCENT) / 100
        return edgeDepth > rearOverlayLimit
    }

    private fun isPreparedVertexVisible(vertexIndex: Int): Boolean {
        return vertexIndex in 0 until MODEL_VERTEX_CAPACITY && preparedVisible[vertexIndex] != 0
    }

    fun drawLine3D(
        startX: Int,
        startY: Int,
        startZ: Int,
        endX: Int,
        endY: Int,
        endZ: Int,
        color: Int
    ): Boolean {
        val output = render ?: return false
        if (!projectToScreen(startX, startY, startZ)) return false
        val ax = projectedX
        val ay = projectedY
        if (!projectToScreen(endX, endY, endZ)) return false
        val bx = projectedX
        val by = projectedY

        if (isFarOutside(ax, ay, output.width, output.height) &&
            isFarOutside(bx, by, output.width, output.height)
        ) {
            return false
        }

        output.drawLine(ax, ay, bx, by, color)
        return true
    }

    private fun projectModelVertex(
        model: N64BakedWireModel3D,
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
    ): Boolean {
        transformToWorld(model.vertices, vertexIndex, size, centerX, centerY, centerZ, cosX, sinX, cosY, sinY, cosZ, sinZ)
        return projectToScreen(worldX, worldY, worldZ)
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
        var x = scaleValue(vertices[base], size, MODEL_SCALE)
        val y = scaleValue(vertices[base + 1], size, MODEL_SCALE)
        var z = scaleValue(vertices[base + 2], size, MODEL_SCALE)

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
        projectedZ = viewZ
        return true
    }

    private fun isFarOutside(x: Int, y: Int, width: Int, height: Int): Boolean {
        return x < -OFFSCREEN_MARGIN ||
            x > width + OFFSCREEN_MARGIN ||
            y < -OFFSCREEN_MARGIN ||
            y > height + OFFSCREEN_MARGIN
    }

    private fun isTriangleFarOutside(
        ax: Int,
        ay: Int,
        bx: Int,
        by: Int,
        cx: Int,
        cy: Int,
        width: Int,
        height: Int
    ): Boolean {
        return (ax < -OFFSCREEN_MARGIN && bx < -OFFSCREEN_MARGIN && cx < -OFFSCREEN_MARGIN) ||
            (ax > width + OFFSCREEN_MARGIN && bx > width + OFFSCREEN_MARGIN && cx > width + OFFSCREEN_MARGIN) ||
            (ay < -OFFSCREEN_MARGIN && by < -OFFSCREEN_MARGIN && cy < -OFFSCREEN_MARGIN) ||
            (ay > height + OFFSCREEN_MARGIN && by > height + OFFSCREEN_MARGIN && cy > height + OFFSCREEN_MARGIN)
    }

    private fun triangleArea(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int): Int {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
    }

    private fun triangleLight(a: Int, b: Int, c: Int, screenArea: Int): Int {
        val averageY = (preparedScreenY[a] + preparedScreenY[b] + preparedScreenY[c]) / 3
        val heightLight = clampInt((centerY - averageY) / 2, -42, 58)
        val areaLight = clampInt(absInt(screenArea) / 105, 0, 56)
        val windingLight = if (screenArea >= 0) 16 else -18
        return 52 + heightLight + areaLight + windingLight
    }

    private fun shadedColor(color: Int, depth: Int, faceLight: Int): Int {
        val shade = clampInt(120 + faceLight - ((depth - cameraDistance) / 25), 68, 255)
        return n64Rgba(
            red = ((color and 0xff) * shade) / 255,
            green = (((color ushr 8) and 0xff) * shade) / 255,
            blue = (((color ushr 16) and 0xff) * shade) / 255,
            alpha = (color ushr 24) and 0xff
        )
    }

    private fun overlayColor(color: Int): Int {
        return n64Rgba(
            red = (((color and 0xff) * 2) + 84) / 3,
            green = ((((color ushr 8) and 0xff) * 2) + 108) / 3,
            blue = ((((color ushr 16) and 0xff) * 2) + 132) / 3,
            alpha = 185
        )
    }

    companion object {
        const val WORLD_SCALE = 256
        const val MODEL_SCALE = 256
        const val TRIG_SCALE = 4096
        const val ANGLE_FULL = 1024
        const val ANGLE_RIGHT = ANGLE_FULL / 4

        private const val NEAR_PLANE = WORLD_SCALE / 5
        private const val OFFSCREEN_MARGIN = 96
        private const val PROJECTED_COORD_LIMIT = 1024
        private const val MODEL_VERTEX_CAPACITY = 512
        private const val TRIANGLE_SORT_CAPACITY = 768
        private const val MODEL_EDGE_CAPACITY = 1024
        private const val DEGENERATE_TRIANGLE_AREA = 3
        private const val EDGE_OVERLAY_DEPTH_PERCENT = 62
    }
}

internal fun sinAngle(angle: Int): Int {
    val wrapped = angle and (N64WireModelRenderer3D.ANGLE_FULL - 1)
    return when {
        wrapped < N64WireModelRenderer3D.ANGLE_RIGHT -> quarterSin(wrapped)
        wrapped < N64WireModelRenderer3D.ANGLE_RIGHT * 2 -> quarterSin(N64WireModelRenderer3D.ANGLE_RIGHT * 2 - wrapped)
        wrapped < N64WireModelRenderer3D.ANGLE_RIGHT * 3 -> -quarterSin(wrapped - N64WireModelRenderer3D.ANGLE_RIGHT * 2)
        else -> -quarterSin(N64WireModelRenderer3D.ANGLE_FULL - wrapped)
    }
}

internal fun cosAngle(angle: Int): Int = sinAngle(angle + N64WireModelRenderer3D.ANGLE_RIGHT)

internal fun trigMul(value: Int, trig: Int): Int {
    return (value * trig) / N64WireModelRenderer3D.TRIG_SCALE
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
    val bounded = clampInt(value, 0, N64WireModelRenderer3D.ANGLE_RIGHT)
    val numerator = bounded * (N64WireModelRenderer3D.ANGLE_RIGHT * 2 - bounded)
    return scaleValue(numerator, N64WireModelRenderer3D.TRIG_SCALE, N64WireModelRenderer3D.ANGLE_RIGHT * N64WireModelRenderer3D.ANGLE_RIGHT)
}

internal fun clampInt(value: Int, min: Int, max: Int): Int {
    return when {
        value < min -> min
        value > max -> max
        else -> value
    }
}

internal fun absInt(value: Int): Int = if (value < 0) -value else value

internal fun wrapAngle(angle: Int): Int = angle and (N64WireModelRenderer3D.ANGLE_FULL - 1)

internal fun n64Rgba(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
    return clampColor(red) or
        (clampColor(green) shl 8) or
        (clampColor(blue) shl 16) or
        (clampColor(alpha) shl 24)
}

private fun clampColor(value: Int): Int {
    return when {
        value < 0 -> 0
        value > 255 -> 255
        else -> value
    }
}
