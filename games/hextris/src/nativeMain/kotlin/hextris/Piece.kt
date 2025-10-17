package hextris

import com.kengine.math.IntVec2

/**
 * Represents a Hextris piece.
 * Uses mathematical rotation instead of pre-defined states.
 */
class Piece(val type: PieceType, val color: Int) {
    // Base shape (unrotated)
    private val baseShape: List<IntVec2> = type.shape.map { IntVec2(it.first, it.second) }
    
    // Current rotation state (0-3)
    var rotation: Int = 0
        private set

    // Get the blocks for the current rotation
    fun getBlocks(): List<IntVec2> {
        return when (rotation) {
            0 -> baseShape // 0°: no rotation
            1 -> baseShape.map { IntVec2(-it.y, it.x) } // 90° clockwise: (x,y) → (-y,x)
            2 -> baseShape.map { IntVec2(-it.x, -it.y) } // 180°: (x,y) → (-x,-y)
            3 -> baseShape.map { IntVec2(it.y, -it.x) } // 270° clockwise (90° counter): (x,y) → (y,-x)
            else -> baseShape
        }
    }

    // Rotate the piece clockwise
    fun rotateClockwise() {
        rotation = (rotation + 1) % 4
    }

    // Rotate the piece counter-clockwise
    fun rotateCounterClockwise() {
        rotation = (rotation + 3) % 4 // Same as (rotation - 1 + 4) % 4
    }

    // Create a copy of this piece
    fun copy(): Piece {
        return Piece(type, color).also { it.rotation = this.rotation }
    }
}

/**
 * Defines the different types of Hextris pieces.
 * Each piece type stores only its base shape - rotation is handled mathematically.
 */
enum class PieceType(val shape: List<Pair<Int, Int>>) {
    // 1. Square (O piece)
    O(listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1))),

    // 2. L piece
    L(listOf(Pair(-1, 1), Pair(-1, 0), Pair(0, 0), Pair(1, 0))),

    // 3. J piece (L-backwards)
    J(listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(1, 1))),

    // 4. I piece (line)
    I(listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0))),

    // 5. S piece
    S(listOf(Pair(1, 0), Pair(0, 0), Pair(0, 1), Pair(-1, 1))),

    // 6. Z piece
    Z(listOf(Pair(1, 1), Pair(0, 1), Pair(0, 0), Pair(-1, 0))),

    // 7. T piece
    T(listOf(Pair(0, 1), Pair(-1, 0), Pair(0, 0), Pair(1, 0))),

    // 8. Dot piece
    DOT(listOf(Pair(0, 0))),

    // 9. Screw piece
    SCREW(listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(1, 1))),

    // 10. Screw backwards piece
    SCREW_BACKWARDS(listOf(Pair(1, -1), Pair(1, 0), Pair(0, 0), Pair(-1, 0), Pair(-1, 1))),

    // 11. Long cross piece (symmetric - looks same at all rotations)
    LONG_CROSS(listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))),

    // 12. Cross piece
    CROSS(listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1), Pair(2, 0))),

    // 13. Layers piece
    LAYERS(listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(-1, 1), Pair(0, 1), Pair(1, 1))),

    // 14. Y piece
    Y(listOf(Pair(-1, -1), Pair(-1, 0), Pair(0, 0), Pair(0, 1), Pair(1, -1), Pair(1, 0))),

    // 15. U piece
    U(listOf(Pair(-1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 0), Pair(1, 1))),

    // 16. 5 line piece
    LINE_5(listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0))),

    // 17. 6 line piece
    LINE_6(listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0))),

    // 18. 3x2 block piece
    BLOCK_3X2(listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1))),

    // 19. Zig-zag piece
    ZIG_ZAG(listOf(Pair(-1, 0), Pair(0, 1), Pair(1, 0))),

    // 20. 2x2 + notch top piece
    NOTCH_TOP(listOf(Pair(-1, 0), Pair(0, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 0))),

    // 21. 2x2 + notch bottom piece
    NOTCH_BOTTOM(listOf(Pair(-1, 0), Pair(0, 0), Pair(-1, 1), Pair(0, 1), Pair(1, 1))),

    // 22. 2-line piece
    LINE_2(listOf(Pair(0, 0), Pair(0, 1))),

    // 23. Big T piece
    BIG_T(listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(0, 0), Pair(0, 1))),

    // 24. Short parallel piece
    SHORT_PARALLEL(listOf(Pair(-1, 0), Pair(-1, 1), Pair(1, 0), Pair(1, 1))),

    // 25. Big L backwards piece
    BIG_L_BACKWARDS(listOf(Pair(-1, -1), Pair(0, -1), Pair(1, -1), Pair(1, 0), Pair(1, 1))),

    // 26. TO piece
    TO(listOf(Pair(-2, 0), Pair(-1, 0), Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1))),

    // 27. Small L piece
    SMALL_L(listOf(Pair(-1, 0), Pair(0, 0), Pair(0, 1))),

    // 28. 3x1 line piece
    LINE_3(listOf(Pair(-1, 0), Pair(0, 0), Pair(1, 0)));

    companion object {
        // Get a random piece type
        fun random(): PieceType {
            return values().random()
        }
    }
}
