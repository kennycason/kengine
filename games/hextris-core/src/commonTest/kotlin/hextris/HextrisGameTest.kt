package hextris

import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioCommandBuffer
import com.kengine.audio.AudioCommandType
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderCommandBuffer
import com.kengine.render.RenderCommandType
import com.kengine.render.RenderContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HextrisGameTest {
    @Test
    fun boardStartsWithPlayablePiecesAndCanLockOne() {
        val board = Board()

        assertFalse(board.gameOver)
        assertNotNull(board.getCurrentPiece())
        assertNotNull(board.getNextPiece())
        assertNotNull(board.getNextNextPiece())

        board.drop()
        board.lockPiece()

        assertEquals(1, board.totalPieces())
        assertNotNull(board.getCurrentPiece())
        assertNotNull(board.getNextPiece())
        assertNotNull(board.getNextNextPiece())
    }

    @Test
    fun boardReportsCurrentDropDistanceWithoutMovingPiece() {
        val board = Board()
        val startPosition = board.getCurrentPiecePosition()
        val distance = board.currentDropDistance()

        assertTrue(distance > 0)
        assertEquals(startPosition, board.getCurrentPiecePosition())
        assertEquals(distance, board.drop())
    }

    @Test
    fun updateAndDrawUsePortableCommandsWithinBudget() {
        val game = HextrisGame()
        val input = InputState()
        val render = RenderContext(commandCapacity = 1024)

        input.set(InputButton.LEFT)
        input.set(InputButton.A)
        game.update(input)
        input.reset()
        game.update(input)

        render.beginFrame(width = 1280, height = 720)
        game.draw(render)

        assertTrue(render.commandCount in 60..1024)
        assertEquals(0, render.droppedCommandCount)
        assertEquals(RenderCommandType.VERTICAL_GRADIENT, render.commandField(0, RenderCommandBuffer.FIELD_TYPE))
        assertFalse(hasText(render, "HEXTRIS"))
        assertTrue(hasText(render, "NEXT"))
        assertTrue(hasText(render, "CONTROLS"))
        assertFalse(hasText(render, "STATS"))
        assertTrue(hasCommand(render, RenderCommandType.DRAW_SPRITE))
    }

    @Test
    fun drawIncludesLandingTargetForActivePieceWithoutColumnGuide() {
        val game = HextrisGame()
        val render = RenderContext(commandCapacity = 1024)

        render.beginFrame(width = 1280, height = 720)
        game.draw(render)

        assertFalse(hasFillRectColor(render, rgba(114, 202, 220, 70)))
        assertTrue(hasFillRectColor(render, rgba(54, 63, 82, 125)))
    }

    @Test
    fun requestsLoopedMusicThroughPortableAudio() {
        val game = HextrisGame()
        val audio = AudioContext(commandCapacity = 4)

        audio.beginFrame()
        game.audio(audio)

        assertEquals(1, audio.commandCount)
        assertEquals(0, audio.droppedCommandCount)
        assertEquals(AudioCommandType.LOOP_MUSIC, audio.commandField(0, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(
            AudioAssetId.music(Sounds.MUSIC),
            audio.commandField(0, AudioCommandBuffer.FIELD_ASSET_ID)
        )
    }

    @Test
    fun requestsSoundEffectAfterSuccessfulRotation() {
        val game = HextrisGame()
        val input = InputState()
        val audio = AudioContext(commandCapacity = 4)

        input.set(InputButton.A)
        game.update(input)
        audio.beginFrame()
        game.audio(audio)

        assertEquals(2, audio.commandCount)
        assertEquals(AudioCommandType.LOOP_MUSIC, audio.commandField(0, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(AudioCommandType.PLAY_SOUND, audio.commandField(1, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(
            AudioAssetId.sound(Sounds.ROTATE),
            audio.commandField(1, AudioCommandBuffer.FIELD_ASSET_ID)
        )
    }

    private fun hasText(render: RenderContext, text: String): Boolean {
        for (index in 0 until render.commandCount) {
            if (render.commandText(index) == text) {
                return true
            }
        }
        return false
    }

    private fun hasCommand(render: RenderContext, type: Int): Boolean {
        for (index in 0 until render.commandCount) {
            if (render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == type) {
                return true
            }
        }
        return false
    }

    private fun hasFillRectColor(render: RenderContext, color: Int): Boolean {
        for (index in 0 until render.commandCount) {
            if (
                render.commandField(index, RenderCommandBuffer.FIELD_TYPE) == RenderCommandType.FILL_RECT &&
                render.commandField(index, RenderCommandBuffer.FIELD_COLOR) == color
            ) {
                return true
            }
        }
        return false
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return r.coerceIn(0, 255) or
            (g.coerceIn(0, 255) shl 8) or
            (b.coerceIn(0, 255) shl 16) or
            (a.coerceIn(0, 255) shl 24)
    }
}
