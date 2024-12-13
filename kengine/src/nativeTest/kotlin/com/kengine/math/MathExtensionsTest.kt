package com.kengine.math

import abs
import com.kengine.test.expectThat
import com.kengine.test.expectThrows
import cubed
import factorial
import isEven
import isOdd
import reciprocal
import root
import squared
import kotlin.test.Test

class MathExtensionsExpandedTest {

    @Test
    fun `squared test`() {
        expectThat(2.squared).isEqualTo(4)
        expectThat(2.0.squared).isEqualTo(4.0)
        expectThat(2f.squared).isEqualTo(4f)
        expectThat(2.toShort().squared).isEqualTo(4)
        expectThat(2.toUShort().squared).isEqualTo(4u)
    }

    @Test
    fun `cubed test`() {
        expectThat(2.cubed).isEqualTo(8)
        expectThat(2.0.cubed).isEqualTo(8.0)
        expectThat(2f.cubed).isEqualTo(8f)
        expectThat(2.toShort().cubed).isEqualTo(8)
        expectThat(2.toUShort().cubed).isEqualTo(8u)
    }

    @Test
    fun `root test`() {
        expectThat(4.0.root).isEqualTo(2.0)
        expectThat(4f.root).isEqualTo(2f)
    }

    @Test
    fun `isEven and isOdd test`() {
        expectThat(4.isEven).isTrue()
        expectThat(4.isOdd).isFalse()
        expectThat((-4).isEven).isTrue()
        expectThat((-4).isOdd).isFalse()
    }

    @Test
    fun `factorial test`() {
        expectThat(5.factorial).isEqualTo(120L)
        expectThat(0.toShort().factorial).isEqualTo(1L)
        expectThrows<IllegalArgumentException> { (-1).factorial }
    }

    @Test
    fun `reciprocal test`() {
        expectThat(2f.reciprocal).isEqualTo(0.5f)
        expectThat(1.0.reciprocal).isEqualTo(1.0)
        expectThrows<ArithmeticException> { 0.0.reciprocal }
    }

    @Test
    fun `absolute value test`() {
        expectThat(5.abs).isEqualTo(5)
        expectThat((-5).abs).isEqualTo(5)
        expectThat((-5f).abs).isEqualTo(5f)
    }
}