package com.kengine

import com.kengine.audio.AudioContext
import com.kengine.input.InputState
import com.kengine.render.RenderContext

interface PortableGame {
    fun update(input: InputState)
    fun audio(audio: AudioContext) {
    }
    fun draw(render: RenderContext)
    fun cleanup()
}
