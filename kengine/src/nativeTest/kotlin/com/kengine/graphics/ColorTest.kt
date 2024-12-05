package com.kengine.graphics

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorTest {

    @Test
    fun toUInt32() {
        val color = Color(
            r = 255.toUByte(),
            g = 128.toUByte(),
            b = 64.toUByte(),
            a = 255.toUByte()
        )

        val packedColor = color.toUInt32()
        val expectedUInt32: UInt = 0xFF8040FFu

        assertEquals(expectedUInt32, packedColor)
    }

    @Test
    fun fromUInt32() {
        val packedColor: UInt = 0xFF8040FFu
        val color = Color.fromUInt32(packedColor)

        assertEquals(255.toUByte(), color.r)
        assertEquals(128.toUByte(), color.g)
        assertEquals(64.toUByte(), color.b)
        assertEquals(255.toUByte(), color.a)
    }

    @Test
    fun `round-trip conversion`() {
        val originalColor = Color(
            r = 100.toUByte(),
            g = 150.toUByte(),
            b = 200.toUByte(),
            a = 250.toUByte()
        )

        val packedColor = originalColor.toUInt32()
        val unpackedColor = Color.fromUInt32(packedColor)

        assertEquals(originalColor, unpackedColor, "Color should remain the same after toUInt32() and fromUInt32()")
    }
}