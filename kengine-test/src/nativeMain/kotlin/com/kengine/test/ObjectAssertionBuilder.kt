package com.kengine.test

import kotlin.reflect.KProperty1

fun <T : Any> expectObject(actual: T): AssertionBuilder<T> = AssertionBuilder(actual)

fun <T : Any> expectObject(
    actual: T,
    block: ObjectAssertionBuilder<T>.() -> Unit
) {
    ObjectAssertionBuilder(actual).apply(block).verify()
}

class ObjectAssertionBuilder<T : Any>(private val actual: T) {
    private val assertions = mutableListOf<() -> Unit>()

    // property Assertion Builder, delegates to AssertionBuilder
    inner class PropertyAssertionBuilder<P>(
        propertyName: String,
        value: P
    ) : AssertionBuilder<P>(value) {
        fun addAssertion(assertion: () -> Unit) {
            assertions.add(assertion)
        }
    }

    /**
     * Delegates property assertions to AssertionBuilder, enabling fluent chaining.
     */
    fun <P> property(prop: KProperty1<T, P>): PropertyAssertionBuilder<P> {
        val value = prop.get(actual)

        // automatically verifies assertions when verify() is called
        return PropertyAssertionBuilder(prop.name, value)
    }

    /**
     * Verify all assertions added during configuration.
     */
    fun verify() {
        assertions.forEach { it() }
    }
}
