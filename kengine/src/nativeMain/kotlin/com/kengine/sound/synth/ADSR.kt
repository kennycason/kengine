package com.kengine.sound.synth

/**
 * ADSR (Attack, Decay, Sustain, Release) envelope shapes the amplitude of the sound to make it more dynamic & expressive.
 */
class ADSR(
    var attack: Double = 0.1,  // Attack time in seconds
    var decay: Double = 0.1,   // Decay time in seconds
    var sustain: Double = 0.7, // Sustain level (0.0 to 1.0)
    var release: Double = 0.3, // Release time in seconds
    val sampleRate: Int = 44100,
    var enabled: Boolean = false
) {
    private var state: State = State.IDLE
    private var value = 0.0 // Current ADSR value
    private var time = 0.0 // Current time within the current phase

    enum class State { ATTACK, DECAY, SUSTAIN, RELEASE, IDLE }

    fun trigger() {
        state = State.ATTACK
        time = 0.0
    }

    fun release() {
        state = State.RELEASE
        time = 0.0
    }

    fun getValue(): Double {
        if (!enabled) return 1.0

        when (state) {
            State.ATTACK -> {
                value += 1.0 / (attack * sampleRate)
                if (value >= 1.0) {
                    value = 1.0
                    state = State.DECAY
                }
            }

            State.DECAY -> {
                value -= (1.0 - sustain) / (decay * sampleRate)
                if (value <= sustain) {
                    value = sustain
                    state = State.SUSTAIN
                }
            }

            State.SUSTAIN -> {
                // Maintain sustain level
                value = sustain
            }

            State.RELEASE -> {
                value -= sustain / (release * sampleRate)
                if (value <= 0.0) {
                    value = 0.0
                    state = State.IDLE
                }
            }

            State.IDLE -> {
                // Do nothing
            }
        }
        return value
    }
}
