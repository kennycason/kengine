package com.kengine.test

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssertionBuilder<T>(private val actual: T) {
    fun isEqualTo(expected: T): AssertionBuilder<T> {
        assertEquals(expected, actual, "Expected $actual to be equal to $expected")
        return this
    }

    fun isNotEqualTo(expected: T): AssertionBuilder<T> {
        assertNotEquals(expected, actual, "Expected $actual to not be equal to $expected")
        return this
    }

    fun isNull(): AssertionBuilder<T> {
        assertNull(actual, "Expected null but was $actual")
        return this
    }

    fun isNotNull(): AssertionBuilder<T> {
        assertNotNull(actual, "Expected non-null value but was null")
        return this
    }

    fun isTrue(): AssertionBuilder<T> {
        assertTrue(actual as Boolean, "Expected true but was false")
        return this
    }

    fun isFalse(): AssertionBuilder<T> {
        assertFalse(actual as Boolean, "Expected false but was true")
        return this
    }

    fun contains(element: Any?): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> assertTrue(actual.contains(element),
                "Expected collection to contain $element but was $actual")
            is String -> assertTrue(actual.contains(element as String),
                "Expected string to contain $element but was $actual")
            else -> throw IllegalArgumentException("Contains assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun doesNotContain(element: Any?): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> assertFalse(actual.contains(element),
                "Expected collection to not contain $element but was $actual")
            is String -> assertFalse(actual.contains(element as String),
                "Expected string to not contain $element but was $actual")
            else -> throw IllegalArgumentException("Contains assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun hasSize(expected: Int): AssertionBuilder<T>{
        when (actual) {
            is Collection<*> -> assertEquals(expected, actual.size,
                "Expected collection size to be $expected but was ${actual.size}")
            is String -> assertEquals(expected, actual.length,
                "Expected string length to be $expected but was ${actual.length}")
            else -> throw IllegalArgumentException("Size assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun isEmpty(): AssertionBuilder<T>{
        when (actual) {
            is Collection<*> -> assertTrue(actual.isEmpty(),
                "Expected empty collection but was $actual")
            is String -> assertTrue(actual.isEmpty(),
                "Expected empty string but was $actual")
            else -> throw IllegalArgumentException("Empty assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun isNotEmpty(): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> assertTrue(actual.isNotEmpty(),
                "Expected non-empty collection but was empty")
            is String -> assertTrue(actual.isNotEmpty(),
                "Expected non-empty string but was empty")
            else -> throw IllegalArgumentException("Empty assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun startsWith(prefix: String): AssertionBuilder<T> {
        assertTrue(actual.toString().startsWith(prefix),
            "Expected $actual to start with $prefix")
        return this
    }

    fun endsWith(suffix: String): AssertionBuilder<T> {
        assertTrue(actual.toString().endsWith(suffix),
            "Expected $actual to end with $suffix")
        return this
    }

    fun matches(regex: Regex): AssertionBuilder<T> {
        assertTrue(actual.toString().matches(regex),
            "Expected $actual to match regex $regex")
        return this
    }

    // Number specific assertions
    fun isGreaterThan(expected: Number): AssertionBuilder<T> {
        when (actual) {
            is Number -> assertTrue(actual.toDouble() > expected.toDouble(),
                "Expected $actual to be greater than $expected")
            else -> throw IllegalArgumentException("Greater than assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun isLessThan(expected: Number): AssertionBuilder<T> {
        when (actual) {
            is Number -> assertTrue(actual.toDouble() < expected.toDouble(),
                "Expected $actual to be less than $expected")
            else -> throw IllegalArgumentException("Less than assertion not supported for type ${actual!!::class}")
        }
        return this
    }
}

class ThrowsBuilder<T : Throwable> {
    private var message: String? = null
    private var cause: Throwable? = null

    fun withMessage(message: String): ThrowsBuilder<T> {
        this.message = message
        return this
    }

    fun withCause(cause: Throwable): ThrowsBuilder<T> {
        this.cause = cause
        return this
    }

    fun verify(block: () -> Unit, exceptionClass: KClass<T>) {
        try {
            block()
            throw IllegalArgumentException("Expected ${exceptionClass.simpleName} to be thrown")
        } catch (e: Throwable) {
            if (!exceptionClass.isInstance(e)) {
                throw IllegalArgumentException("Expected ${exceptionClass.simpleName} but got ${e::class.simpleName}")
            }

            message?.let { expectedMessage ->
                assertEquals(expectedMessage, e.message,
                    "Expected exception message to be '$expectedMessage' but was '${e.message}'")
            }

            cause?.let { expectedCause ->
                assertEquals(expectedCause, e.cause,
                    "Expected exception cause to be $expectedCause but was ${e.cause}")
            }
        }
    }
}

fun <T> expectThat(actual: T): AssertionBuilder<T> = AssertionBuilder(actual)

fun <T> expectThat(
    actual: T,
    lambda: AssertionBuilder<T>.() -> Unit
): AssertionBuilder<T> = AssertionBuilder(actual)

inline fun <reified T : Throwable> expectThrows(noinline block: () -> Unit): ThrowsBuilder<T> {
    val builder = ThrowsBuilder<T>()
    builder.verify(block, T::class)
    return builder
}