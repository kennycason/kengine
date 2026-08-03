package com.kengine.audio

interface PortableAudioSink {
    fun render(audio: AudioContext)

    object NoOp : PortableAudioSink {
        override fun render(audio: AudioContext) {
        }
    }
}
