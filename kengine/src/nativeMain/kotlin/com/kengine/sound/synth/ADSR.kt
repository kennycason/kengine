package com.kengine.sound.synth

import com.kengine.log.Logging

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
) : Logging {
    private var state: State = State.IDLE
    private var value = 0.0 // Current ADSR value
    private var time = 0.0 // Current time within the current phase

    enum class State { ATTACK, DECAY, SUSTAIN, RELEASE, IDLE }

    fun trigger() {
        state = State.ATTACK
        time = 0.0
        logger.info("Triggered -> Phase: $state, Time: $time")
    }

    fun release() {
        state = State.RELEASE
        time = 0.0
        logger.info("Released -> Phase: $state, Time: $time")
    }

    fun getValue(): Double {
        if (!enabled) return 1.0

       // logger.info("Before Update -> Phase: $state, Value: $value, Time: $time")

        when (state) {
            State.ATTACK -> {
                time += 1.0 / sampleRate
                value = time / attack
                if (value >= 1.0) {
                    value = 1.0
                    state = State.DECAY
                    time = 0.0 // Reset time for the next phase
                    //logger.info("Transitioned to DECAY phase")
                }
            }

            State.DECAY -> {
                time += 1.0 / sampleRate
                value = 1.0 - (1.0 - sustain) * (time / decay)
                if (value <= sustain) {
                    value = sustain
                    state = State.SUSTAIN
                    time = 0.0 // Reset time for the next phase
                    //logger.info("Transitioned to SUSTAIN phase")
                }
            }

            State.SUSTAIN -> {
                value = sustain // Maintain sustain level
            }

            State.RELEASE -> {
                time += 1.0 / sampleRate
                value = sustain * (1.0 - (time / release))
                if (value <= 0.0) {
                    value = 0.0
                    state = State.IDLE
                    time = 0.0 // Reset time when idle
                    //logger.info("Transitioned to IDLE phase")
                }
            }

            State.IDLE -> {
                // Do nothing, maintain value = 0.0
            }
        }

        //logger.info("After Update -> Phase: $state, Value: $value, Time: $time")
        return value
    }

    override fun toString(): String {
        return "ADSR(attack=$attack, decay=$decay, sustain=$sustain, release=$release, sampleRate=$sampleRate, enabled=$enabled, state=$state, value=$value, time=$time)"
    }
}
