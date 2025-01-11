package com.kengine.math

import kotlin.math.cos
import kotlin.math.sin

data class Vec3(val x: Double, val y: Double, val z: Double) {

    companion object {
        // Rotate around X-axis
        fun rotateX(point: Vec3, angle: Double): Vec3 {
            val cosTheta = cos(angle)
            val sinTheta = sin(angle)
            return Vec3(
                point.x,
                point.y * cosTheta - point.z * sinTheta,
                point.y * sinTheta + point.z * cosTheta
            )
        }

        // Rotate around Y-axis
        fun rotateY(point: Vec3, angle: Double): Vec3 {
            val cosTheta = cos(angle)
            val sinTheta = sin(angle)
            return Vec3(
                point.x * cosTheta + point.z * sinTheta,
                point.y,
                -point.x * sinTheta + point.z * cosTheta
            )
        }

        // Rotate around Z-axis
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
            val perspective = 500.0 // Adjust for zoom
            val scale = perspective / (perspective + point.z)
            return Vec2(
                screenWidth / 2 + point.x * scale,
                screenHeight / 2 - point.y * scale
            )
        }
    }
}
