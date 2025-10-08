package hextris

import com.kengine.math.IntVec2

/**
 * Represents a Hextris piece.
 */
class Piece(val type: PieceType, val color: Int) {
    // Current rotation state (0-3)
    var rotation: Int = 0
        private set

    // Get the blocks for the current rotation
    fun getBlocks(): List<IntVec2> {
        return type.rotations[rotation].map { IntVec2(it.first, it.second) }
    }

    // Rotate the piece clockwise
    fun rotateClockwise() {
        rotation = (rotation + 1) % type.rotations.size
    }

    // Rotate the piece counter-clockwise
    fun rotateCounterClockwise() {
        rotation = (rotation + type.rotations.size - 1) % type.rotations.size
    }

    // Create a copy of this piece
    fun copy(): Piece {
        return Piece(type, color).also { it.rotation = this.rotation }
    }
}

/**
 * Defines the different types of Hextris pieces.
 * Each piece type has 4 rotation states.
 */
enum class PieceType(val rotations: List<List<Pair<Int, Int>>>) {
    // 1. Square (O piece)
    O(listOf(
        listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1))
    )),

    // 2. L piece
    L(listOf(
        listOf(Pair(-1, 1), Pair(-1, 0), Pair(0, 0), Pair(1, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, -1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(1, -1)),
        listOf(Pair(-1, 1), Pair(0, -1), Pair(0, 0), Pair(0, 1))
    )),

    // 3. J piece (L-backwards)
    J(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(-1, -1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(1, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, 1))
    )),

    // 4. I piece (line)
    I(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2))
    )),

    // 5. S piece (N)
    S(listOf(
        listOf(Pair(1, 0), Pair(0, 0), Pair(0, 1), Pair(-1, 1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1)),
        listOf(Pair(1, 0), Pair(0, 0), Pair(0, 1), Pair(-1, 1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1))
    )),

    // 6. Z piece (N-backwards)
    Z(listOf(
        listOf(Pair(1, 1), Pair(0, 1), Pair(0, 0), Pair(-1, 0)),
        listOf(Pair(1, -1), Pair(1, 0), Pair(0, 0), Pair(0, 1)),
        listOf(Pair(1, 1), Pair(0, 1), Pair(0, 0), Pair(-1, 0)),
        listOf(Pair(1, -1), Pair(1, 0), Pair(0, 0), Pair(0, 1))
    )),

    // 7. T piece
    T(listOf(
        listOf(Pair(0, 1), Pair(-1, 0), Pair(0, 0), Pair(1, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(1, 0), Pair(0, 1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1)),
        listOf(Pair(0, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1))
    )),

    // 8. Dot piece
    DOT(listOf(
        listOf(Pair(0, 0)),
        listOf(Pair(0, 0)),
        listOf(Pair(0, 0)),
        listOf(Pair(0, 0))
    )),

    // 9. Screw piece
    SCREW(listOf(
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(1, -1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(-1, 1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(1, -1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(-1, 1))
    )),

    // 10. Screw backwards piece
    SCREW_BACKWARDS(listOf(
        listOf(Pair(1, -1), Pair(1, 0), Pair(0, 0), Pair(-1, 0), Pair(-1, 1)),
        listOf(Pair(-1, -1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(1, -1), Pair(1, 0), Pair(0, 0), Pair(-1, 0), Pair(-1, 1)),
        listOf(Pair(-1, -1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, 1))
    )),

    // 11. Long cross piece
    LONG_CROSS(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
    )),

    // 12. Cross piece
    CROSS(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1), Pair(2, 0)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1), Pair(0, 2)),
        listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -2), Pair(0, -1), Pair(0, 1))
    )),

    // 13. Layers piece
    LAYERS(listOf(
        listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(-1, 1), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(-1, 1), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1))
    )),

    // 14. Y piece
    Y(listOf(
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1), Pair(1, -1), Pair(1, 0)),
        listOf(Pair(-1, -1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, 1), Pair(1, 0)),
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, 0), Pair(0, -1), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(-1, 1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, -1), Pair(1, 0))
    )),

    // 15. U piece
    U(listOf(
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(-1, -1), Pair(1, -1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, -1), Pair(1, -1), Pair(1, 0)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(-1, 1), Pair(1, 1))
    )),

    // 16. 5 line piece
    LINE_5(listOf(
        listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0)),  // Horizontal
        listOf(Pair(0, -2), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2)),  // Vertical
        listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0)),  // Horizontal
        listOf(Pair(0, -2), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2))   // Vertical
    )),

    // 17. 6 line piece
    LINE_6(listOf(
        listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0)),  // Horizontal
        listOf(Pair(0, -2), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3)),  // Vertical
        listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0)),  // Horizontal
        listOf(Pair(0, -2), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3))   // Vertical
    )),

    // 18. 3x2 block piece
    BLOCK_3X2(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 0), Pair(0, 1)),
        listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(-1, 0), Pair(0, 0), Pair(1, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1))
    )),

    // 19. Zig-zag piece
    ZIG_ZAG(listOf(
        listOf(Pair(-1, 0), Pair(0, 1), Pair(1, 0)),
        listOf(Pair(0, -1), Pair(-1, 0), Pair(0, 1)),
        listOf(Pair(-1, 0), Pair(0, -1), Pair(1, 0)),
        listOf(Pair(0, -1), Pair(1, 0), Pair(0, 1))
    )),

    // 20. 2x2 + notch top piece
    NOTCH_TOP(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 0)),
        listOf(Pair(-1, -1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(-1, 0)),
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(1, -1)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))
    )),

    // 21. 2x2 + notch bottom piece
    NOTCH_BOTTOM(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(-1, 1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(-1, 0)),
        listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(0, 0), Pair(1, 0)),
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, -1), Pair(1, 0))
    )),

    // 22. 2-line piece
    LINE_2(listOf(
        listOf(Pair(0, 0), Pair(0, 1)),
        listOf(Pair(0, 0), Pair(1, 0)),
        listOf(Pair(0, 0), Pair(0, 1)),
        listOf(Pair(0, 0), Pair(1, 0))
    )),

    // 23. Big T piece
    BIG_T(listOf(
        listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(0, 0), Pair(0, 1)),  // T pointing up
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)),    // + shape (cross)
        listOf(Pair(0, -1), Pair(0, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1)),    // T pointing down
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, 0), Pair(1, 0))   // T pointing left
    )),

    // 24. Short parallel piece
    SHORT_PARALLEL(listOf(
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(1, 0), Pair(1, 1)),  // Horizontal orientation
        listOf(Pair(0, -1), Pair(1, -1), Pair(0, 1), Pair(1, 1)),  // Vertical orientation
        listOf(Pair(-1, 0), Pair(-1, 1), Pair(1, 0), Pair(1, 1)),  // Horizontal orientation
        listOf(Pair(0, -1), Pair(1, -1), Pair(0, 1), Pair(1, 1))   // Vertical orientation
    )),

    // 25. Big L backwards piece
    BIG_L_BACKWARDS(listOf(
        listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(1, 0), Pair(1, 1)),
        listOf(Pair(-1, 1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, -1)),
        listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1)),
        listOf(Pair(-1, 1), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(1, 1))
    )),

    // 26. TO piece
    TO(listOf(
        listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1)),
        listOf(Pair(0, -2), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 0)),
        listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, -1)),
        listOf(Pair(0, -2), Pair(0, -1), Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(-1, 0))
    )),

    // 27. Small L piece
    SMALL_L(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(0, 1)),       // L shape pointing right
        listOf(Pair(0, -1), Pair(0, 0), Pair(1, 0)),       // L shape pointing down
        listOf(Pair(0, -1), Pair(0, 0), Pair(-1, 0)),      // L shape pointing left
        listOf(Pair(-1, 0), Pair(0, 0), Pair(0, 1))        // L shape pointing right (same as state 0)
    )),

    // 28. 3x1 line piece
    LINE_3(listOf(
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0)),  // Horizontal orientation
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1)),  // Vertical orientation
        listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0)),  // Horizontal orientation
        listOf(Pair(0, -1), Pair(0, 0), Pair(0, 1))   // Vertical orientation
    ));

    companion object {
        // Get a random piece type
        fun random(): PieceType {
            return values().random()
        }
    }
}
