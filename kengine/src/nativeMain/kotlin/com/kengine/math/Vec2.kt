package com.kengine.math

import squared
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
        return sqrt(x.squared + y.squared)
    }
    fun normalized(): Vec2 {
        val mag = this.magnitude()
        return if (mag == 0.0) Vec2(0.0, 0.0) else Vec2(this.x / mag, this.y / mag)
    }

    fun linearInterpolate(target: Vec2, alpha: Double): Vec2 {
        return Vec2(
            x + (target.x - x) * alpha,
            y + (target.y - y) * alpha
        )
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
    operator fun plusAssign(other: Vec2) {
        this.x += other.x
        this.y += other.y
    }
    operator fun minusAssign(other: Vec2) {
        this.x -= other.x
        this.y -= other.y
    }
    operator fun timesAssign(other: Vec2) {
        this.x *= other.x
        this.y *= other.y
    }
    operator fun divAssign(other: Vec2) {
        this.x /= other.x
        this.y /= other.y
    }
    operator fun plusAssign(other: Double) {
        this.x += other
        this.y += other
    }
    operator fun minusAssign(other: Double) {
        this.x -= other
        this.y -= other
    }
    operator fun timesAssign(other: Double) {
        this.x *= other
        this.y *= other
    }
    operator fun divAssign(other: Double) {
        this.x /= other
        this.y /= other
    }
}
