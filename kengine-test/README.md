# Kengine Test

A lightweight, fluent assertion library for Kotlin Native that makes your tests more readable and expressive.

## Features
- Fluent assertion API
- Type-safe property assertions
- Exception testing
- Collection assertions
- Type checking
- Object assertions

## Installation

Add the Kengine Test dependency to your project (coming soon).

## Usage

### Basic Assertions

```kotlin
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
```

### Testing Exceptions

```kotlin
// Basic exception type checking
expectThrows<IllegalArgumentException> {
    throw IllegalArgumentException("bad value")
}

// Check exception message and cause
val cause = RuntimeException("root cause")
expectThrows<IllegalArgumentException> {
    throw IllegalArgumentException("bad value", cause)
}
    .withMessage("bad value")
    .withCause(cause)
```

### Type Checking

```kotlin
// Generic type checking
expectThat("test").isA<String>()
expectThat(42).isA<Int>()
expectThat(listOf(1,2,3)).isA<List<*>>()

// Specific type helpers
expectThat(true).isBoolean()
expectThat("test").isString()
expectThat(42).isInt()
expectThat(42u).isUInt()
expectThat(42L).isLong()
expectThat(listOf(1,2,3)).isList()
```

### Property Assertions

```kotlin
import java.awt.Color

data class User(val name: String, val age: Int)
val user = User("John", 25)

// Chained property assertions
expectThat(user)
    .property(User::name).isEqualTo("John") { "Name should be John" }
    .property(User::age).satisfies { it > 18 }

// Object-style property assertions
expectObject(user) {
    property(User::name).isEqualTo("John") { "Name should be John" }
    property(User::age).isGreaterThan(18) { "User should be an adult" }
}

// Array of Objects
val users = listOf(
    User("John", 25),
    User("Bill", 41)
)

expectArray(users) {
    first().property(User::name).isEqualTo("John") { "Name should be John" }
    last().property(User::age).isGreaterThan(18) { "User should be an adult" }
    item(0).property(User::age).isEqualTo(25)
}

expectArray(users).isNotEmpty()

expectArray(users)
    .first().property(User::name).isEqualTo("John")
```

### Collection Assertions

```kotlin
expectThat(listOf(1, 2, 3, 4))
    .containsAll(1, 2)
    .containsExactly(1, 2, 3, 4)

// Multiple predicates
expectThat(10).satisfiesAll(
    { it > 0 },
    { it % 2 == 0 },
    { it <= 10 }
)
```

### Value Transformations

```kotlin
expectThat("123")
    .transform { it.toInt() }
    .isGreaterThan(100)
```

### Expression-Style Assertions

```kotlin
val numbers = listOf(1, 2, 3)
expectThat(numbers) {
    isNotEmpty()
    hasSize(3)
    contains(2)
}
```
