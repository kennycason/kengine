package com.kengine.sound.synth

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.toCValues
import platform.posix.pow
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
class Osc3x(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 512
) {

    data class OscillatorConfig(
        var enabled: Boolean = true,
        var frequency: Double = 440.0,
        var waveform: Oscillator.Waveform = Oscillator.Waveform.SINE,
        var detune: Double = 0.0,
        val adsr: ADSR = ADSR(),
        val lfo: LFO = LFO(),
        val filter: Filter = Filter(),
        var volume: Double = 1.0
    )

    private val configs = listOf(
        OscillatorConfig(),  // Osc 1
        OscillatorConfig(),  // Osc 2
        OscillatorConfig()   // Osc 3
    )

    val oscillators = configs.map {
        Oscillator(it.frequency, sampleRate, it.waveform, it.detune)
    }

    private var masterVolume = 1.0 // Global volume control
        get

    private val buffer = FloatArray(bufferSize)
    private val stream: CPointer<cnames.structs.SDL_AudioStream>

    init {
        val srcSpec = cValue<SDL_AudioSpec> {
            freq = sampleRate
            format = SDL_AUDIO_F32
            channels = 1
        }

        stream = SDL_OpenAudioDeviceStream(
            SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK,
            srcSpec,
            null,
            null
        ) ?: error("Failed to open audio stream: ${SDL_GetError()}")

        SDL_ResumeAudioStreamDevice(stream)
    }

    // master volume control
    fun setMasterVolume(volume: Double) {
        masterVolume = volume.coerceIn(0.0, 1.0)
    }

    fun setConfig(
        enabled: Boolean? = null,
        frequency: Double? = null,
        waveform: Oscillator.Waveform? = null,
        detune: Double? = null,
        volume: Double? = null
    ) {
        for (i in oscillators.indices) {
            setConfig(
                oscillator = i,
                enabled = enabled,
                frequency = frequency,
                waveform = waveform,
                detune = detune,
                volume = volume
            )
        }
    }

    // per-oscillator configuration
    fun setConfig(
        oscillator: Int,
        enabled: Boolean? = null,
        frequency: Double? = null,
        waveform: Oscillator.Waveform? = null,
        detune: Double? = null,
        volume: Double? = null
    ) {
        if (oscillator !in configs.indices) return // ignore invalid oscillator index

        val config = configs[oscillator]
        val osc = oscillators[oscillator]

        // merge the provided parameters with the current configuration
        enabled?.let { config.enabled = it }
        frequency?.let { config.frequency = it }
        waveform?.let { config.waveform = it }
        detune?.let { config.detune = it }
        volume?.let {
            config.volume = it.coerceIn(0.0, 1.0)
            config.enabled = config.volume > 0.0
        }

        // calculate frequency with detune (in cents)
        val detunedFrequency = config.frequency * pow(2.0, config.detune / 1200.0)

        // apply the updates to the oscillator
        osc.setFrequency(detunedFrequency)
        osc.setWaveform(config.waveform)
    }

    // Individual setter functions for each parameter
    fun setEnabled(index: Int, enabled: Boolean) {
        if (index !in configs.indices) return
        configs[index].enabled = enabled
    }

    fun setFrequency(index: Int, frequency: Double) {
        if (index !in configs.indices) return
        val config = configs[index]
        config.frequency = frequency.coerceIn(20.0, 20000.0)
        updateOscillator(index)
    }

    fun setWaveform(index: Int, waveform: Oscillator.Waveform) {
        if (index !in configs.indices) return
        configs[index].waveform = waveform
        oscillators[index].setWaveform(waveform)
    }

    fun setDetune(index: Int, detune: Double) {
        if (index !in configs.indices) return
        configs[index].detune = detune
        updateOscillator(index)
    }

    fun setVolume(index: Int, volume: Double) {
        if (index !in configs.indices) return
        configs[index].volume = volume.coerceIn(0.0, 1.0)
    }

    // Update oscillator frequency with detune
    private fun updateOscillator(index: Int) {
        val config = configs[index]
        val detunedFrequency = config.frequency * pow(2.0, config.detune / 1200.0)
        oscillators[index].setFrequency(detunedFrequency)
    }

    fun getConfig(oscillator: Int): OscillatorConfig = configs[oscillator]

    // update the audio stream
    fun update() {
        if (SDL_GetAudioStreamAvailable(stream) < bufferSize * Float.SIZE_BYTES) {
            for (i in buffer.indices) {
                var sample = 0.0

                // Mix enabled oscillators
                for (index in oscillators.indices) {
                    if (configs[index].enabled) {
                        oscillators[index].trigger()
                        sample += oscillators[index].nextSample() * configs[index].volume
                    } else {
                        oscillators[index].release()
                    }
                }

                val activeOscillators = configs.count { it.enabled }
                buffer[i] = if (activeOscillators > 0) (sample * masterVolume).toFloat() / activeOscillators else 0.0f
            }

            if (!SDL_PutAudioStreamData(stream, buffer.toCValues(), buffer.size * Float.SIZE_BYTES)) {
                error("Failed to put audio data: ${SDL_GetError()}")
            }
        }
    }

    fun getOscillatorControl(index: Int): OscillatorControl? {
        return if (index in oscillators.indices) oscillators[index] else null
    }

    fun withOscillator(index: Int, block: Oscillator.() -> Unit) {
        if (index in oscillators.indices) {
            oscillators[index].block()
        }
    }

    fun randomize() {
        configs.forEachIndexed { index, config ->
            config.enabled = (0..1).random() == 1
            config.frequency = (20..20000).random().toDouble()
            config.detune = (-50..50).random().toDouble()
            config.volume = (0..100).random() / 100.0
            config.waveform = Oscillator.Waveform.entries.toTypedArray().random()

            updateOscillator(index)
        }
    }

    fun countEnabled(): Int {
        return configs.filter { it.enabled }.size
    }

    // Pause, resume, clear, and cleanup methods
    fun pause() = SDL_PauseAudioStreamDevice(stream)
    fun resume() = SDL_ResumeAudioStreamDevice(stream)
    fun clear() = SDL_ClearAudioStream(stream)
    fun cleanup() = SDL_DestroyAudioStream(stream)
}
