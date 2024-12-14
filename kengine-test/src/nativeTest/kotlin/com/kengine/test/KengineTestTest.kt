package com.kengine.test

import kotlin.test.Test

class KengineTestTest {

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
    fun `test exceptions`() {
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
        }.withMessage("bad value")
            .withCause(cause)
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

    @Test
    fun `test type checking`() {
        data class User(val name: String)

        val user = User("John")

        expectThat(user).isA<User>()

        expectThat("test").isA<String>()
        expectThat(42).isA<Int>()
        expectThat(listOf(1, 2, 3)).isA<List<*>>()

        // specific helpers
        expectThat(true).isBoolean()
        expectThat("test").isString()
        expectThat(42.toByte()).isByte()
        expectThat(42.toUByte()).isUByte()
        expectThat(42).isInt()
        expectThat(42u).isUInt()
        expectThat(42L).isLong()
        expectThat(42uL).isULong()
        expectThat(listOf(1, 2, 3)).isList()
        expectThat(mapOf(1 to 2)).isMap()
        expectThat(setOf(1, 2, 3)).isSet()
    }

    @Test
    fun `test transform and chain`() {
        expectThat("123")
            .transform { it.toInt() }
            .isGreaterThan(100)
    }

    @Test
    fun `test property assertions`() {
        data class User(val name: String, val age: Int)

        val user = User("John", 25)
        expectThat(user)
            .property(User::name).isEqualTo("John")
            .property(User::age).satisfies { it > 18 }
    }

    @Test
    fun `test object assertions`() {
        data class User(val name: String, val age: Int)

        val user = User("John", 25)

        expectObject(user) {
            property(User::name, { it == "John" }, "Name should be John")
            property(User::age, { it > 18 }, "User should be an adult")
        }
    }

    @Test
    fun `test collections assertions`() {
        expectThat(listOf(1, 2, 3, 4))
            .containsAll(1, 2)
            .containsExactly(1, 2, 3, 4)
    }

    @Test
    fun `test satisfyAll with multiple predicates`() {
        expectThat(10).satisfiesAll(
            { it > 0 },
            { it % 2 == 0 },
            { it <= 10 }
        )
    }

    @Test
    fun `test hasToString`() {
        data class User(val name: String) {
            override fun toString() = "Hello $name"
        }

        val user = User(name = "Kenny")
        expectThat(user).hasToString("Hello Kenny")
    }

}
