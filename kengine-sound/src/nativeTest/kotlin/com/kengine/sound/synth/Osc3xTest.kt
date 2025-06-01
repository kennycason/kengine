package com.kengine.sound.synth

import com.kengine.sdl.SDLContext
import com.kengine.sound.SoundContext
import com.kengine.test.expectThat
import kotlin.test.Test

class Osc3xTest {
    @Test
    fun `detune applies correctly`() {
        SDLContext.create("Osc3x Test", width = 10, height = 10)
        SoundContext.get()

        val osc3x = Osc3x()

        osc3x.setConfig(0, frequency = 440.0, detune = 100.0) // +1 semitone
        expectThat(osc3x.getConfig(0).frequency).isEqualTo(440.0)
        expectThat(osc3x.getConfig(0).detune).isEqualTo(100.0)

        osc3x.setConfig(0, detune = -100.0) // -1 semitone
        expectThat(osc3x.getConfig(0).detune).isEqualTo(-100.0)
    }
}
