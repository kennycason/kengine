package com.kengine.test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

inline fun <reified T : Throwable> expectThrows(noinline block: () -> Unit): ThrowsBuilder<T> {
    val builder = ThrowsBuilder<T>()
    builder.verify(block, T::class)
    return builder
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
                assertEquals(
                    expectedMessage, e.message,
                    "Expected exception message to be '$expectedMessage' but was '${e.message}'"
                )
            }

            cause?.let { expectedCause ->
                assertEquals(
                    expectedCause, e.cause,
                    "Expected exception cause to be $expectedCause but was ${e.cause}"
                )
            }
        }
    }
}