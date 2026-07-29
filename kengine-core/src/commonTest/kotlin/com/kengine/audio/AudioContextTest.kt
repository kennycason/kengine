package com.kengine.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioContextTest {
    @Test
    fun beginFrameClearsPreviousCommands() {
        val audio = AudioContext(commandCapacity = 1)

        audio.loopMusic(AudioAssetId.music("demo/theme"))
        audio.stopMusic()

        assertEquals(1, audio.commandCount)
        assertEquals(1, audio.droppedCommandCount)

        audio.beginFrame()

        assertEquals(0, audio.commandCount)
        assertEquals(0, audio.droppedCommandCount)
    }
}
