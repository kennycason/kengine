package com.kengine.sound.synth

import com.kengine.math.Math

class Oscillator(
    private var frequency: Double,
    private val sampleRate: Int,
    private var waveform: Waveform = Waveform.SINE,
    private var detune: Double = 0.0 // detuning in Hz
) {
    enum class Waveform { SINE, SQUARE, SAW, TRIANGLE }

    private var phase = 0.0
    private var phaseIncrement = calculatePhaseIncrement()

    private fun calculatePhaseIncrement(): Double {
        val adjustedFrequency = frequency + detune
        return 2.0 * Math.PI * adjustedFrequency / sampleRate
    }

    fun setFrequency(newFrequency: Double) {
        frequency = newFrequency
        phaseIncrement = calculatePhaseIncrement()
    }

    fun getFrequency() = frequency

    fun setWaveform(newWaveform: Waveform) {
        waveform = newWaveform
    }

    fun setDetune(newDetune: Double) {
        detune = newDetune
        phaseIncrement = calculatePhaseIncrement()
    }

    fun nextSample(): Float {
        val sample = when (waveform) {
            Waveform.SINE -> kotlin.math.sin(phase)
            Waveform.SQUARE -> if (kotlin.math.sin(phase) >= 0) 1.0 else -1.0
            Waveform.SAW -> 2.0 * (phase / (2.0 * Math.PI)) - 1.0
            Waveform.TRIANGLE -> 2.0 * kotlin.math.abs(2.0 * (phase / (2.0 * Math.PI)) - 1.0) - 1.0
        }

        phase = (phase + phaseIncrement) % (2.0 * Math.PI) // wrap phase
        return sample.toFloat()
    }
}
