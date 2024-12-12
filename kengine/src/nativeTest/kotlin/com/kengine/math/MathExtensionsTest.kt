package com.kengine.math

import abs
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

class MathExtensionsExpandedTest {

    @Test
    fun `squared test`() {
        assertEquals(4, 2.squared)
        assertEquals(4.0, 2.0.squared)
        assertEquals(4f, 2f.squared)
        assertEquals(4, 2.toShort().squared)
        assertEquals(4u, 2.toUShort().squared)
    }

    @Test
    fun `cubed test`() {
        assertEquals(8, 2.cubed)
        assertEquals(8.0, 2.0.cubed)
        assertEquals(8f, 2f.cubed)
        assertEquals(8, 2.toShort().cubed)
        assertEquals(8u, 2.toUShort().cubed)
    }

    @Test
    fun `root test`() {
        assertEquals(2.0, 4.0.root, 0.0001)
        assertEquals(2f, 4f.root, 0.0001f)
    }

    @Test
    fun `isEven and isOdd test`() {
        assertEquals(true, 4.isEven)
        assertEquals(false, 4.isOdd)
        assertEquals(true, (-4).isEven)
        assertEquals(false, (-4).isOdd)
    }

    @Test
    fun `factorial test`() {
        assertEquals(120L, 5.factorial)
        assertEquals(1L, 0.toShort().factorial)
        assertFailsWith<IllegalArgumentException> { (-1).factorial }
    }

    @Test
    fun `reciprocal test`() {
        assertEquals(0.5f, 2f.reciprocal)
        assertEquals(1.0, 1.0.reciprocal)
        assertFailsWith<ArithmeticException> { 0.0.reciprocal }
    }

    @Test
    fun `absolute value test`() {
        assertEquals(5, 5.abs)
        assertEquals(5, (-5).abs)
        assertEquals(5f, (-5f).abs)
    }
}