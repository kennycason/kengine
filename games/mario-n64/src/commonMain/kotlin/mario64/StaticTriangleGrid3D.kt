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
        const val TRIANGLE_STRIDE = 4
        const val TRIANGLE_FLAGS = 3
        const val FLAG_FLOOR = 1
        const val NO_GROUND = Int.MIN_VALUE
        const val NO_TRIANGLE = -1
    }
}
