package mario64

class Mario64BakedWorld(
    val name: String,
    val vertices: IntArray,
    val triangles: IntArray,
    val colors: IntArray
) {
    val vertexCount: Int
        get() = vertices.size / 3

    val triangleCount: Int
        get() = triangles.size / TRIANGLE_FIELD_COUNT

    companion object {
        const val TRIANGLE_FIELD_COUNT = 4
    }
}
