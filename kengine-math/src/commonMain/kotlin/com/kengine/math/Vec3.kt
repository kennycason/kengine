package com.kengine.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    val lengthSquared: Double
        get() = x * x + y * y + z * z

    val length: Double
        get() = sqrt(lengthSquared)

    fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 {
        return Vec3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    fun normalized(): Vec3 {
        val len = length
        return if (len == 0.0) ZERO else this / len
    }

    fun rotateX(angle: Double): Vec3 = rotateX(this, angle)

    fun rotateY(angle: Double): Vec3 = rotateY(this, angle)

    fun rotateZ(angle: Double): Vec3 = rotateZ(this, angle)

    fun toFloatVector3(): FloatVector3 = FloatVector3(x.toFloat(), y.toFloat(), z.toFloat())

    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    operator fun times(scale: Double): Vec3 = Vec3(x * scale, y * scale, z * scale)

    operator fun div(scale: Double): Vec3 = Vec3(x / scale, y / scale, z / scale)

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)

        fun rotateX(point: Vec3, angle: Double): Vec3 {
            val cosTheta = cos(angle)
            val sinTheta = sin(angle)
            return Vec3(
                point.x,
                point.y * cosTheta - point.z * sinTheta,
                point.y * sinTheta + point.z * cosTheta
            )
        }

        fun rotateY(point: Vec3, angle: Double): Vec3 {
            val cosTheta = cos(angle)
            val sinTheta = sin(angle)
            return Vec3(
                point.x * cosTheta + point.z * sinTheta,
                point.y,
                -point.x * sinTheta + point.z * cosTheta
            )
        }

        fun rotateZ(point: Vec3, angle: Double): Vec3 {
            val cosTheta = cos(angle)
            val sinTheta = sin(angle)
            return Vec3(
                point.x * cosTheta - point.y * sinTheta,
                point.x * sinTheta + point.y * cosTheta,
                point.z
            )
        }

        fun projectTo2D(point: Vec3, screenWidth: Double, screenHeight: Double): Vec2 {
            val perspective = 500.0
            val scale = perspective / (perspective + point.z)
            return Vec2(
                screenWidth / 2 + point.x * scale,
                screenHeight / 2 - point.y * scale
            )
        }
    }
}

operator fun Double.times(vector: Vec3): Vec3 = vector * this
