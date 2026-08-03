package hextris

class Piece(val type: PieceType, val color: Int) {
    private val baseShape = type.shape.map { GridPoint(it.first, it.second) }

    var rotation: Int = 0
        private set

    fun getBlocks(): List<GridPoint> {
        return when (rotation) {
            0 -> baseShape
            1 -> baseShape.map { GridPoint(-it.y, it.x) }
            2 -> baseShape.map { GridPoint(-it.x, -it.y) }
            3 -> baseShape.map { GridPoint(it.y, -it.x) }
            else -> baseShape
        }
    }

    fun rotateClockwise() {
        rotation = (rotation + 1) % 4
    }

    fun rotateCounterClockwise() {
        rotation = (rotation + 3) % 4
    }
}

enum class PieceType(val shape: List<Pair<Int, Int>>) {
    O(listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)),
    L(listOf(-1 to 1, -1 to 0, 0 to 0, 1 to 0)),
    J(listOf(-1 to 0, 0 to 0, 1 to 0, 1 to 1)),
    I(listOf(-1 to 0, 0 to 0, 1 to 0, 2 to 0)),
    S(listOf(1 to 0, 0 to 0, 0 to 1, -1 to 1)),
    Z(listOf(1 to 1, 0 to 1, 0 to 0, -1 to 0)),
    T(listOf(0 to 1, -1 to 0, 0 to 0, 1 to 0)),
    DOT(listOf(0 to 0)),
    SCREW(listOf(-1 to -1, -1 to 0, 0 to 0, 1 to 0, 1 to 1)),
    SCREW_BACKWARDS(listOf(1 to -1, 1 to 0, 0 to 0, -1 to 0, -1 to 1)),
    LONG_CROSS(listOf(-1 to 0, 0 to 0, 1 to 0, 0 to -1, 0 to 1)),
    CROSS(listOf(-1 to 0, 0 to 0, 1 to 0, 0 to -1, 0 to 1, 2 to 0)),
    LAYERS(listOf(-1 to -1, 0 to -1, 1 to -1, -1 to 1, 0 to 1, 1 to 1)),
    Y(listOf(-1 to -1, -1 to 0, 0 to 0, 0 to 1, 1 to -1, 1 to 0)),
    U(listOf(-1 to 0, -1 to 1, 0 to 1, 1 to 0, 1 to 1)),
    LINE_5(listOf(-2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0)),
    LINE_6(listOf(-2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0, 3 to 0)),
    BLOCK_3X2(listOf(-1 to 0, 0 to 0, 1 to 0, -1 to 1, 0 to 1, 1 to 1)),
    ZIG_ZAG(listOf(-1 to 0, 0 to 1, 1 to 0)),
    NOTCH_TOP(listOf(-1 to 0, 0 to 0, -1 to 1, 0 to 1, 1 to 0)),
    NOTCH_BOTTOM(listOf(-1 to 0, 0 to 0, -1 to 1, 0 to 1, 1 to 1)),
    LINE_2(listOf(0 to 0, 0 to 1)),
    BIG_T(listOf(-1 to -1, 0 to -1, 1 to -1, 0 to 0, 0 to 1)),
    SHORT_PARALLEL(listOf(-1 to 0, -1 to 1, 1 to 0, 1 to 1)),
    BIG_L_BACKWARDS(listOf(-1 to -1, 0 to -1, 1 to -1, 1 to 0, 1 to 1)),
    TO(listOf(-2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0, 0 to 1)),
    SMALL_L(listOf(-1 to 0, 0 to 0, 0 to 1)),
    LINE_3(listOf(-1 to 0, 0 to 0, 1 to 0));

    companion object {
        fun fromSeed(seed: Int): PieceType {
            val values = entries
            val index = (seed and Int.MAX_VALUE) % values.size
            return values[index]
        }
    }
}
