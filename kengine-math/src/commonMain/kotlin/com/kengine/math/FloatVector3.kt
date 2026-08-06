package com.kengine.math

import kotlin.math.sqrt

data class FloatVector3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    val lengthSquared: Float
        get() = x * x + y * y + z * z

    val length: Float
        get() = sqrt(lengthSquared)

    fun dot(other: FloatVector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: FloatVector3): FloatVector3 {
        return FloatVector3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    fun normalized(): FloatVector3 {
        val len = length
        return if (len == 0.0f) ZERO else this / len
    }

    fun toVec3(): Vec3 = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

    fun toVector3(): Vector3 = toVec3()

    operator fun plus(other: FloatVector3): FloatVector3 = FloatVector3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: FloatVector3): FloatVector3 = FloatVector3(x - other.x, y - other.y, z - other.z)

    operator fun unaryMinus(): FloatVector3 = FloatVector3(-x, -y, -z)

    operator fun times(scale: Float): FloatVector3 = FloatVector3(x * scale, y * scale, z * scale)

    operator fun div(scale: Float): FloatVector3 = FloatVector3(x / scale, y / scale, z / scale)

    companion object {
        val ZERO = FloatVector3(0.0f, 0.0f, 0.0f)
    }
}

operator fun Float.times(vector: FloatVector3): FloatVector3 = vector * this
