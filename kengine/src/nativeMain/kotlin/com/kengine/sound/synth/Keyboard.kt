package com.kengine.sound.synth

class Keyboard(private val oscillator: Oscillator) {

    // Mapping keys to frequencies (just an example for A4=440Hz)
    private val noteFrequencies = mapOf(
        "A" to 440.0,
        "A#" to 466.16,
        "B" to 493.88,
        "C" to 523.25,
        "C#" to 554.37,
        "D" to 587.33,
        "D#" to 622.25,
        "E" to 659.25,
        "F" to 698.46,
        "F#" to 739.99,
        "G" to 783.99,
        "G#" to 830.61
    )

    fun playNote(note: String) {
        val frequency = noteFrequencies[note] ?: error("Invalid note: $note")
        oscillator.setFrequency(frequency)
    }
}
