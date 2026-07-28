import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RubiksNotationParserTest {
    @Test
    fun `parses compact basic notation`() {
        val moves = RubiksNotationParser.parse("UU'D'D")

        assertEquals(listOf("U", "U'", "D'", "D"), moves.map { it.token })
        assertEquals(listOf(1, -1, 1, -1), moves.map { it.direction })
        assertEquals(listOf(setOf(1), setOf(1), setOf(-1), setOf(-1)), moves.map { it.layers })
    }

    @Test
    fun `parses spaced double-turn checkerboard notation`() {
        val moves = RubiksNotationParser.parse("F2 B2 U2 D2 L2 R2")

        assertEquals(listOf("F2", "B2", "U2", "D2", "L2", "R2"), moves.map { it.token })
        assertEquals(List(6) { 2 }, moves.map { it.quarterTurns })
        assertEquals(12, moves.flatMap { it.toSliceMoves() }.size)
    }

    @Test
    fun `basic notation parses whole cube rotations`() {
        val moves = RubiksNotationParser.parse("x y' z2")

        assertEquals(SliceAxis.X, moves[0].axis)
        assertEquals(setOf(-1, 0, 1), moves[0].layers)
        assertEquals(-1, moves[0].direction)
        assertEquals(SliceAxis.Y, moves[1].axis)
        assertEquals(setOf(-1, 0, 1), moves[1].layers)
        assertEquals(-1, moves[1].direction)
        assertEquals(SliceAxis.Z, moves[2].axis)
        assertEquals(setOf(-1, 0, 1), moves[2].layers)
        assertEquals(2, moves[2].quarterTurns)
    }

    @Test
    fun `basic notation parses middle and wide turns`() {
        val moves = RubiksNotationParser.parse("M E' S2 u r' f")

        assertEquals(setOf(0), moves[0].layers)
        assertEquals(SliceAxis.X, moves[0].axis)
        assertEquals(1, moves[0].direction)
        assertEquals(setOf(0), moves[1].layers)
        assertEquals(SliceAxis.Y, moves[1].axis)
        assertEquals(1, moves[1].direction)
        assertEquals(2, moves[2].quarterTurns)
        assertEquals(setOf(0, 1), moves[3].layers)
        assertEquals(SliceAxis.Y, moves[3].axis)
        assertEquals(setOf(0, 1), moves[4].layers)
        assertEquals(SliceAxis.X, moves[4].axis)
        assertEquals(1, moves[4].direction)
        assertEquals(setOf(0, 1), moves[5].layers)
        assertEquals(SliceAxis.Z, moves[5].axis)
    }

    @Test
    fun `basic notation rejects unsupported tokens`() {
        assertFailsWith<RubiksNotationParseException> {
            RubiksNotationParser.parse("R U Q")
        }
    }
}
