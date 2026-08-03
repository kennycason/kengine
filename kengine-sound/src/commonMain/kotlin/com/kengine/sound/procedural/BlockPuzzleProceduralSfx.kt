package com.kengine.sound.procedural

object BlockPuzzleProceduralSfx {
    fun rotate(): ProceduralSfxSpec = sweep(
        durationSamples = 2_400,
        volume = 180,
        startFrequencyHz = 880.0,
        endFrequencyHz = 1_280.0
    )

    fun hardDrop(): ProceduralSfxSpec = sweep(
        durationSamples = 5_200,
        volume = 220,
        startFrequencyHz = 360.0,
        endFrequencyHz = 110.0,
        noise = 80
    )

    fun lock(): ProceduralSfxSpec = sweep(
        durationSamples = 3_600,
        volume = 190,
        startFrequencyHz = 210.0,
        endFrequencyHz = 150.0,
        noise = 110
    )

    fun lineClear(): ProceduralSfxSpec = sweep(
        durationSamples = 8_800,
        volume = 220,
        startFrequencyHz = 560.0,
        endFrequencyHz = 1_560.0
    )

    fun gameOver(): ProceduralSfxSpec = sweep(
        durationSamples = 18_000,
        volume = 210,
        startFrequencyHz = 320.0,
        endFrequencyHz = 70.0,
        noise = 50
    )

    fun pause(): ProceduralSfxSpec = sweep(
        durationSamples = 2_800,
        volume = 150,
        startFrequencyHz = 620.0,
        endFrequencyHz = 420.0
    )

    fun reset(): ProceduralSfxSpec = sweep(
        durationSamples = 4_200,
        volume = 170,
        startFrequencyHz = 1_250.0,
        endFrequencyHz = 760.0
    )

    fun all(): Map<String, ProceduralSfxSpec> = mapOf(
        "rotate" to rotate(),
        "hard-drop" to hardDrop(),
        "lock" to lock(),
        "line-clear" to lineClear(),
        "game-over" to gameOver(),
        "pause" to pause(),
        "reset" to reset()
    )

    private fun sweep(
        durationSamples: Int,
        volume: Int,
        startFrequencyHz: Double,
        endFrequencyHz: Double,
        noise: Int = 0
    ): ProceduralSfxSpec {
        return ProceduralSfxSpec(
            durationSeconds = durationSamples.toDouble() / SAMPLE_RATE.toDouble(),
            startFrequencyHz = startFrequencyHz,
            endFrequencyHz = endFrequencyHz,
            volume = volume.toDouble() / 255.0,
            noiseAmount = noise.toDouble() / 255.0,
            waveform = ProceduralWaveform.SQUARE,
            envelope = ProceduralSfxEnvelope.LINEAR_DECAY
        )
    }

    private const val SAMPLE_RATE = 48_000
}
