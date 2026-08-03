package nintendoswitchdemo

import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderCommandBuffer
import com.kengine.render.RenderCommandType
import com.kengine.render.RenderContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NintendoSwitchDemoGameTest {
    @Test
    fun updateAndDrawProduceCommands() {
        val game = NintendoSwitchDemoGame()
        val input = InputState()
        val render = RenderContext(commandCapacity = 32)

        input.set(InputButton.RIGHT)
        input.set(InputButton.A)
        game.update(input)
        render.beginFrame(width = 1280, height = 720)
        game.draw(render)

        assertEquals(26, render.commandCount)
        assertEquals(0, render.droppedCommandCount)
        assertEquals(RenderCommandType.VERTICAL_GRADIENT, render.commandField(0, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(RenderCommandType.DRAW_SPRITE, render.commandField(16, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(RenderAssetId.sprite(DEMO_POKEBALL_SPRITE), render.commandField(16, RenderCommandBuffer.FIELD_COLOR2))
        assertEquals(RenderCommandType.DRAW_SPRITE, render.commandField(20, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(RenderAssetId.sprite(DEMO_BLOCK_SPRITES), render.commandField(20, RenderCommandBuffer.FIELD_COLOR2))
        assertEquals(RenderCommandType.DRAW_LINE, render.commandField(21, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(RenderCommandType.DRAW_LINE, render.commandField(22, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(RenderCommandType.DRAW_TEXT, render.commandField(23, RenderCommandBuffer.FIELD_TYPE))
        assertEquals("KENGINE SWITCH", render.commandText(23))
        assertEquals(RenderCommandType.DRAW_TEXT, render.commandField(24, RenderCommandBuffer.FIELD_TYPE))
        assertTrue(render.commandText(24).contains("L/R SPEED"))
        assertEquals(RenderCommandType.DRAW_TEXT, render.commandField(25, RenderCommandBuffer.FIELD_TYPE))
        assertTrue(render.commandText(25).contains("U:1 D:1"))
        assertTrue(game.snapshot().contains("updates=1 draws=1"))
    }
}
