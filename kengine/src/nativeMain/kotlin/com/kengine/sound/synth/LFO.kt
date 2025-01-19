package com.kengine.sound.synth

import com.kengine.math.Math

/**
 * LFO (Low-Frequency Oscillator) Modulation
 *
 * Modulate parameters like pitch, amplitude, or filter cutoff, creating vibrato, tremolo, or wah-wah effects.
 */
class LFO(
    var frequency: Double = 5.0, // LFO frequency in Hz
    val sampleRate: Int = 44100,
    private var depth: Double = 0.5, // Modulation depth
    var amplitude: Double = 1.0, // Overall amplitude scaling
    private var waveform: Oscillator.Waveform = Oscillator.Waveform.SINE,
    var enabled: Boolean = false
) {
    private var phase = 0.0

    private var phaseIncrement = calculatePhaseIncrement()

    private fun calculatePhaseIncrement(): Double {
        return 2.0 * Math.PI * frequency / sampleRate
    }

    fun getValue(): Double {
        if (!enabled) return 0.0

        val baseValue = when (waveform) {
            Oscillator.Waveform.SINE -> kotlin.math.sin(phase)
            Oscillator.Waveform.SQUARE -> if (kotlin.math.sin(phase) >= 0) 1.0 else -1.0
            Oscillator.Waveform.SAW -> 2.0 * (phase / (2.0 * Math.PI)) - 1.0
            Oscillator.Waveform.TRIANGLE -> 2.0 * kotlin.math.abs(2.0 * (phase / (2.0 * Math.PI)) - 1.0) - 1.0
        }
        phase = (phase + calculatePhaseIncrement()) % (2.0 * Math.PI)
        return baseValue * depth * amplitude // Combine depth and amplitude
    }

    fun setWaveform(newWaveform: Oscillator.Waveform) {
        waveform = newWaveform
    }

    fun setFrequency(newFrequency: Double) {
        frequency = newFrequency
        phaseIncrement = calculatePhaseIncrement()
    }

    fun setDepth(newDepth: Double) {
        depth = newDepth.coerceIn(0.0, 1.0) // Clamp to valid range
    }

    fun setAmplitude(newAmplitude: Double) {
        amplitude = newAmplitude.coerceIn(0.0, 1.0) // Clamp to valid range
    }
}
