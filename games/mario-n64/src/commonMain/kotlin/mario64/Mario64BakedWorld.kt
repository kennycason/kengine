package mario64

class Mario64BakedWorld(
    val name: String,
    val vertices: IntArray,
    val triangles: IntArray,
    val colors: IntArray
) {
    val vertexCount: Int
        get() = vertices.size / VERTEX_STRIDE

    val triangleCount: Int
        get() = triangles.size / TRIANGLE_FIELD_COUNT

    companion object {
        const val VERTEX_STRIDE = 5
        const val TRIANGLE_FIELD_COUNT = 4
    }
}
