package com.kengine.sound.procedural

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ProceduralWaveform {
    SINE,
    SQUARE,
    SAW,
    TRIANGLE
}

data class ProceduralSfxFormat(
    val sampleRate: Int = 48_000,
    val channels: Int = 2
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive." }
        require(channels > 0) { "channels must be positive." }
    }
}

data class ProceduralSfxSpec(
    val durationSeconds: Double,
    val startFrequencyHz: Double,
    val endFrequencyHz: Double = startFrequencyHz,
    val volume: Double = 0.75,
    val noiseAmount: Double = 0.0,
    val waveform: ProceduralWaveform = ProceduralWaveform.SQUARE,
    val seed: Int = 0x12345678.toInt(),
    val envelope: ProceduralSfxEnvelope = ProceduralSfxEnvelope.LINEAR_DECAY
) {
    init {
        require(durationSeconds > 0.0) { "durationSeconds must be positive." }
        require(startFrequencyHz > 0.0) { "startFrequencyHz must be positive." }
        require(endFrequencyHz > 0.0) { "endFrequencyHz must be positive." }
    }
}

enum class ProceduralSfxEnvelope {
    FLAT,
    LINEAR_DECAY
}

object ProceduralSfx {
    fun renderPcm16Le(
        spec: ProceduralSfxSpec,
        format: ProceduralSfxFormat = ProceduralSfxFormat()
    ): ByteArray {
        val frameCount = (spec.durationSeconds * format.sampleRate).roundToInt().coerceAtLeast(1)
        val output = ByteArray(frameCount * format.channels * BYTES_PER_SAMPLE)
        var phase = 0.0
        var noise = spec.seed
        var byteIndex = 0

        for (frame in 0 until frameCount) {
            val progress = if (frameCount <= 1) 1.0 else frame.toDouble() / (frameCount - 1).toDouble()
            val frequency = spec.startFrequencyHz + (spec.endFrequencyHz - spec.startFrequencyHz) * progress
            phase = (phase + frequency / format.sampleRate.toDouble()) % 1.0

            val wave = sampleWaveform(spec.waveform, phase)
            noise = noise * 1_664_525 + 1_013_904_223
            val noiseWave = if (noise < 0) 1.0 else -1.0
            val mixed = mix(wave, noiseWave, spec.noiseAmount.coerceIn(0.0, 1.0))
            val envelope = envelopeValue(spec.envelope, progress)
            val sample = (mixed * spec.volume.coerceIn(0.0, 1.0) * envelope)
                .coerceIn(-1.0, 1.0)
            val pcm = (sample * Short.MAX_VALUE).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

            repeat(format.channels) {
                output[byteIndex] = (pcm and 0xff).toByte()
                output[byteIndex + 1] = ((pcm ushr 8) and 0xff).toByte()
                byteIndex += BYTES_PER_SAMPLE
            }
        }

        return output
    }

    fun renderWavPcm16Le(
        spec: ProceduralSfxSpec,
        format: ProceduralSfxFormat = ProceduralSfxFormat()
    ): ByteArray {
        return wavPcm16Le(renderPcm16Le(spec, format), format)
    }

    fun wavPcm16Le(
        pcm: ByteArray,
        format: ProceduralSfxFormat = ProceduralSfxFormat()
    ): ByteArray {
        val byteRate = format.sampleRate * format.channels * BYTES_PER_SAMPLE
        val blockAlign = format.channels * BYTES_PER_SAMPLE
        val output = ByteArray(WAV_HEADER_BYTES + pcm.size)

        fun writeAscii(offset: Int, value: String) {
            value.forEachIndexed { index, char ->
                output[offset + index] = char.code.toByte()
            }
        }

        fun writeIntLe(offset: Int, value: Int) {
            output[offset] = (value and 0xff).toByte()
            output[offset + 1] = ((value ushr 8) and 0xff).toByte()
            output[offset + 2] = ((value ushr 16) and 0xff).toByte()
            output[offset + 3] = ((value ushr 24) and 0xff).toByte()
        }

        fun writeShortLe(offset: Int, value: Int) {
            output[offset] = (value and 0xff).toByte()
            output[offset + 1] = ((value ushr 8) and 0xff).toByte()
        }

        writeAscii(0, "RIFF")
        writeIntLe(4, output.size - 8)
        writeAscii(8, "WAVE")
        writeAscii(12, "fmt ")
        writeIntLe(16, 16)
        writeShortLe(20, 1)
        writeShortLe(22, format.channels)
        writeIntLe(24, format.sampleRate)
        writeIntLe(28, byteRate)
        writeShortLe(32, blockAlign)
        writeShortLe(34, 16)
        writeAscii(36, "data")
        writeIntLe(40, pcm.size)
        pcm.copyInto(output, WAV_HEADER_BYTES)

        return output
    }

    private fun sampleWaveform(waveform: ProceduralWaveform, phase: Double): Double {
        return when (waveform) {
            ProceduralWaveform.SINE -> sin(phase * 2.0 * PI)
            ProceduralWaveform.SQUARE -> if (phase < 0.5) 1.0 else -1.0
            ProceduralWaveform.SAW -> 2.0 * phase - 1.0
            ProceduralWaveform.TRIANGLE -> 1.0 - 4.0 * abs(phase - 0.5)
        }
    }

    private fun envelopeValue(envelope: ProceduralSfxEnvelope, progress: Double): Double {
        return when (envelope) {
            ProceduralSfxEnvelope.FLAT -> 1.0
            ProceduralSfxEnvelope.LINEAR_DECAY -> 1.0 - progress.coerceIn(0.0, 1.0)
        }
    }

    private fun mix(a: Double, b: Double, amount: Double): Double {
        return a * (1.0 - amount) + b * amount
    }

    private const val BYTES_PER_SAMPLE = 2
    private const val WAV_HEADER_BYTES = 44
}
