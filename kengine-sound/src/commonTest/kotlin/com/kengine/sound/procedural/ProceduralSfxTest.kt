package com.kengine.sound.procedural

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProceduralSfxTest {
    @Test
    fun rendersDeterministicPcm() {
        val spec = BlockPuzzleProceduralSfx.hardDrop()

        val first = ProceduralSfx.renderPcm16Le(spec)
        val second = ProceduralSfx.renderPcm16Le(spec)

        assertContentEquals(first, second)
        assertTrue(first.any { it != 0.toByte() })
    }

    @Test
    fun rendersPcm16WavHeader() {
        val format = ProceduralSfxFormat(sampleRate = 48_000, channels = 2)
        val wav = ProceduralSfx.renderWavPcm16Le(BlockPuzzleProceduralSfx.rotate(), format)

        assertAscii(wav, 0, "RIFF")
        assertAscii(wav, 8, "WAVE")
        assertAscii(wav, 12, "fmt ")
        assertAscii(wav, 36, "data")
        assertEquals(48_000, wav.readIntLe(24))
        assertEquals(2, wav.readShortLe(22))
        assertEquals(wav.size - 44, wav.readIntLe(40))
    }

    @Test
    fun blockPuzzlePresetsProduceShortClips() {
        BlockPuzzleProceduralSfx.all().forEach { (_, spec) ->
            val pcm = ProceduralSfx.renderPcm16Le(spec)

            assertTrue(pcm.isNotEmpty())
            assertTrue(pcm.size < 48_000 * 2 * 2)
        }
    }

    private fun assertAscii(bytes: ByteArray, offset: Int, value: String) {
        value.forEachIndexed { index, char ->
            assertEquals(char.code.toByte(), bytes[offset + index])
        }
    }

    private fun ByteArray.readIntLe(offset: Int): Int {
        return (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            ((this[offset + 3].toInt() and 0xff) shl 24)
    }

    private fun ByteArray.readShortLe(offset: Int): Int {
        return (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8)
    }
}
