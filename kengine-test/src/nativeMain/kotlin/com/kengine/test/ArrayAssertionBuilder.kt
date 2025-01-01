package com.kengine.test

fun <T> expectArray(actual: List<T>): ArrayAssertionBuilder<T> {
    return ArrayAssertionBuilder(actual)
}


fun <T> expectArray(
    actual: List<T>,
    block: ArrayAssertionBuilder<T>.() -> Unit
) {
    ArrayAssertionBuilder(actual).apply(block).verify()
}

class ArrayAssertionBuilder<T>(private val actual: List<T>) {
    private val assertions = mutableListOf<() -> Unit>()

    // Verify all added assertions
    fun verify() {
        assertions.forEach { it() } // Executes all collected assertions
    }

    // Assertions for the entire array
    fun hasSize(expected: Int): ArrayAssertionBuilder<T> {
        assertions.add {
            expectThat(actual.size)
                .isEqualTo(expected) { "Expected size $expected but was ${actual.size}" }
        }
        return this
    }

    fun isNotEmpty(): ArrayAssertionBuilder<T> {
        assertions.add {
            expectThat(actual).isNotEmpty() { "Expected array to be non-empty but was empty" }
        }
        return this
    }

    fun isEmpty(): ArrayAssertionBuilder<T> {
        assertions.add {
            expectThat(actual).isEmpty() { "Expected array to be empty but was $actual" }
        }
        return this
    }

    // Access individual items
    fun item(index: Int): AssertionBuilder<T> {
        require(index in actual.indices) { "Index $index is out of bounds for size ${actual.size}" }
        // No need to add assertions explicitly; they evaluate directly via AssertionBuilder
        return AssertionBuilder(actual[index])
    }

    fun first(): AssertionBuilder<T> {
        require(actual.isNotEmpty()) { "Array is empty" }
        return AssertionBuilder(actual.first())
    }

    fun last(): AssertionBuilder<T> {
        require(actual.isNotEmpty()) { "Array is empty" }
        return AssertionBuilder(actual.last())
    }
}
