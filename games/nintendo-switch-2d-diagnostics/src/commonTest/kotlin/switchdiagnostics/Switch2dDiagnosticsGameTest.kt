package switchdiagnostics

import com.kengine.audio.AudioCommandBuffer
import com.kengine.audio.AudioCommandType
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderCommandBuffer
import com.kengine.render.RenderCommandType
import com.kengine.render.RenderContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Switch2dDiagnosticsGameTest {
    @Test
    fun firstPageExercisesCore2dRenderCommands() {
        val game = Switch2dDiagnosticsGame()
        val render = RenderContext(commandCapacity = 512)

        render.beginFrame(width = 1280, height = 720)
        game.draw(render)

        assertEquals(0, render.droppedCommandCount)
        assertEquals(RenderCommandType.VERTICAL_GRADIENT, render.commandField(0, RenderCommandBuffer.FIELD_TYPE))
        assertTrue(render.containsText("SWITCH 2D DIAGNOSTICS"))
        assertTrue(render.containsType(RenderCommandType.FILL_RECT))
        assertTrue(render.containsType(RenderCommandType.DRAW_LINE))
        assertTrue(render.containsType(RenderCommandType.DRAW_SPRITE))
        assertTrue(render.containsSprite(DIAGNOSTICS_SPRITES))
        assertTrue(game.snapshot().contains("page=0"))
    }

    @Test
    fun shoulderButtonsCycleDiagnosticsPages() {
        val game = Switch2dDiagnosticsGame()
        val input = InputState()
        val render = RenderContext(commandCapacity = 512)

        tap(game, input, InputButton.R)
        render.beginFrame(width = 1280, height = 720)
        game.draw(render)

        assertTrue(game.snapshot().contains("page=1"))
        assertTrue(render.containsText("TEXT GLYPH COVERAGE"))

        tap(game, input, InputButton.R)
        tap(game, input, InputButton.R)
        render.beginFrame(width = 1280, height = 720)
        game.draw(render)

        assertTrue(game.snapshot().contains("page=3"))
        assertTrue(render.containsText("PERFORMANCE BUDGET"))
    }

    @Test
    fun audioPageQueuesMusicAndDeclaredSoundEffects() {
        val game = Switch2dDiagnosticsGame()
        val input = InputState()
        val audio = AudioContext(commandCapacity = 8)

        tap(game, input, InputButton.R)
        tap(game, input, InputButton.R)
        press(game, input, InputButton.A)
        press(game, input, InputButton.B)
        press(game, input, InputButton.Y)

        audio.beginFrame()
        game.audio(audio)

        assertEquals(4, audio.commandCount)
        assertEquals(AudioCommandType.LOOP_MUSIC, audio.commandField(0, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(AudioCommandType.PLAY_SOUND, audio.commandField(1, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(AudioCommandType.PLAY_SOUND, audio.commandField(2, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(AudioCommandType.PLAY_SOUND, audio.commandField(3, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(0, audio.droppedCommandCount)
    }

    @Test
    fun stressModeCanExposeRenderCommandOverflow() {
        val game = Switch2dDiagnosticsGame()
        val input = InputState()
        val render = RenderContext(commandCapacity = 64)

        press(game, input, InputButton.START)
        render.beginFrame(width = 1280, height = 720)
        game.draw(render)

        assertEquals(64, render.commandCount)
        assertTrue(render.droppedCommandCount > 0)
        assertTrue(game.snapshot().contains("enabled=true"))
    }

    @Test
    fun cleanupStopsMusicOnNextAudioFrame() {
        val game = Switch2dDiagnosticsGame()
        val audio = AudioContext(commandCapacity = 4)

        game.cleanup()
        audio.beginFrame()
        game.audio(audio)

        assertEquals(1, audio.commandCount)
        assertEquals(AudioCommandType.STOP_MUSIC, audio.commandField(0, AudioCommandBuffer.FIELD_TYPE))
        assertTrue(game.snapshot().contains("resets=1"))
    }

    private fun tap(game: Switch2dDiagnosticsGame, input: InputState, button: InputButton) {
        press(game, input, button)
        input.reset()
        game.update(input)
    }

    private fun press(game: Switch2dDiagnosticsGame, input: InputState, button: InputButton) {
        input.reset()
        input.set(button)
        game.update(input)
    }

    private fun RenderContext.containsType(type: Int): Boolean {
        return (0 until commandCount).any { index ->
            commandField(index, RenderCommandBuffer.FIELD_TYPE) == type
        }
    }

    private fun RenderContext.containsText(text: String): Boolean {
        return (0 until commandCount).any { index ->
            commandText(index).contains(text)
        }
    }

    private fun RenderContext.containsSprite(spriteId: String): Boolean {
        val packedSpriteId = RenderAssetId.sprite(spriteId)
        return (0 until commandCount).any { index ->
            commandField(index, RenderCommandBuffer.FIELD_TYPE) == RenderCommandType.DRAW_SPRITE &&
                commandField(index, RenderCommandBuffer.FIELD_COLOR2) == packedSpriteId
        }
    }
}
