data class RubiksNotationMove(
    val token: String,
    val axis: SliceAxis,
    val layers: Set<Int>,
    val direction: Int,
    val quarterTurns: Int
) {
    fun toSliceMoves(): List<SliceMove> {
        return List(quarterTurns) { SliceMove(axis, layers, direction) }
    }
}

class RubiksNotationParseException(message: String) : IllegalArgumentException(message)

object RubiksNotationParser {
    fun parse(sequence: String): List<RubiksNotationMove> {
        val moves = mutableListOf<RubiksNotationMove>()
        var index = 0

        while (index < sequence.length) {
            val char = sequence[index]
            if (char.isWhitespace() || char == ',' || char == ';') {
                index += 1
                continue
            }

            val base = moveFor(char, index)
            var token = char.toString()
            var quarterTurns = 1
            var direction = base.direction

            val suffixIndex = index + 1
            if (suffixIndex < sequence.length) {
                when (sequence[suffixIndex]) {
                    '\'' -> {
                        token += "'"
                        direction = -direction
                        index += 1
                    }
                    '2' -> {
                        token += "2"
                        quarterTurns = 2
                        index += 1
                    }
                }
            }

            moves += RubiksNotationMove(
                token = token,
                axis = base.axis,
                layers = base.layers,
                direction = direction,
                quarterTurns = quarterTurns
            )
            index += 1
        }

        return moves
    }

    private fun moveFor(char: Char, index: Int): MoveDefinition {
        return ALL_MOVES[char]
            ?: throw RubiksNotationParseException("Unsupported BASIC notation '${char}' at index $index.")
    }

    private data class MoveDefinition(
        val axis: SliceAxis,
        val layers: Set<Int>,
        val direction: Int
    )

    private val ALL_LAYERS = setOf(-1, 0, 1)

    private val FACE_MOVES = mapOf(
        'U' to MoveDefinition(SliceAxis.Y, setOf(1), 1),
        'D' to MoveDefinition(SliceAxis.Y, setOf(-1), -1),
        'L' to MoveDefinition(SliceAxis.X, setOf(-1), 1),
        'R' to MoveDefinition(SliceAxis.X, setOf(1), -1),
        'F' to MoveDefinition(SliceAxis.Z, setOf(1), -1),
        'B' to MoveDefinition(SliceAxis.Z, setOf(-1), 1)
    )

    private val EXTRA_MOVES = mapOf(
        'x' to MoveDefinition(SliceAxis.X, ALL_LAYERS, -1),
        'y' to MoveDefinition(SliceAxis.Y, ALL_LAYERS, 1),
        'z' to MoveDefinition(SliceAxis.Z, ALL_LAYERS, -1),
        'M' to MoveDefinition(SliceAxis.X, setOf(0), 1),
        'E' to MoveDefinition(SliceAxis.Y, setOf(0), -1),
        'S' to MoveDefinition(SliceAxis.Z, setOf(0), -1),
        'u' to MoveDefinition(SliceAxis.Y, setOf(0, 1), 1),
        'd' to MoveDefinition(SliceAxis.Y, setOf(-1, 0), -1),
        'l' to MoveDefinition(SliceAxis.X, setOf(-1, 0), 1),
        'r' to MoveDefinition(SliceAxis.X, setOf(0, 1), -1),
        'f' to MoveDefinition(SliceAxis.Z, setOf(0, 1), -1),
        'b' to MoveDefinition(SliceAxis.Z, setOf(-1, 0), 1)
    )

    private val ALL_MOVES = FACE_MOVES + EXTRA_MOVES
}
