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

    fun verticalGradient(topColor: Int, bottomColor: Int, pulse: Int = 0) {
        commands.verticalGradient(topColor, bottomColor, pulse)
    }

    fun commandField(commandIndex: Int, fieldIndex: Int): Int {
        return commands.field(commandIndex, fieldIndex)
    }

    fun copyCommandsTo(destination: IntArray, maxCommands: Int = destination.size / RenderCommandBuffer.FIELD_COUNT): Int {
        return commands.copyTo(destination, maxCommands)
    }

    companion object {
        const val FIELD_COUNT = RenderCommandBuffer.FIELD_COUNT
    }
}
