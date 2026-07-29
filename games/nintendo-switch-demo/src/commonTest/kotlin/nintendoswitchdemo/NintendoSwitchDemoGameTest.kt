package nintendoswitchdemo

import com.kengine.input.InputButton
import com.kengine.input.InputState
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

        assertEquals(19, render.commandCount)
        assertEquals(0, render.droppedCommandCount)
        assertEquals(RenderCommandType.VERTICAL_GRADIENT, render.commandField(0, RenderCommandBuffer.FIELD_TYPE))
        assertTrue(game.snapshot().contains("updates=1 draws=1"))
    }
}
