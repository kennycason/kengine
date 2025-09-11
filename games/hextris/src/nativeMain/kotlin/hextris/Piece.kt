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
    // I piece (line)
    I(listOf(
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4), Pair(0, 5)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(4, 0), Pair(5, 0)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4), Pair(0, 5)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(4, 0), Pair(5, 0))
    )),

    // J piece
    J(listOf(
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4), Pair(-1, 4)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(4, 0), Pair(4, 1)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1), Pair(2, 1), Pair(3, 1), Pair(4, 1))
    )),

    // L piece
    L(listOf(
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4), Pair(1, 4)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(4, 0)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(1, 2), Pair(1, 3), Pair(1, 4)),
        listOf(Pair(0, 1), Pair(1, 1), Pair(2, 1), Pair(3, 1), Pair(4, 1), Pair(4, 0))
    )),

    // O piece (square)
    O(listOf(
        listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1), Pair(0, 2), Pair(1, 2)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1), Pair(1, 1), Pair(2, 1)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1), Pair(0, 2), Pair(1, 2)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1), Pair(1, 1), Pair(2, 1))
    )),

    // S piece
    S(listOf(
        listOf(Pair(1, 0), Pair(2, 0), Pair(0, 1), Pair(1, 1), Pair(0, 2), Pair(-1, 2)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1), Pair(1, 2), Pair(2, 2), Pair(2, 3)),
        listOf(Pair(1, 0), Pair(2, 0), Pair(0, 1), Pair(1, 1), Pair(0, 2), Pair(-1, 2)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1), Pair(1, 2), Pair(2, 2), Pair(2, 3))
    )),

    // Z piece
    Z(listOf(
        listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(2, 1), Pair(2, 2), Pair(3, 2)),
        listOf(Pair(1, 0), Pair(1, 1), Pair(0, 1), Pair(0, 2), Pair(-1, 2), Pair(-1, 3)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(2, 1), Pair(2, 2), Pair(3, 2)),
        listOf(Pair(1, 0), Pair(1, 1), Pair(0, 1), Pair(0, 2), Pair(-1, 2), Pair(-1, 3))
    )),

    // T piece
    T(listOf(
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(-1, 1), Pair(1, 1), Pair(0, 3)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(1, -1), Pair(1, 1), Pair(3, 0)),
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(-1, 1), Pair(1, 1), Pair(0, -1)),
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(1, -1), Pair(1, 1), Pair(-1, 0))
    ));

    companion object {
        // Get a random piece type
        fun random(): PieceType {
            return values().random()
        }
    }
}
