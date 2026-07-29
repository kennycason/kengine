package com.kengine.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioCommandBufferTest {
    @Test
    fun storesMusicCommandsInOrder() {
        val commands = AudioCommandBuffer(capacity = 2)
        val musicId = AudioAssetId.music("demo/theme")

        commands.loopMusic(musicId, volume = 300)
        commands.stopMusic(musicId)

        assertEquals(2, commands.count)
        assertEquals(0, commands.dropped)
        assertEquals(AudioCommandType.LOOP_MUSIC, commands.field(0, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(musicId, commands.field(0, AudioCommandBuffer.FIELD_ASSET_ID))
        assertEquals(AudioCommandBuffer.MAX_VOLUME, commands.field(0, AudioCommandBuffer.FIELD_VOLUME))
        assertEquals(AudioCommandType.STOP_MUSIC, commands.field(1, AudioCommandBuffer.FIELD_TYPE))
        assertEquals(musicId, commands.field(1, AudioCommandBuffer.FIELD_ASSET_ID))
    }

    @Test
    fun dropsCommandsBeyondCapacityAndResets() {
        val commands = AudioCommandBuffer(capacity = 1)

        commands.loopMusic(AudioAssetId.music("demo/theme"))
        commands.stopMusic()

        assertEquals(1, commands.count)
        assertEquals(1, commands.dropped)

        commands.reset()

        assertEquals(0, commands.count)
        assertEquals(0, commands.dropped)
    }

    @Test
    fun copiesWholeCommandsToFlatArray() {
        val commands = AudioCommandBuffer(capacity = 2)
        val destination = IntArray(AudioCommandBuffer.FIELD_COUNT)
        val musicId = AudioAssetId.music("demo/theme")

        commands.loopMusic(musicId, volume = 128)
        commands.stopMusic()

        val copied = commands.copyTo(destination, maxCommands = 1)

        assertEquals(1, copied)
        assertEquals(AudioCommandType.LOOP_MUSIC, destination[AudioCommandBuffer.FIELD_TYPE])
        assertEquals(musicId, destination[AudioCommandBuffer.FIELD_ASSET_ID])
        assertEquals(128, destination[AudioCommandBuffer.FIELD_VOLUME])
    }
}
