package com.kengine.render

class RenderCommandBuffer(capacity: Int = 128) {
    private val commandCapacity = capacity.coerceAtLeast(1)
    private val values = IntArray(commandCapacity * FIELD_COUNT)

    var count: Int = 0
        private set

    var dropped: Int = 0
        private set

    fun reset() {
        count = 0
        dropped = 0
    }

    fun clear(color: Int) {
        add(RenderCommandType.CLEAR, 0, 0, 0, 0, color, 0, 0)
    }

    fun fillRect(x: Int, y: Int, width: Int, height: Int, color: Int) {
        if (width <= 0 || height <= 0) return
        add(RenderCommandType.FILL_RECT, x, y, width, height, color, 0, 0)
    }

    fun verticalGradient(topColor: Int, bottomColor: Int, pulse: Int = 0) {
        add(RenderCommandType.VERTICAL_GRADIENT, 0, 0, 0, 0, topColor, bottomColor, pulse)
    }

    fun field(commandIndex: Int, fieldIndex: Int): Int {
        if (commandIndex !in 0 until count || fieldIndex !in 0 until FIELD_COUNT) {
            return 0
        }
        return values[commandIndex * FIELD_COUNT + fieldIndex]
    }

    fun copyTo(destination: IntArray, maxCommands: Int = destination.size / FIELD_COUNT): Int {
        val commandLimit = minOf(count, maxCommands, destination.size / FIELD_COUNT)
        if (commandLimit <= 0) {
            return 0
        }

        val valueLimit = commandLimit * FIELD_COUNT
        var index = 0
        while (index < valueLimit) {
            destination[index] = values[index]
            index += 1
        }
        return commandLimit
    }

    private fun add(type: Int, x: Int, y: Int, width: Int, height: Int, color: Int, color2: Int, param: Int) {
        if (count >= commandCapacity) {
            dropped += 1
            return
        }

        val offset = count * FIELD_COUNT
        values[offset + FIELD_TYPE] = type
        values[offset + FIELD_X] = x
        values[offset + FIELD_Y] = y
        values[offset + FIELD_WIDTH] = width
        values[offset + FIELD_HEIGHT] = height
        values[offset + FIELD_COLOR] = color
        values[offset + FIELD_COLOR2] = color2
        values[offset + FIELD_PARAM] = param
        count += 1
    }

    companion object {
        const val FIELD_COUNT = 8
        const val FIELD_TYPE = 0
        const val FIELD_X = 1
        const val FIELD_Y = 2
        const val FIELD_WIDTH = 3
        const val FIELD_HEIGHT = 4
        const val FIELD_COLOR = 5
        const val FIELD_COLOR2 = 6
        const val FIELD_PARAM = 7
    }
}
