package com.kengine.math

import kotlin.math.sqrt

data class FloatVector2(
    val x: Float,
    val y: Float
) {
    val lengthSquared: Float
        get() = x * x + y * y

    val length: Float
        get() = sqrt(lengthSquared)

    fun dot(other: FloatVector2): Float = x * other.x + y * other.y

    fun normalized(): FloatVector2 {
        val len = length
        return if (len == 0.0f) ZERO else this / len
    }

    fun toVec2(): Vec2 = Vec2(x.toDouble(), y.toDouble())

    fun toVector2(): Vector2 = toVec2()

    operator fun plus(other: FloatVector2): FloatVector2 = FloatVector2(x + other.x, y + other.y)

    operator fun minus(other: FloatVector2): FloatVector2 = FloatVector2(x - other.x, y - other.y)

    operator fun unaryMinus(): FloatVector2 = FloatVector2(-x, -y)

    operator fun times(scale: Float): FloatVector2 = FloatVector2(x * scale, y * scale)

    operator fun div(scale: Float): FloatVector2 = FloatVector2(x / scale, y / scale)

    companion object {
        val ZERO = FloatVector2(0.0f, 0.0f)
    }
}

operator fun Float.times(vector: FloatVector2): FloatVector2 = vector * this
