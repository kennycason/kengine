package com.kengine.math

import kotlin.math.sqrt

data class Vec2(
    var x: Double = 0.0,
    var y: Double = 0.0
) {
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
    fun magnitude(): Double {
        return sqrt(x * x + y * y)
    }
    fun normalized(): Vec2 {
        val mag = this.magnitude()
        return if (mag == 0.0) Vec2(0.0, 0.0) else Vec2(this.x / mag, this.y / mag)
    }

    operator fun plus(other: Vec2): Vec2 {
        return Vec2(this.x + other.x, this.y + other.y)
    }
    operator fun minus(other: Vec2): Vec2 {
        return Vec2(this.x - other.x, this.y - other.y)
    }
    operator fun times(other: Vec2): Vec2 {
        return Vec2(this.x * other.x, this.y * other.y)
    }
    operator fun div(other: Vec2): Vec2 {
        return Vec2(this.x / other.x, this.y / other.y)
    }
    operator fun plus(other: Double): Vec2 {
        return Vec2(this.x + other, this.y + other)
    }
    operator fun minus(other: Double): Vec2 {
        return Vec2(this.x - other, this.y - other)
    }
    operator fun times(other: Double): Vec2 {
        return Vec2(this.x * other, this.y * other)
    }
    operator fun div(other: Double): Vec2 {
        return Vec2(this.x / other, this.y / other)
    }
    operator fun plusAssign(other: Vec2): Unit {
        this.x += other.x
        this.y += other.y
    }
    operator fun minusAssign(other: Vec2): Unit {
        this.x -= other.x
        this.y -= other.y
    }
    operator fun timesAssign(other: Vec2): Unit {
        this.x *= other.x
        this.y *= other.y
    }
    operator fun divAssign(other: Vec2): Unit {
        this.x /= other.x
        this.y /= other.y
    }
    operator fun plusAssign(other: Double): Unit {
        this.x += other
        this.y += other
    }
    operator fun minusAssign(other: Double): Unit {
        this.x -= other
        this.y -= other
    }
    operator fun timesAssign(other: Double): Unit {
        this.x *= other
        this.y *= other
    }
    operator fun divAssign(other: Double): Unit {
        this.x /= other
        this.y /= other
    }
}