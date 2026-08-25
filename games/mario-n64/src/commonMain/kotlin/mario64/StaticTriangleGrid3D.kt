package mario64

/**
 * Allocation-free broad phase and floor query for a static triangle mesh.
 * This starts game-local while the API is proven, but contains no Mario or N64
 * assumptions and is intended to move into a reusable engine module. Asset
 * generation must validate that coordinate-difference cross-products fit Int.
 */
class StaticTriangleGrid3D(
    val vertices: IntArray,
    val triangles: IntArray,
    val cellOffsets: IntArray,
    val cellTriangleIndices: IntArray,
    private val metadata: IntArray
) {
    val minX: Int
        get() = metadata[METADATA_MIN_X]

    val minY: Int
        get() = metadata[METADATA_MIN_Y]

    val minZ: Int
        get() = metadata[METADATA_MIN_Z]

    val maxX: Int
        get() = metadata[METADATA_MAX_X]

    val maxY: Int
        get() = metadata[METADATA_MAX_Y]

    val maxZ: Int
        get() = metadata[METADATA_MAX_Z]

    val gridWidth: Int
        get() = metadata[METADATA_GRID_WIDTH]

    val gridDepth: Int
        get() = metadata[METADATA_GRID_DEPTH]

    val cellSize: Int
        get() = metadata[METADATA_CELL_SIZE]

    var lastCandidateCount: Int = 0
        private set

    var lastSupportTriangle: Int = NO_TRIANGLE
        private set

    var lastResolvedX: Int = 0
        private set

    var lastResolvedZ: Int = 0
        private set

    var lastWallTriangle: Int = NO_TRIANGLE
        private set

    var lastWallCandidateCount: Int = 0
        private set

    var lastWallCollisionCount: Int = 0
        private set

    val vertexCount: Int
        get() = vertices.size / VERTEX_STRIDE

    val triangleCount: Int
        get() = triangles.size / TRIANGLE_STRIDE

    fun groundHeightAt(x: Int, z: Int, maximumY: Int = Int.MAX_VALUE): Int {
        lastCandidateCount = 0
        lastSupportTriangle = NO_TRIANGLE
        if (x < minX || x > maxX || z < minZ || z > maxZ) return NO_GROUND

        var cellX = (x - minX) / cellSize
        var cellZ = (z - minZ) / cellSize
        if (cellX >= gridWidth) cellX = gridWidth - 1
        if (cellZ >= gridDepth) cellZ = gridDepth - 1
        val cellIndex = cellZ * gridWidth + cellX
        val referenceStart = cellOffsets[cellIndex]
        val referenceEnd = cellOffsets[cellIndex + 1]
        lastCandidateCount = referenceEnd - referenceStart

        var bestHeight = NO_GROUND
        var referenceIndex = referenceStart
        while (referenceIndex < referenceEnd) {
            val triangleIndex = cellTriangleIndices[referenceIndex]
            val triangleBase = triangleIndex * TRIANGLE_STRIDE
            if ((triangles[triangleBase + TRIANGLE_FLAGS] and FLAG_FLOOR) != 0) {
                val aBase = triangles[triangleBase] * VERTEX_STRIDE
                val bBase = triangles[triangleBase + 1] * VERTEX_STRIDE
                val cBase = triangles[triangleBase + 2] * VERTEX_STRIDE
                val ax = vertices[aBase]
                val ay = vertices[aBase + 1]
                val az = vertices[aBase + 2]
                val bx = vertices[bBase]
                val by = vertices[bBase + 1]
                val bz = vertices[bBase + 2]
                val cx = vertices[cBase]
                val cy = vertices[cBase + 1]
                val cz = vertices[cBase + 2]

                var area = edge(ax, az, bx, bz, cx, cz)
                var weightA = edge(bx, bz, cx, cz, x, z)
                var weightB = edge(cx, cz, ax, az, x, z)
                var weightC = edge(ax, az, bx, bz, x, z)
                if (area < 0) {
                    area = -area
                    weightA = -weightA
                    weightB = -weightB
                    weightC = -weightC
                }

                if (area > 0 && weightA >= 0 && weightB >= 0 && weightC >= 0) {
                    val height = interpolateHeight(
                        area = area,
                        weightB = weightB,
                        weightC = weightC,
                        ay = ay,
                        by = by,
                        cy = cy
                    )
                    if (height <= maximumY && (bestHeight == NO_GROUND || height > bestHeight)) {
                        bestHeight = height
                        lastSupportTriangle = triangleIndex
                    }
                }
            }
            referenceIndex += 1
        }
        return bestHeight
    }

    /**
     * Uses the exact center query first, then four tiny edge probes only when
     * the center finds no floor. This bridges quantized triangle seams without
     * turning the complete body radius into a cliff-hovering support point.
     */
    fun groundHeightNear(
        x: Int,
        z: Int,
        maximumY: Int = Int.MAX_VALUE,
        probeDistance: Int = DEFAULT_GROUND_PROBE_DISTANCE
    ): Int {
        val centerHeight = groundHeightAt(x, z, maximumY)
        if (centerHeight != NO_GROUND || probeDistance <= 0) return centerHeight

        var candidateTotal = lastCandidateCount
        var bestHeight = NO_GROUND
        var bestTriangle = NO_TRIANGLE
        var probeIndex = 0
        while (probeIndex < 4) {
            val probeX = when (probeIndex) {
                0 -> x - probeDistance
                1 -> x + probeDistance
                else -> x
            }
            val probeZ = when (probeIndex) {
                2 -> z - probeDistance
                3 -> z + probeDistance
                else -> z
            }
            val height = groundHeightAt(probeX, probeZ, maximumY)
            candidateTotal += lastCandidateCount
            if (height != NO_GROUND && (bestHeight == NO_GROUND || height > bestHeight)) {
                bestHeight = height
                bestTriangle = lastSupportTriangle
            }
            probeIndex += 1
        }

        lastCandidateCount = candidateTotal
        lastSupportTriangle = bestTriangle
        return bestHeight
    }

    /**
     * Resolves a horizontal circle against steep triangles at [sampleY]. The
     * result is exposed through [lastResolvedX]/[lastResolvedZ] to keep the hot
     * path allocation-free on the no-op-GC N64 runtime.
     */
    fun resolveWalls(
        x: Int,
        sampleY: Int,
        z: Int,
        radius: Int,
        maximumIterations: Int = DEFAULT_WALL_ITERATIONS
    ): Boolean {
        lastResolvedX = x
        lastResolvedZ = z
        lastWallTriangle = NO_TRIANGLE
        lastWallCandidateCount = 0
        lastWallCollisionCount = 0
        if (radius <= 0 || maximumIterations <= 0) return false
        if (x + radius < minX || x - radius > maxX || z + radius < minZ || z - radius > maxZ) {
            return false
        }

        var iteration = 0
        while (iteration < maximumIterations) {
            val minCellX = gridCoordinate(lastResolvedX - radius, minX, gridWidth)
            val maxCellX = gridCoordinate(lastResolvedX + radius, minX, gridWidth)
            val minCellZ = gridCoordinate(lastResolvedZ - radius, minZ, gridDepth)
            val maxCellZ = gridCoordinate(lastResolvedZ + radius, minZ, gridDepth)
            var iterationCollisions = 0
            var cellZ = minCellZ
            while (cellZ <= maxCellZ) {
                var cellX = minCellX
                while (cellX <= maxCellX) {
                    val cellIndex = cellZ * gridWidth + cellX
                    val referenceStart = cellOffsets[cellIndex]
                    val referenceEnd = cellOffsets[cellIndex + 1]
                    lastWallCandidateCount += referenceEnd - referenceStart
                    var referenceIndex = referenceStart
                    while (referenceIndex < referenceEnd) {
                        val triangleIndex = cellTriangleIndices[referenceIndex]
                        val triangleBase = triangleIndex * TRIANGLE_STRIDE
                        if ((triangles[triangleBase + TRIANGLE_FLAGS] and FLAG_FLOOR) == 0 &&
                            resolveWallTriangle(triangleBase, sampleY, radius)
                        ) {
                            iterationCollisions += 1
                            lastWallCollisionCount += 1
                            lastWallTriangle = triangleIndex
                        }
                        referenceIndex += 1
                    }
                    cellX += 1
                }
                cellZ += 1
            }
            if (iterationCollisions == 0) break
            iteration += 1
        }
        return lastWallCollisionCount > 0
    }

    private fun resolveWallTriangle(
        triangleBase: Int,
        sampleY: Int,
        radius: Int
    ): Boolean {
        val normalX = triangles[triangleBase + TRIANGLE_NORMAL_X]
        val normalZ = triangles[triangleBase + TRIANGLE_NORMAL_Z]
        if (normalX == 0 && normalZ == 0) return false
        if (!wallProjectionContains(triangleBase, lastResolvedX, sampleY, lastResolvedZ)) return false

        val planeValue = normalX * lastResolvedX +
            triangles[triangleBase + TRIANGLE_NORMAL_Y] * sampleY +
            normalZ * lastResolvedZ +
            triangles[triangleBase + TRIANGLE_ORIGIN_OFFSET]
        val signedDistance = roundedDivide(planeValue, NORMAL_SCALE)
        val absoluteDistance = absInt(signedDistance)
        if (absoluteDistance >= radius) return false

        val direction = if (signedDistance < 0) -1 else 1
        val pushDistance = radius - absoluteDistance
        var pushX = roundedDivide(normalX * pushDistance * direction, NORMAL_SCALE)
        var pushZ = roundedDivide(normalZ * pushDistance * direction, NORMAL_SCALE)
        if (pushX == 0 && normalX != 0) pushX = if (normalX * direction < 0) -1 else 1
        if (pushZ == 0 && normalZ != 0) pushZ = if (normalZ * direction < 0) -1 else 1
        lastResolvedX += pushX
        lastResolvedZ += pushZ
        return true
    }

    private fun wallProjectionContains(triangleBase: Int, x: Int, y: Int, z: Int): Boolean {
        val aBase = triangles[triangleBase] * VERTEX_STRIDE
        val bBase = triangles[triangleBase + 1] * VERTEX_STRIDE
        val cBase = triangles[triangleBase + 2] * VERTEX_STRIDE
        val normalX = triangles[triangleBase + TRIANGLE_NORMAL_X]
        val normalZ = triangles[triangleBase + TRIANGLE_NORMAL_Z]
        return if (absInt(normalX) >= absInt(normalZ)) {
            pointInTriangle2D(
                vertices[aBase + 2], vertices[aBase + 1],
                vertices[bBase + 2], vertices[bBase + 1],
                vertices[cBase + 2], vertices[cBase + 1],
                z, y
            )
        } else {
            pointInTriangle2D(
                vertices[aBase], vertices[aBase + 1],
                vertices[bBase], vertices[bBase + 1],
                vertices[cBase], vertices[cBase + 1],
                x, y
            )
        }
    }

    private fun pointInTriangle2D(
        au: Int,
        av: Int,
        bu: Int,
        bv: Int,
        cu: Int,
        cv: Int,
        u: Int,
        v: Int
    ): Boolean {
        var area = edge(au, av, bu, bv, cu, cv)
        var weightA = edge(bu, bv, cu, cv, u, v)
        var weightB = edge(cu, cv, au, av, u, v)
        var weightC = edge(au, av, bu, bv, u, v)
        if (area < 0) {
            area = -area
            weightA = -weightA
            weightB = -weightB
            weightC = -weightC
        }
        return area > 0 && weightA >= 0 && weightB >= 0 && weightC >= 0
    }

    private fun gridCoordinate(value: Int, minimum: Int, size: Int): Int {
        val coordinate = (value - minimum) / cellSize
        return when {
            coordinate < 0 -> 0
            coordinate >= size -> size - 1
            else -> coordinate
        }
    }

    private fun edge(
        ax: Int,
        az: Int,
        bx: Int,
        bz: Int,
        px: Int,
        pz: Int
    ): Int {
        return (bx - ax) * (pz - az) - (bz - az) * (px - ax)
    }

    /**
     * Interpolates relative to A so the large absolute Y component never
     * enters the numerator. The weights and denominator are scaled together
     * only when needed to keep both products inside signed 32-bit range.
     * World coordinates are limited to the generated mesh bounds, so the
     * edge products themselves are also safe signed Int values.
     */
    private fun interpolateHeight(
        area: Int,
        weightB: Int,
        weightC: Int,
        ay: Int,
        by: Int,
        cy: Int
    ): Int {
        val deltaB = by - ay
        val deltaC = cy - ay
        val maximumDelta = maxOf(absInt(deltaB), absInt(deltaC))
        if (maximumDelta == 0) return ay

        val safeAreaLimit = Int.MAX_VALUE / maximumDelta / 2
        var scaledArea = area
        var scaledWeightB = weightB
        var scaledWeightC = weightC
        while (scaledArea > safeAreaLimit) {
            scaledArea = (scaledArea + 1) / 2
            scaledWeightB = (scaledWeightB + 1) / 2
            scaledWeightC = (scaledWeightC + 1) / 2
        }

        val heightNumerator = scaledWeightB * deltaB + scaledWeightC * deltaC
        return ay + roundedDivide(heightNumerator, scaledArea)
    }

    private fun absInt(value: Int): Int = if (value < 0) -value else value

    private fun roundedDivide(numerator: Int, denominator: Int): Int {
        return if (numerator >= 0) {
            (numerator + denominator / 2) / denominator
        } else {
            (numerator - denominator / 2) / denominator
        }
    }

    companion object {
        const val METADATA_FIELD_COUNT = 9
        const val METADATA_MIN_X = 0
        const val METADATA_MIN_Y = 1
        const val METADATA_MIN_Z = 2
        const val METADATA_MAX_X = 3
        const val METADATA_MAX_Y = 4
        const val METADATA_MAX_Z = 5
        const val METADATA_GRID_WIDTH = 6
        const val METADATA_GRID_DEPTH = 7
        const val METADATA_CELL_SIZE = 8
        const val VERTEX_STRIDE = 3
        const val TRIANGLE_STRIDE = 8
        const val TRIANGLE_FLAGS = 3
        const val TRIANGLE_NORMAL_X = 4
        const val TRIANGLE_NORMAL_Y = 5
        const val TRIANGLE_NORMAL_Z = 6
        const val TRIANGLE_ORIGIN_OFFSET = 7
        const val FLAG_FLOOR = 1
        const val NO_GROUND = Int.MIN_VALUE
        const val NO_TRIANGLE = -1
        const val NORMAL_SCALE = 1024
        private const val DEFAULT_GROUND_PROBE_DISTANCE = 8
        private const val DEFAULT_WALL_ITERATIONS = 3
    }
}
