package com.kengine.sound.keyboard

import platform.posix.pow

object NoteGenerator {
    const val WHITE_KEYS_PER_OCTAVE = 7
    const val BLACK_KEYS_PER_OCTAVE = 5
    const val TOTAL_KEYS_PER_OCTAVE = 12

    // Base frequencies for octave 4 (A4 = 440Hz is the standard reference pitch)
    private val baseOctaveNotes = listOf(
        Note("C", 261.63, false),
        Note("C#", 277.18, true),
        Note("D", 293.66, false),
        Note("D#", 311.13, true),
        Note("E", 329.63, false),
        Note("F", 349.23, false),
        Note("F#", 369.99, true),
        Note("G", 392.00, false),
        Note("G#", 415.30, true),
        Note("A", 440.00, false),
        Note("A#", 466.16, true),
        Note("B", 493.88, false)
    )

    data class Note(
        val name: String,
        val frequency: Double,
        val isBlack: Boolean
    )

    /**
     * Generates notes for specified octaves.
     * @param startOctave The starting octave number (e.g., 2 for C2)
     * @param numOctaves How many octaves to generate
     * @return List of PianoKey objects spanning the requested octaves
     */
    fun generateNotes(startOctave: Int, numOctaves: Int): List<VirtualKeyboard.PianoKey> {
        val notes = mutableListOf<VirtualKeyboard.PianoKey>()

        for (octave in startOctave until (startOctave + numOctaves)) {
            // For each note in the base octave, calculate its frequency in the current octave
            // The frequency doubles each octave
            val multiplier = pow(2.0, octave - 4.0)  // 4 is our reference octave

            baseOctaveNotes.forEach { note ->
                notes.add(
                    VirtualKeyboard.PianoKey(
                        note = "${note.name}$octave",  // e.g., "C4", "A#3"
                        frequency = note.frequency * multiplier,
                        isBlack = note.isBlack
                    )
                )
            }
        }

        return notes
    }

    /**
     * Gets the frequency for a specific note in a specific octave
     */
    fun getNoteFrequency(noteName: String, octave: Int): Double {
        val baseNote = baseOctaveNotes.find { it.name == noteName }
            ?: throw IllegalArgumentException("Invalid note name: $noteName")

        return baseNote.frequency * pow(2.0, octave - 4.0)
    }
}
