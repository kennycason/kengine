package com.kengine.sound.synth

import com.kengine.math.Math

class Oscillator(
    private var frequency: Double,
    private val sampleRate: Int
) {
    private var phase = 0.0
    private var phaseIncrement = calculatePhaseIncrement()

    // calculate phase increment based on frequency
    private fun calculatePhaseIncrement(): Double {
        return 2.0 * Math.PI * frequency / sampleRate
    }

    fun setFrequency(newFrequency: Double) {
        frequency = newFrequency
        phaseIncrement = calculatePhaseIncrement() // recalculate immediately
    }

    fun nextSample(): Float {
        val sample = kotlin.math.sin(phase).toFloat()
        phase = (phase + phaseIncrement) % (2.0 * Math.PI) // ensure phase wraps around
        return sample
    }
}
