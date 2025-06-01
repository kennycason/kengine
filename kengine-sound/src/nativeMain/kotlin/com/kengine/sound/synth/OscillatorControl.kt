package com.kengine.sound.synth

interface OscillatorControl {
    fun enableADSR(enabled: Boolean)
    fun enableLFO(enabled: Boolean)
    fun enableFilter(enabled: Boolean)
    fun setADSR(attack: Double? = null, decay: Double? = null, sustain: Double? = null, release: Double? = null)
    fun setLFO(frequency: Double? = null, amplitude: Double? = null)
    fun setFilterCutoff(cutoff: Double)
    fun setFilterResonance(resonance: Double)
}
