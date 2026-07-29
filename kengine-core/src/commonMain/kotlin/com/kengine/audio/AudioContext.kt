package com.kengine.audio

class AudioContext(commandCapacity: Int = 32) {
    private val commands = AudioCommandBuffer(commandCapacity)

    val commandCount: Int
        get() = commands.count

    val droppedCommandCount: Int
        get() = commands.dropped

    fun beginFrame() {
        commands.reset()
    }

    fun loopMusic(assetId: Int, volume: Int = AudioCommandBuffer.MAX_VOLUME) {
        commands.loopMusic(assetId, volume)
    }

    fun stopMusic(assetId: Int = 0) {
        commands.stopMusic(assetId)
    }

    fun playSound(assetId: Int, volume: Int = AudioCommandBuffer.MAX_VOLUME) {
        commands.playSound(assetId, volume)
    }

    fun commandField(commandIndex: Int, fieldIndex: Int): Int {
        return commands.field(commandIndex, fieldIndex)
    }

    fun copyCommandsTo(destination: IntArray, maxCommands: Int = destination.size / AudioCommandBuffer.FIELD_COUNT): Int {
        return commands.copyTo(destination, maxCommands)
    }

    companion object {
        const val FIELD_COUNT = AudioCommandBuffer.FIELD_COUNT
        const val MAX_VOLUME = AudioCommandBuffer.MAX_VOLUME
    }
}
