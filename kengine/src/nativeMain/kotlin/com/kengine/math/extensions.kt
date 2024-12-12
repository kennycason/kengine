// squares
val Float.squared: Float
    get() = this * this

val Double.squared: Double
    get() = this * this

val Short.squared: Int
    get() = this * this

val Int.squared: Int
    get() = this * this

val Long.squared: Long
    get() = this * this

val UShort.squared: UInt
    get() = this.toUInt() * this.toUInt()

val UInt.squared: UInt
    get() = this * this

val ULong.squared: ULong
    get() = this * this

val Byte.squared: Int
    get() = this * this

val Number.squared: Double
    get() = this.toDouble() * this.toDouble()


// cubes
val Float.cubed: Float
    get() = this * this * this

val Double.cubed: Double
    get() = this * this * this

val Short.cubed: Int
    get() = this * this * this

val Int.cubed: Int
    get() = this * this * this

val Long.cubed: Long
    get() = this * this * this

val UShort.cubed: UInt
    get() = this.toUInt() * this.toUInt() * this.toUInt()

val UInt.cubed: UInt
    get() = this * this * this

val ULong.cubed: ULong
    get() = this * this * this

val Byte.cubed: Int
    get() = this * this * this

val Number.cubed: Double
    get() = this.toDouble() * this.toDouble() * this.toDouble()


// roots
val Float.root: Float
    get() = kotlin.math.sqrt(this)

val Double.root: Double
    get() = kotlin.math.sqrt(this)

val Number.root: Double
    get() = kotlin.math.sqrt(this.toDouble())


// even/odd
val Byte.isEven: Boolean
    get() = this % 2 == 0

val Byte.isOdd: Boolean
    get() = this % 2 != 0

val Short.isEven: Boolean
    get() = this % 2 == 0

val Short.isOdd: Boolean
    get() = this % 2 != 0

val Int.isEven: Boolean
    get() = this % 2 == 0

val Int.isOdd: Boolean
    get() = this % 2 != 0

val Long.isEven: Boolean
    get() = this % 2 == 0L

val Long.isOdd: Boolean
    get() = this % 2 != 0L

val UInt.isEven: Boolean
    get() = this % 2u == 0u

val UInt.isOdd: Boolean
    get() = this % 2u != 0u

val UShort.isEven: Boolean
    get() = this % 2u == 0u

val UShort.isOdd: Boolean
    get() = this % 2u != 0u

val ULong.isEven: Boolean
    get() = this % 2uL == 0uL

val ULong.isOdd: Boolean
    get() = this % 2uL != 0uL


// clamp
fun Double.clamp(min: Double, max: Double): Double = when {
    this < min -> min
    this > max -> max
    else -> this
}

fun Long.clamp(min: Long, max: Long): Long = when {
    this < min -> min
    this > max -> max
    else -> this
}

fun Int.clamp(min: Int, max: Int): Int = when {
    this < min -> min
    this > max -> max
    else -> this
}

fun UInt.clamp(min: UInt, max: UInt): UInt = when {
    this < min -> min
    this > max -> max
    else -> this
}

fun ULong.clamp(min: ULong, max: ULong): ULong = when {
    this < min -> min
    this > max -> max
    else -> this
}


// factorial
val Short.factorial: Long
    get() = if (this < 0) throw IllegalArgumentException("Negative numbers don't have factorials")
    else (2..this.toInt()).fold(1L) { acc, i -> acc * i }

val Int.factorial: Long
    get() = if (this < 0) throw IllegalArgumentException("Negative numbers don't have factorials")
    else if (this == 0 || this == 1) 1
    else (2..this).fold(1L) { acc, i -> acc * i }

val UInt.factorial: ULong
    get() = if (this == 0u || this == 1u) 1uL
    else (2u..this).fold(1uL) { acc, i -> acc * i.toULong() }

val Byte.factorial: Long
    get() = this.toInt().factorial


// reciprocal
val Float.reciprocal: Float
    get() = if (this == 0f) throw ArithmeticException("Cannot divide by zero") else 1f / this

val Double.reciprocal: Double
    get() = if (this == 0.0) throw ArithmeticException("Cannot divide by zero") else 1.0 / this


// abs
val Double.abs: Double
    get() = if (this < 0.0) -this else this

val Float.abs: Float
    get() = if (this < 0f) -this else this

val Byte.abs: Int
    get() = if (this < 0) -this else this.toInt()

val Short.abs: Int
    get() = if (this < 0) -this else this.toInt()

val Int.abs: Int
    get() = if (this < 0) -this else this

val Long.abs: Long
    get() = if (this < 0L) -this else this