package com.kengine.render

import kotlin.test.Test
import kotlin.test.assertEquals

class RenderContextTest {
    @Test
    fun beginFrameStoresDimensionsAndClearsPreviousCommands() {
        val render = RenderContext(commandCapacity = 1)

        render.beginFrame(width = 320, height = 240)
        render.clear(0x12345678)
        render.fillRect(x = 1, y = 2, width = 3, height = 4, color = 5)

        assertEquals(320, render.width)
        assertEquals(240, render.height)
        assertEquals(1, render.commandCount)
        assertEquals(1, render.droppedCommandCount)

        render.beginFrame(width = 0, height = -1)

        assertEquals(1, render.width)
        assertEquals(1, render.height)
        assertEquals(0, render.commandCount)
        assertEquals(0, render.droppedCommandCount)
    }

    @Test
    fun recordsDrawCommandsThroughContext() {
        val render = RenderContext(commandCapacity = 5)
        val spriteId = RenderAssetId.sprite("demo/pokeball")

        render.beginFrame(width = 640, height = 480)
        render.verticalGradient(topColor = 0x01020304, bottomColor = 0x11121314, pulse = 17)
        render.fillRect(x = 10, y = 20, width = 30, height = 40, color = 0x21222324)
        render.drawLine(startX = 50, startY = 60, endX = 70, endY = 80, color = 0x31323334)
        render.drawSprite(spriteId = spriteId, x = 90, y = 100, width = 110, height = 120, tint = 0x41424344, frame = 5)
        render.drawText(text = "SCORE 123", x = 130, y = 140, color = 0x51525354, scale = 3)

        assertEquals(5, render.commandCount)
        assertEquals(RenderCommandType.VERTICAL_GRADIENT, render.commandField(0, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(0x01020304, render.commandField(0, RenderCommandBuffer.FIELD_COLOR))
        assertEquals(0x11121314, render.commandField(0, RenderCommandBuffer.FIELD_COLOR2))
        assertEquals(17, render.commandField(0, RenderCommandBuffer.FIELD_PARAM))
        assertEquals(RenderCommandType.FILL_RECT, render.commandField(1, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(10, render.commandField(1, RenderCommandBuffer.FIELD_X))
        assertEquals(20, render.commandField(1, RenderCommandBuffer.FIELD_Y))
        assertEquals(30, render.commandField(1, RenderCommandBuffer.FIELD_WIDTH))
        assertEquals(40, render.commandField(1, RenderCommandBuffer.FIELD_HEIGHT))
        assertEquals(0x21222324, render.commandField(1, RenderCommandBuffer.FIELD_COLOR))
        assertEquals(RenderCommandType.DRAW_LINE, render.commandField(2, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(50, render.commandField(2, RenderCommandBuffer.FIELD_X))
        assertEquals(60, render.commandField(2, RenderCommandBuffer.FIELD_Y))
        assertEquals(70, render.commandField(2, RenderCommandBuffer.FIELD_WIDTH))
        assertEquals(80, render.commandField(2, RenderCommandBuffer.FIELD_HEIGHT))
        assertEquals(0x31323334, render.commandField(2, RenderCommandBuffer.FIELD_COLOR))
        assertEquals(RenderCommandType.DRAW_SPRITE, render.commandField(3, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(90, render.commandField(3, RenderCommandBuffer.FIELD_X))
        assertEquals(100, render.commandField(3, RenderCommandBuffer.FIELD_Y))
        assertEquals(110, render.commandField(3, RenderCommandBuffer.FIELD_WIDTH))
        assertEquals(120, render.commandField(3, RenderCommandBuffer.FIELD_HEIGHT))
        assertEquals(0x41424344, render.commandField(3, RenderCommandBuffer.FIELD_COLOR))
        assertEquals(spriteId, render.commandField(3, RenderCommandBuffer.FIELD_COLOR2))
        assertEquals(5, render.commandField(3, RenderCommandBuffer.FIELD_PARAM))
        assertEquals(RenderCommandType.DRAW_TEXT, render.commandField(4, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(130, render.commandField(4, RenderCommandBuffer.FIELD_X))
        assertEquals(140, render.commandField(4, RenderCommandBuffer.FIELD_Y))
        assertEquals(3, render.commandField(4, RenderCommandBuffer.FIELD_WIDTH))
        assertEquals(0x51525354, render.commandField(4, RenderCommandBuffer.FIELD_COLOR))
        assertEquals("SCORE 123", render.commandText(4))
    }

    @Test
    fun copiesCommandsThroughContext() {
        val render = RenderContext(commandCapacity = 2)
        val destination = IntArray(RenderContext.FIELD_COUNT)

        render.beginFrame(width = 640, height = 480)
        render.fillRect(x = 1, y = 2, width = 3, height = 4, color = 5)
        render.clear(6)

        val copied = render.copyCommandsTo(destination, maxCommands = 1)

        assertEquals(1, copied)
        assertEquals(RenderCommandType.FILL_RECT, destination[RenderCommandBuffer.FIELD_TYPE])
        assertEquals(1, destination[RenderCommandBuffer.FIELD_X])
        assertEquals(2, destination[RenderCommandBuffer.FIELD_Y])
        assertEquals(3, destination[RenderCommandBuffer.FIELD_WIDTH])
        assertEquals(4, destination[RenderCommandBuffer.FIELD_HEIGHT])
        assertEquals(5, destination[RenderCommandBuffer.FIELD_COLOR])
    }
}
