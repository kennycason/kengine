package com.kengine.math

import abs
import clamp
import cubed
import factorial
import isEven
import isOdd
import reciprocal
import root
import squared
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MathExtensionsTest {
    @Test
    fun squaresAndCubesNumbers() {
        assertEquals(4, 2.squared)
        assertEquals(4.0, 2.0.squared)
        assertEquals(4f, 2f.squared)
        assertEquals(4, 2.toShort().squared)
        assertEquals(4u, 2.toUShort().squared)
        assertEquals(8, 2.cubed)
        assertEquals(8.0, 2.0.cubed)
        assertEquals(8f, 2f.cubed)
        assertEquals(8, 2.toShort().cubed)
        assertEquals(8u, 2.toUShort().cubed)
    }

    @Test
    fun rootsAndReciprocalsNumbers() {
        assertEquals(2.0, 4.0.root)
        assertEquals(2f, 4f.root)
        assertEquals(0.5f, 2f.reciprocal)
        assertEquals(1.0, 1.0.reciprocal)
        assertFailsWith<ArithmeticException> { 0.0.reciprocal }
    }

    @Test
    fun reportsParityAndFactorials() {
        assertTrue(4.isEven)
        assertFalse(4.isOdd)
        assertTrue((-4).isEven)
        assertFalse((-4).isOdd)
        assertEquals(120L, 5.factorial)
        assertEquals(1L, 0.toShort().factorial)
        assertFailsWith<IllegalArgumentException> { (-1).factorial }
    }

    @Test
    fun absoluteValueAndClampWork() {
        assertEquals(5, 5.abs)
        assertEquals(5, (-5).abs)
        assertEquals(5f, (-5f).abs)
        assertEquals(5, 10.clamp(0, 5))
        assertEquals(0.0, (-2.0).clamp(0.0, 5.0))
        assertEquals(3u, 3u.clamp(0u, 5u))
    }
}
