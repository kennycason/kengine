package com.kengine.math

import kotlin.math.sqrt

data class IntVec2(
    var x: Int = 0,
    var y: Int = 0
) {
    fun set(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun set(v: Int) {
        this.x = v
        this.y = v
    }

    fun set(v: IntVec2) {
        this.x = v.x
        this.y = v.y
    }

    fun magnitude(): Double = sqrt((x * x + y * y).toDouble())

    fun normalized(): Vec2 = Vec2(x.toDouble(), y.toDouble()).normalized()

    operator fun plus(other: IntVec2): IntVec2 = IntVec2(x + other.x, y + other.y)

    operator fun minus(other: IntVec2): IntVec2 = IntVec2(x - other.x, y - other.y)

    operator fun times(other: IntVec2): IntVec2 = IntVec2(x * other.x, y * other.y)

    operator fun div(other: IntVec2): IntVec2 = IntVec2(x / other.x, y / other.y)

    operator fun plus(other: Int): IntVec2 = IntVec2(x + other, y + other)

    operator fun minus(other: Int): IntVec2 = IntVec2(x - other, y - other)

    operator fun times(other: Int): IntVec2 = IntVec2(x * other, y * other)

    operator fun div(other: Int): IntVec2 = IntVec2(x / other, y / other)

    operator fun plusAssign(other: IntVec2) {
        x += other.x
        y += other.y
    }

    operator fun minusAssign(other: IntVec2) {
        x -= other.x
        y -= other.y
    }

    operator fun timesAssign(other: IntVec2) {
        x *= other.x
        y *= other.y
    }

    operator fun divAssign(other: IntVec2) {
        x /= other.x
        y /= other.y
    }

    operator fun plusAssign(other: Int) {
        x += other
        y += other
    }

    operator fun minusAssign(other: Int) {
        x -= other
        y -= other
    }

    operator fun timesAssign(other: Int) {
        x *= other
        y *= other
    }

    operator fun divAssign(other: Int) {
        x /= other
        y /= other
    }
}
