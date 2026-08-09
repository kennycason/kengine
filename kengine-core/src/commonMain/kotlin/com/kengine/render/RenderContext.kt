package com.kengine.render

class RenderContext(commandCapacity: Int = 128) {
    private val commands = RenderCommandBuffer(commandCapacity)

    var width: Int = 1
        private set

    var height: Int = 1
        private set

    val commandCount: Int
        get() = commands.count

    val droppedCommandCount: Int
        get() = commands.dropped

    fun beginFrame(width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        commands.reset()
    }

    fun clear(color: Int) {
        commands.clear(color)
    }

    fun fillRect(x: Int, y: Int, width: Int, height: Int, color: Int) {
        commands.fillRect(x, y, width, height, color)
    }

    fun drawLine(startX: Int, startY: Int, endX: Int, endY: Int, color: Int) {
        commands.drawLine(startX, startY, endX, endY, color)
    }

    fun drawTriangle(x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int, color: Int) {
        commands.drawTriangle(x1, y1, x2, y2, x3, y3, color)
    }

    fun drawSprite(
        spriteId: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        tint: Int = WHITE,
        frame: Int = 0
    ) {
        commands.drawSprite(spriteId, x, y, width, height, tint, frame)
    }

    fun drawText(text: String, x: Int, y: Int, color: Int, scale: Int = 2) {
        commands.drawText(text, x, y, color, scale)
    }

    fun verticalGradient(topColor: Int, bottomColor: Int, pulse: Int = 0) {
        commands.verticalGradient(topColor, bottomColor, pulse)
    }

    fun commandField(commandIndex: Int, fieldIndex: Int): Int {
        return commands.field(commandIndex, fieldIndex)
    }

    fun commandText(commandIndex: Int): String {
        return commands.text(commandIndex)
    }

    fun copyCommandsTo(destination: IntArray, maxCommands: Int = destination.size / RenderCommandBuffer.FIELD_COUNT): Int {
        return commands.copyTo(destination, maxCommands)
    }

    companion object {
        const val FIELD_COUNT = RenderCommandBuffer.FIELD_COUNT
        const val WHITE = -1
    }
}
