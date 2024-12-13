package com.kengine.test

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

fun <T> expectThat(actual: T): AssertionBuilder<T> = AssertionBuilder(actual)

fun <T> expectThat(
    actual: T,
    lambda: AssertionBuilder<T>.() -> Unit
): AssertionBuilder<T> = AssertionBuilder(actual)

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
            is Collection<*> -> assertTrue(
                actual.contains(element),
                "Expected collection to contain $element but was $actual"
            )
            is String -> assertTrue(
                actual.contains(element as String),
                "Expected string to contain $element but was $actual"
            )
            else -> throw IllegalArgumentException("Contains assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun containsKey(key: Any?): AssertionBuilder<T> {
        when (actual) {
            is Map<*, *> -> assertTrue(
                actual.containsKey(key),
                "Expected collection to contain key $key but was $actual"
            )
            else -> throw IllegalArgumentException("Contains key assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun doesNotContain(element: Any?): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> assertFalse(
                actual.contains(element),
                "Expected collection to not contain $element but was $actual"
            )
            is String -> assertFalse(
                actual.contains(element as String),
                "Expected string to not contain $element but was $actual"
            )
            else -> throw IllegalArgumentException("Contains assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun hasSize(expected: Int): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> assertEquals(
                expected, actual.size,
                "Expected collection size to be $expected but was ${actual.size}"
            )
            is String -> assertEquals(
                expected, actual.length,
                "Expected string length to be $expected but was ${actual.length}"
            )
            else -> throw IllegalArgumentException("Size assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun isEmpty(): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> assertTrue(
                actual.isEmpty(),
                "Expected empty collection but was $actual"
            )
            is String -> assertTrue(
                actual.isEmpty(),
                "Expected empty string but was $actual"
            )
            else -> throw IllegalArgumentException("Empty assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun isNotEmpty(): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> assertTrue(
                actual.isNotEmpty(),
                "Expected non-empty collection but was empty"
            )
            is String -> assertTrue(
                actual.isNotEmpty(),
                "Expected non-empty string but was empty"
            )
            else -> throw IllegalArgumentException("Empty assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun startsWith(prefix: String): AssertionBuilder<T> {
        assertTrue(
            actual.toString().startsWith(prefix),
            "Expected $actual to start with $prefix"
        )
        return this
    }

    fun endsWith(suffix: String): AssertionBuilder<T> {
        assertTrue(
            actual.toString().endsWith(suffix),
            "Expected $actual to end with $suffix"
        )
        return this
    }

    fun matches(regex: Regex): AssertionBuilder<T> {
        assertTrue(
            actual.toString().matches(regex),
            "Expected $actual to match regex $regex"
        )
        return this
    }

    // Number specific assertions
    fun isGreaterThan(expected: Number): AssertionBuilder<T> {
        when (actual) {
            is Number -> assertTrue(
                actual.toDouble() > expected.toDouble(),
                "Expected $actual to be greater than $expected"
            )
            else -> throw IllegalArgumentException("Greater than assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun isLessThan(expected: Number): AssertionBuilder<T> {
        when (actual) {
            is Number -> assertTrue(
                actual.toDouble() < expected.toDouble(),
                "Expected $actual to be less than $expected"
            )
            else -> throw IllegalArgumentException("Less than assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun <P> property(prop: KProperty1<T, P>): PropertyAssertionBuilder<T, P> {
        return PropertyAssertionBuilder(actual, prop, this)
    }

    fun satisfiesAll(vararg predicates: (T) -> Boolean): AssertionBuilder<T> {
        predicates.forEachIndexed { index, predicate ->
            assertTrue(
                predicate(actual),
                "Value $actual did not satisfy predicate at index $index"
            )
        }
        return this
    }

    fun transform(transform: (T) -> Any?): AssertionBuilder<Any?> {
        return AssertionBuilder(transform(actual))
    }

    fun <R : Any> isInstanceOf(kClass: KClass<R>): AssertionBuilder<T> {
        assertTrue(
            kClass.isInstance(actual),
            "Expected instance of ${kClass.simpleName} but was ${actual!!::class.simpleName}"
        )
        return this
    }

    fun <R : Any> isA(kClass: KClass<R>): AssertionBuilder<T> {
        assertTrue(
            kClass.isInstance(actual),
            "Expected instance of ${kClass.simpleName} but was ${actual!!::class.simpleName}"
        )
        return this
    }

    inline fun <reified R : Any> isA(): AssertionBuilder<T> = isA(R::class)

    fun isString(): AssertionBuilder<T> = isA(String::class)
    fun isUByte(): AssertionBuilder<T> = isA(UByte::class)
    fun isByte(): AssertionBuilder<T> = isA(Byte::class)
    fun isUInt(): AssertionBuilder<T> = isA(UInt::class)
    fun isInt(): AssertionBuilder<T> = isA(Int::class)
    fun isULong(): AssertionBuilder<T> = isA(ULong::class)
    fun isLong(): AssertionBuilder<T> = isA(Long::class)
    fun isBoolean(): AssertionBuilder<T> = isA(Boolean::class)
    fun isList(): AssertionBuilder<T> = isA(List::class)
    fun isMap(): AssertionBuilder<T> = isA(Map::class)
    fun isSet(): AssertionBuilder<T> = isA(Set::class)

    fun hasToString(expected: String): AssertionBuilder<T> {
        assertEquals(
            expected, actual.toString(),
            "Expected toString() to be '$expected' but was '${actual.toString()}'"
        )
        return this
    }

    fun containsAll(vararg elements: Any?): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> {
                elements.forEach { element ->
                    assertTrue(
                        actual.contains(element),
                        "Expected collection to contain all of ${elements.toList()} but was missing $element"
                    )
                }
            }
            else -> throw IllegalArgumentException("ContainsAll assertion not supported for type ${actual!!::class}")
        }
        return this
    }

    fun containsExactly(vararg elements: Any?): AssertionBuilder<T> {
        when (actual) {
            is Collection<*> -> {
                assertEquals(
                    elements.size, actual.size,
                    "Expected collection to have size ${elements.size} but was ${actual.size}"
                )
                elements.forEachIndexed { index, element ->
                    assertTrue(
                        actual.contains(element),
                        "Expected element at position $index to be $element"
                    )
                }
            }
            else -> throw IllegalArgumentException("ContainsExactly assertion not supported for type ${actual!!::class}")
        }
        return this
    }

}