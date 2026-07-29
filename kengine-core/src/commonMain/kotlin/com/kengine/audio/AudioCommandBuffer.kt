package com.kengine.audio

class AudioCommandBuffer(capacity: Int = 32) {
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

    fun loopMusic(assetId: Int, volume: Int = MAX_VOLUME) {
        if (assetId == 0) return
        add(AudioCommandType.LOOP_MUSIC, assetId, volume.coerceIn(0, MAX_VOLUME), 0)
    }

    fun stopMusic(assetId: Int = 0) {
        add(AudioCommandType.STOP_MUSIC, assetId, 0, 0)
    }

    fun playSound(assetId: Int, volume: Int = MAX_VOLUME) {
        if (assetId == 0) return
        add(AudioCommandType.PLAY_SOUND, assetId, volume.coerceIn(0, MAX_VOLUME), 0)
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

    private fun add(type: Int, assetId: Int, volume: Int, param: Int) {
        if (count >= commandCapacity) {
            dropped += 1
            return
        }

        val offset = count * FIELD_COUNT
        values[offset + FIELD_TYPE] = type
        values[offset + FIELD_ASSET_ID] = assetId
        values[offset + FIELD_VOLUME] = volume
        values[offset + FIELD_PARAM] = param
        count += 1
    }

    companion object {
        const val FIELD_COUNT = 4
        const val FIELD_TYPE = 0
        const val FIELD_ASSET_ID = 1
        const val FIELD_VOLUME = 2
        const val FIELD_PARAM = 3
        const val MAX_VOLUME = 255
    }
}
