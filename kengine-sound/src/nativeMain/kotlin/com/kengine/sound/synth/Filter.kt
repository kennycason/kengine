package com.kengine.sound.synth

class Filter(
    private var cutoff: Double = 1000.0, // Cutoff frequency in Hz
    private var resonance: Double = 0.5, // Resonance amount (0-1)
    private val sampleRate: Int = 44100,
    var enabled: Boolean = false
) {
    private var previousInput = 0.0
    private var previousOutput = 0.0
    private var feedback = calculateFeedback()

    private fun calculateFeedback(): Double {
        return resonance * (1.0 - 0.15 * (cutoff / sampleRate)) // Simple feedback factor
    }

    fun apply(input: Double): Double {
        if (!enabled) return input
        // 1-pole low-pass filter with resonance feedback
        val output = previousOutput + (cutoff / sampleRate) *
            (input - previousOutput + feedback * (previousOutput - previousInput))
        previousInput = input
        previousOutput = output
        return output
    }

    fun setCutoff(newCutoff: Double) {
        cutoff = newCutoff
        feedback = calculateFeedback()
    }

    fun setResonance(newResonance: Double) {
        resonance = newResonance.coerceIn(0.0, 1.0) // Clamp resonance to [0, 1]
        feedback = calculateFeedback()
    }
}
