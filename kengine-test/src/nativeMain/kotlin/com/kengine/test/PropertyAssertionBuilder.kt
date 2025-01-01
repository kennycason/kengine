package com.kengine.test

import kotlin.reflect.KProperty1

class PropertyAssertionBuilder<T, P>(
    private val obj: T,
    private val property: KProperty1<T, P>,
    private val parentBuilder: AssertionBuilder<T>
) : AssertionBuilder<P>(property.get(obj)) {

    /**
     * Allows directly chaining assertions on the property.
     */
    fun endProperty(): AssertionBuilder<T> {
        return parentBuilder
    }
}
