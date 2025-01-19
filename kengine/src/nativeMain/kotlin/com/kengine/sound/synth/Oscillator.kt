package com.kengine.sound.synth

import com.kengine.log.Logging
import com.kengine.math.Math

class Oscillator(
    private var frequency: Double,
    private val sampleRate: Int,
    private var waveform: Waveform = Waveform.SINE,
    private var detune: Double = 0.0, // detuning in Hz
    private val filter: Filter = Filter(cutoff = 1000.0, resonance = 0.5, sampleRate = sampleRate),
    private val adsr: ADSR = ADSR(),
    private val lfo: LFO = LFO()
) : OscillatorControl, Logging {
    enum class Waveform { SINE, SQUARE, SAW, TRIANGLE }

    private var phase = 0.0
    private var phaseIncrement = calculatePhaseIncrement()

    fun nextSample(): Float {
        // Apply LFO to modulate the frequency
        val frequencyModulation = if (lfo.enabled) lfo.getValue() else 0.0
        val modulatedFrequency = (frequency + frequencyModulation).coerceIn(20.0, 20000.0)
        setFrequency(modulatedFrequency)

        // Generate the raw waveform
        val rawSample = when (waveform) {
            Waveform.SINE -> kotlin.math.sin(phase)
            Waveform.SQUARE -> if (kotlin.math.sin(phase) >= 0) 1.0 else -1.0
            Waveform.SAW -> 2.0 * (phase / (2.0 * Math.PI)) - 1.0
            Waveform.TRIANGLE -> 2.0 * kotlin.math.abs(2.0 * (phase / (2.0 * Math.PI)) - 1.0) - 1.0
        }

        // Apply ADSR envelope if enabled
        val adsrValue = if (adsr.enabled) adsr.getValue() else 1.0
        //logger.info("Oscillator ADSR: enabled=${adsr.enabled}, value=$adsrValue, state=${adsr.toString()}")

        // Apply filter if enabled
        val filteredSample = if (filter.enabled) filter.apply(rawSample) else rawSample
        //logger.info("Oscillator Filter: enabled=${filter.enabled}, sample=$filteredSample")

        // Scale the filtered sample by the ADSR value
        val finalSample = filteredSample * adsrValue

        // Increment phase based on frequency
        phase = (phase + phaseIncrement) % (2.0 * Math.PI)

        // Final output
        return finalSample.toFloat()
    }

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

    override fun setFilterCutoff(cutoff: Double) {
        filter.setCutoff(cutoff)
    }

    override fun setFilterResonance(resonance: Double) {
        filter.setResonance(resonance)
    }

    override fun enableADSR(enabled: Boolean) {
        adsr.enabled = enabled
        logger.info("ADSR Enabled: $enabled")
    }

    override fun enableLFO(enabled: Boolean) {
        lfo.enabled = enabled
    }

    override fun enableFilter(enabled: Boolean) {
        filter.enabled = enabled
    }

    override fun setADSR(
        attack: Double?,
        decay: Double?,
        sustain: Double?,
        release: Double?
    ) {
        attack?.let { adsr.attack = it }
        decay?.let { adsr.decay = it }
        sustain?.let { adsr.sustain = it }
        release?.let { adsr.release = it }
    }

    override fun setLFO(
        frequency: Double?,
        amplitude: Double?
    ) {
        frequency?.let { lfo.frequency = it }
        amplitude?.let { lfo.amplitude = it }
    }

    fun trigger() {
        adsr.trigger()
    }

    fun release() {
        adsr.release()
    }

}
