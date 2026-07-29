package com.kengine.render

import kotlin.test.Test
import kotlin.test.assertEquals

class RenderCommandBufferTest {
    @Test
    fun storesCommandFieldsInOrder() {
        val commands = RenderCommandBuffer(capacity = 5)
        val spriteId = RenderAssetId.sprite("demo/pokeball")

        commands.verticalGradient(topColor = 0x11223344, bottomColor = 0x55667788.toInt(), pulse = 9)
        commands.fillRect(x = 10, y = 20, width = 30, height = 40, color = 0x7f00ffee)
        commands.drawLine(startX = 50, startY = 60, endX = 70, endY = 80, color = 0x01020304)
        commands.drawSprite(spriteId = spriteId, x = 90, y = 100, width = 110, height = 120, tint = 0x11121314, frame = 3)
        commands.drawText(text = "KENGINE", x = 130, y = 140, color = 0x21222324, scale = 4)

        assertEquals(5, commands.count)
        assertEquals(0, commands.dropped)
        assertEquals(RenderCommandType.VERTICAL_GRADIENT, commands.field(0, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(0x11223344, commands.field(0, RenderCommandBuffer.FIELD_COLOR))
        assertEquals(0x55667788.toInt(), commands.field(0, RenderCommandBuffer.FIELD_COLOR2))
        assertEquals(9, commands.field(0, RenderCommandBuffer.FIELD_PARAM))
        assertEquals(RenderCommandType.FILL_RECT, commands.field(1, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(10, commands.field(1, RenderCommandBuffer.FIELD_X))
        assertEquals(20, commands.field(1, RenderCommandBuffer.FIELD_Y))
        assertEquals(30, commands.field(1, RenderCommandBuffer.FIELD_WIDTH))
        assertEquals(40, commands.field(1, RenderCommandBuffer.FIELD_HEIGHT))
        assertEquals(0x7f00ffee, commands.field(1, RenderCommandBuffer.FIELD_COLOR))
        assertEquals(RenderCommandType.DRAW_LINE, commands.field(2, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(50, commands.field(2, RenderCommandBuffer.FIELD_X))
        assertEquals(60, commands.field(2, RenderCommandBuffer.FIELD_Y))
        assertEquals(70, commands.field(2, RenderCommandBuffer.FIELD_WIDTH))
        assertEquals(80, commands.field(2, RenderCommandBuffer.FIELD_HEIGHT))
        assertEquals(0x01020304, commands.field(2, RenderCommandBuffer.FIELD_COLOR))
        assertEquals(RenderCommandType.DRAW_SPRITE, commands.field(3, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(90, commands.field(3, RenderCommandBuffer.FIELD_X))
        assertEquals(100, commands.field(3, RenderCommandBuffer.FIELD_Y))
        assertEquals(110, commands.field(3, RenderCommandBuffer.FIELD_WIDTH))
        assertEquals(120, commands.field(3, RenderCommandBuffer.FIELD_HEIGHT))
        assertEquals(0x11121314, commands.field(3, RenderCommandBuffer.FIELD_COLOR))
        assertEquals(spriteId, commands.field(3, RenderCommandBuffer.FIELD_COLOR2))
        assertEquals(3, commands.field(3, RenderCommandBuffer.FIELD_PARAM))
        assertEquals(RenderCommandType.DRAW_TEXT, commands.field(4, RenderCommandBuffer.FIELD_TYPE))
        assertEquals(130, commands.field(4, RenderCommandBuffer.FIELD_X))
        assertEquals(140, commands.field(4, RenderCommandBuffer.FIELD_Y))
        assertEquals(4, commands.field(4, RenderCommandBuffer.FIELD_WIDTH))
        assertEquals(0x21222324, commands.field(4, RenderCommandBuffer.FIELD_COLOR))
        assertEquals("KENGINE", commands.text(4))
    }

    @Test
    fun dropsCommandsBeyondCapacityAndResets() {
        val commands = RenderCommandBuffer(capacity = 1)

        commands.clear(0x12345678)
        commands.fillRect(x = 1, y = 2, width = 3, height = 4, color = 0x90abcdef.toInt())

        assertEquals(1, commands.count)
        assertEquals(1, commands.dropped)

        commands.reset()

        assertEquals(0, commands.count)
        assertEquals(0, commands.dropped)
        assertEquals("", commands.text(0))
    }

    @Test
    fun copiesWholeCommandsToFlatArray() {
        val commands = RenderCommandBuffer(capacity = 3)
        val destination = IntArray(RenderCommandBuffer.FIELD_COUNT * 2)

        commands.fillRect(x = 1, y = 2, width = 3, height = 4, color = 5)
        commands.verticalGradient(topColor = 6, bottomColor = 7, pulse = 8)
        commands.clear(9)

        val copied = commands.copyTo(destination)

        assertEquals(2, copied)
        assertEquals(RenderCommandType.FILL_RECT, destination[0])
        assertEquals(1, destination[1])
        assertEquals(2, destination[2])
        assertEquals(3, destination[3])
        assertEquals(4, destination[4])
        assertEquals(5, destination[5])
        assertEquals(RenderCommandType.VERTICAL_GRADIENT, destination[RenderCommandBuffer.FIELD_COUNT])
        assertEquals(6, destination[RenderCommandBuffer.FIELD_COUNT + RenderCommandBuffer.FIELD_COLOR])
        assertEquals(7, destination[RenderCommandBuffer.FIELD_COUNT + RenderCommandBuffer.FIELD_COLOR2])
        assertEquals(8, destination[RenderCommandBuffer.FIELD_COUNT + RenderCommandBuffer.FIELD_PARAM])
    }
}
