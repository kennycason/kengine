package com.kengine.sound.synth

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.toCValues
import sdl3.SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK
import sdl3.SDL_AUDIO_F32
import sdl3.SDL_AudioSpec
import sdl3.SDL_ClearAudioStream
import sdl3.SDL_DestroyAudioStream
import sdl3.SDL_GetAudioStreamAvailable
import sdl3.SDL_GetError
import sdl3.SDL_OpenAudioDeviceStream
import sdl3.SDL_PauseAudioStreamDevice
import sdl3.SDL_PutAudioStreamData
import sdl3.SDL_ResumeAudioStreamDevice

@OptIn(ExperimentalForeignApi::class)
class AudioStream(
    private var frequency: Double = 440.0,
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 512
) {
    private var oscillator = Oscillator(frequency, sampleRate)
    private val buffer = FloatArray(bufferSize)

    // audio stream and device setup
    private val stream: CPointer<cnames.structs.SDL_AudioStream>

    init {
        // create audio specs
        val srcSpec = cValue<SDL_AudioSpec> {
            freq = sampleRate
            format = SDL_AUDIO_F32
            channels = 1
        }

        // let SDL choose the best output format
        stream = SDL_OpenAudioDeviceStream(
            SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK,
            srcSpec,
            null, // No callback needed
            null  // No userdata needed
        ) ?: error("Failed to open audio stream: ${SDL_GetError()}")

        // start playback
        SDL_ResumeAudioStreamDevice(stream)
    }

    fun setFrequency(newFrequency: Double) {
        frequency = newFrequency
        oscillator.setFrequency(frequency)
    }

    fun update() {
        // check if we need to add more data
        if (SDL_GetAudioStreamAvailable(stream) < bufferSize * Float.SIZE_BYTES) {
            // Fill the buffer with new samples
            for (i in buffer.indices) {
                buffer[i] = oscillator.nextSample()
            }

            // put the data into the stream
            if (!SDL_PutAudioStreamData(stream, buffer.toCValues(), buffer.size * Float.SIZE_BYTES)) {
                error("Failed to put audio data: ${SDL_GetError()}")
            }
        }
    }

    fun pause() {
        SDL_PauseAudioStreamDevice(stream)
    }

    fun resume() {
        SDL_ResumeAudioStreamDevice(stream)
    }

    fun clear() {
        SDL_ClearAudioStream(stream)
    }

    fun cleanup() {
        SDL_DestroyAudioStream(stream)
    }

}
