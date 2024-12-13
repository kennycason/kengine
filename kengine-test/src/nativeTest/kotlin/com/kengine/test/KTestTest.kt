package com.kengine.test

import kotlin.test.Test

class KTestTest {
    @Test
    fun `test assertions`() {
        val text = "Hello World"
        expectThat(text)
            .isNotNull()
            .hasSize(11)
            .contains("World")
            .startsWith("Hello")

        val numbers = listOf(1, 2, 3)
        expectThat(numbers)
            .isNotEmpty()
            .hasSize(3)
            .contains(2)

        val number = 42
        expectThat(number)
            .isEqualTo(42)
            .isGreaterThan(40)
            .isLessThan(50)
            .isNotEqualTo(41)
    }

    @Test
    fun testExceptions() {
        // Basic exception type checking
        expectThrows<IllegalArgumentException> {
            throw IllegalArgumentException("bad value")
        }

        // Check exception message
        expectThrows<IllegalArgumentException> {
            throw IllegalArgumentException("bad value")
        }.withMessage("bad value")

        // Check exception cause
        val cause = RuntimeException("root cause")
        expectThrows<IllegalArgumentException> {
            throw IllegalArgumentException("bad value", cause)
        }.withMessage("bad value").withCause(cause)
    }

    @Test
    fun `test assertion expressions`() {
        val text = "Hello World"
        expectThat(text) {
            isNotNull()
            hasSize(11)
            contains("World")
            startsWith("Hello")
        }

        val numbers = listOf(1, 2, 3)
        expectThat(numbers) {
            isNotEmpty()
            hasSize(3)
            contains(2)
        }

        val number = 42
        expectThat(number) {
            isEqualTo(42)
            isGreaterThan(40)
            isLessThan(50)
            isNotEqualTo(41)
        }
    }

}
