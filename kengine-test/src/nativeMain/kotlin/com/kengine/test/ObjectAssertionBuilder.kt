package com.kengine.test

import kotlin.reflect.KProperty1
import kotlin.test.assertTrue

fun <T : Any> expectObject(actual: T, block: ObjectAssertionBuilder<T>.() -> Unit) {
    ObjectAssertionBuilder(actual).apply(block).verify()
}

class ObjectAssertionBuilder<T : Any>(private val actual: T) {
    private val assertions = mutableListOf<(T) -> Unit>()

    fun <P> property(prop: KProperty1<T, P>, assertion: (P) -> Boolean, message: String? = null) {
        assertions.add { obj ->
            val value = prop.get(obj)
            assertTrue(
                assertion(value),
                message ?: "Property ${prop.name} with value $value did not satisfy the assertion"
            )
        }
    }

    fun verify() {
        assertions.forEach { it(actual) }
    }
}