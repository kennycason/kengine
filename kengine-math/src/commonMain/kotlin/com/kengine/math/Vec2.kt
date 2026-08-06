package com.kengine.math

import kotlin.math.sqrt

data class Vec2(
    var x: Double = 0.0,
    var y: Double = 0.0
) {
    val lengthSquared: Double
        get() = x * x + y * y

    val length: Double
        get() = sqrt(lengthSquared)

    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    fun set(v: Double) {
        this.x = v
        this.y = v
    }

    fun set(v: Vec2) {
        this.x = v.x
        this.y = v.y
    }

    fun magnitude(): Double = length

    fun dot(other: Vec2): Double = x * other.x + y * other.y

    fun normalized(): Vec2 {
        val len = length
        return if (len == 0.0) Vec2() else this / len
    }

    fun linearInterpolate(target: Vec2, alpha: Double): Vec2 {
        return Vec2(
            x + (target.x - x) * alpha,
            y + (target.y - y) * alpha
        )
    }

    fun toFloatVector2(): FloatVector2 = FloatVector2(x.toFloat(), y.toFloat())

    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    operator fun times(other: Vec2): Vec2 = Vec2(x * other.x, y * other.y)

    operator fun div(other: Vec2): Vec2 = Vec2(x / other.x, y / other.y)

    operator fun plus(other: Double): Vec2 = Vec2(x + other, y + other)

    operator fun minus(other: Double): Vec2 = Vec2(x - other, y - other)

    operator fun times(other: Double): Vec2 = Vec2(x * other, y * other)

    operator fun div(other: Double): Vec2 = Vec2(x / other, y / other)

    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    operator fun plusAssign(other: Vec2) {
        x += other.x
        y += other.y
    }

    operator fun minusAssign(other: Vec2) {
        x -= other.x
        y -= other.y
    }

    operator fun timesAssign(other: Vec2) {
        x *= other.x
        y *= other.y
    }

    operator fun divAssign(other: Vec2) {
        x /= other.x
        y /= other.y
    }

    operator fun plusAssign(other: Double) {
        x += other
        y += other
    }

    operator fun minusAssign(other: Double) {
        x -= other
        y -= other
    }

    operator fun timesAssign(other: Double) {
        x *= other
        y *= other
    }

    operator fun divAssign(other: Double) {
        x /= other
        y /= other
    }

    companion object {
        val ZERO: Vec2
            get() = Vec2(0.0, 0.0)
    }
}

operator fun Double.times(vector: Vec2): Vec2 = vector * this
