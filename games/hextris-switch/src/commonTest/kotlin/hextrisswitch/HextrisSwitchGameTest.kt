package hextrisswitch

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

class HextrisSwitchGameTest {
    @Test
    fun boardStartsWithPlayablePiecesAndCanLockOne() {
        val board = Board()

        assertFalse(board.gameOver)
        assertNotNull(board.getCurrentPiece())
        assertNotNull(board.getNextPiece())

        board.drop()
        board.lockPiece()

        assertEquals(1, board.totalPieces())
        assertNotNull(board.getCurrentPiece())
    }

    @Test
    fun updateAndDrawUsePortableCommandsWithinBudget() {
        val game = HextrisSwitchGame()
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
        assertTrue(hasText(render, "HEXTRIS"))
        assertTrue(hasCommand(render, RenderCommandType.DRAW_SPRITE))
    }

    @Test
    fun requestsLoopedMusicThroughPortableAudio() {
        val game = HextrisSwitchGame()
        val audio = AudioContext(commandCapacity = 4)

        audio.beginFrame()
        game.audio(audio)

        assertEquals(1, audio.commandCount)
        assertEquals(0, audio.droppedCommandCount)
        assertEquals(AudioCommandType.LOOP_MUSIC, audio.commandField(0, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(
            AudioAssetId.music(Sprites.MUSIC_ID),
            audio.commandField(0, AudioCommandBuffer.FIELD_ASSET_ID)
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
}
