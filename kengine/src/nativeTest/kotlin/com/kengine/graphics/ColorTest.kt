package com.kengine.graphics

import com.kengine.test.expectArray
import com.kengine.test.expectThat
import com.kengine.test.expectThrows
import kotlin.test.Test

class ColorTest {

    @Test
    fun toUInt32() {
        val color = Color(
            r = 255.toUByte(),
            g = 128.toUByte(),
            b = 64.toUByte(),
            a = 255.toUByte()
        )

        val packedColor = color.toUInt()
        val expectedUInt32: UInt = 0xFF8040FFu

        expectThat(packedColor).isEqualTo(expectedUInt32)
    }

    @Test
    fun fromUInt32() {
        val packedColor: UInt = 0xFF8040FFu
        val color = Color.fromUInt32(packedColor)

        expectThat(color.r).isEqualTo(255.toUByte())
        expectThat(color.g).isEqualTo(128.toUByte())
        expectThat(color.b).isEqualTo(64.toUByte())
        expectThat(color.a).isEqualTo(255.toUByte())
    }

    @Test
    fun `round-trip conversion`() {
        val originalColor = Color(
            r = 100.toUByte(),
            g = 150.toUByte(),
            b = 200.toUByte(),
            a = 250.toUByte()
        )

        val packedColor = originalColor.toUInt()
        val unpackedColor = Color.fromUInt32(packedColor)

        expectThat(unpackedColor).isEqualTo(originalColor)
    }

    @Test
    fun `test basic color creation`() {
        val color = Color(255u, 0u, 0u, 255u)

        expectThat(color.r).isEqualTo(255u.toUByte())
        expectThat(color.g).isEqualTo(0u.toUByte())
        expectThat(color.b).isEqualTo(0u.toUByte())
        expectThat(color.a).isEqualTo(255u.toUByte())
    }

    @Test
    fun `test color from UInt32`() {
        val color = Color.fromUInt32(0xFF0000FFu)

        expectThat(color.r).isEqualTo(255u.toUByte())
        expectThat(color.g).isEqualTo(0u.toUByte())
        expectThat(color.b).isEqualTo(0u.toUByte())
        expectThat(color.a).isEqualTo(255u.toUByte())
    }

    @Test
    fun `test color from RGBA`() {
        val color = Color.fromRGBA(1.0f, 0.5f, 0.0f)

        expectThat(color.r).isEqualTo(255u.toUByte())
        expectThat(color.g).isEqualTo(127u.toUByte())
        expectThat(color.b).isEqualTo(0u.toUByte())
        expectThat(color.a).isEqualTo(255u.toUByte())
    }

    @Test
    fun `test color from HSV`() {
        val color = Color.fromHSV(0f, 1.0f, 1.0f) // Red

        expectThat(color.r).isEqualTo(255u.toUByte())
        expectThat(color.g).isEqualTo(0u.toUByte())
        expectThat(color.b).isEqualTo(0u.toUByte())
    }

    @Test
    fun `test invert color`() {
        val color = Color(0u, 0u, 0u, 255u) // Black
        val inverted = color.invert()

        expectThat(inverted.r).isEqualTo(255u.toUByte())
        expectThat(inverted.g).isEqualTo(255u.toUByte())
        expectThat(inverted.b).isEqualTo(255u.toUByte())
    }

    @Test
    fun `test brightness adjustment`() {
        val color = Color(100u, 100u, 100u, 255u)
        val brighter = color.adjustBrightness(1.5f)

        expectThat(brighter.r).isEqualTo(150u.toUByte())
        expectThat(brighter.g).isEqualTo(150u.toUByte())
        expectThat(brighter.b).isEqualTo(150u.toUByte())
    }

    @Test
    fun `test blending colors`() {
        val red = Color(255u, 0u, 0u, 255u)
        val blue = Color(0u, 0u, 255u, 255u)
        val blended = red.blend(blue, 0.5f)

        expectThat(blended.r).isEqualTo(127u.toUByte())
        expectThat(blended.g).isEqualTo(0u.toUByte())
        expectThat(blended.b).isEqualTo(127u.toUByte())
    }

    @Test
    fun `test grayscale conversion`() {
        val color = Color(50u, 100u, 150u, 255u)
        val gray = color.toGrayscale()

        expectThat(gray.r).isEqualTo(gray.g).isEqualTo(gray.b)
    }

    /**
     * 🌈
     * Color(r=255, g=0, b=0, a=255)
     * Color(r=255, g=182, b=0, a=255)
     * Color(r=145, g=255, b=0, a=255)
     * Color(r=0, g=255, b=36, a=255)
     * Color(r=0, g=255, b=218, a=255)
     * Color(r=0, g=109, b=255, a=255)
     * Color(r=72, g=0, b=255, a=255)
     * Color(r=255, g=0, b=255, a=255)
     */
    @Test
    fun `test rainbow generation`() {
        val rainbowColors = Color.rainbow(8)

        expectThat(rainbowColors)
            .hasSize(8)

        expectArray(rainbowColors) {
            first().property(Color::r).isEqualTo(255u.toUByte())
            last().property(Color::b).isEqualTo(255u.toUByte())
        }
    }

    @Test
    fun `test invalid hex input`() {
        expectThrows<IllegalArgumentException> {
            Color.fromHex("12345")
        }
    }

    @Test
    fun `test valid hex input`() {
        val color = Color.fromHex("FF0000FF") // Red with full alpha

        expectThat(color.r).isEqualTo(255u.toUByte())
        expectThat(color.g).isEqualTo(0u.toUByte())
        expectThat(color.b).isEqualTo(0u.toUByte())
        expectThat(color.a).isEqualTo(255u.toUByte())
    }
}
