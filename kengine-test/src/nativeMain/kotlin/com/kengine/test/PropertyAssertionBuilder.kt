package com.kengine.test

import kotlin.reflect.KProperty1
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyAssertionBuilder<T, P>(
    private val obj: T,
    private val property: KProperty1<T, P>,
    private val parentBuilder: AssertionBuilder<T>
) {
    fun isEqualTo(expected: P): AssertionBuilder<T> {
        assertEquals(
            expected, property.get(obj),
            "Expected property ${property.name} to be equal to $expected but was ${property.get(obj)}"
        )
        return parentBuilder
    }

    fun satisfies(message: String? = null, predicate: (P) -> Boolean): AssertionBuilder<T> {
        assertTrue(
            predicate(property.get(obj)),
            message ?: "Property ${property.name} with value ${property.get(obj)} did not satisfy the predicate"
        )
        return parentBuilder
    }
}