package com.kengine.three

import com.kengine.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class Mat4(
    val values: FloatArray
) {
    init {
        require(values.size == 16) {
            "Mat4 requires exactly 16 values."
        }
    }

    operator fun times(other: Mat4): Mat4 {
        val out = FloatArray(16)
        for (column in 0 until 4) {
            for (row in 0 until 4) {
                out[column * 4 + row] =
                    values[0 * 4 + row] * other.values[column * 4 + 0] +
                    values[1 * 4 + row] * other.values[column * 4 + 1] +
                    values[2 * 4 + row] * other.values[column * 4 + 2] +
                    values[3 * 4 + row] * other.values[column * 4 + 3]
            }
        }
        return Mat4(out)
    }

    companion object {
        fun identity(): Mat4 {
            return Mat4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun perspective(
            fovDegrees: Float,
            aspect: Float,
            near: Float,
            far: Float
        ): Mat4 {
            val f = 1.0f / tan((fovDegrees * PI.toFloat() / 180.0f) * 0.5f)
            return Mat4(
                floatArrayOf(
                    f / aspect, 0f, 0f, 0f,
                    0f, f, 0f, 0f,
                    0f, 0f, (near + far) / (near - far), -1f,
                    0f, 0f, (2f * near * far) / (near - far), 0f
                )
            )
        }

        fun translation(position: Vec3): Mat4 {
            return Mat4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    position.x.toFloat(), position.y.toFloat(), position.z.toFloat(), 1f
                )
            )
        }

        fun scale(scale: Vec3): Mat4 {
            return Mat4(
                floatArrayOf(
                    scale.x.toFloat(), 0f, 0f, 0f,
                    0f, scale.y.toFloat(), 0f, 0f,
                    0f, 0f, scale.z.toFloat(), 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun rotationX(radians: Float): Mat4 {
            val c = cos(radians)
            val s = sin(radians)
            return Mat4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, c, s, 0f,
                    0f, -s, c, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun rotationY(radians: Float): Mat4 {
            val c = cos(radians)
            val s = sin(radians)
            return Mat4(
                floatArrayOf(
                    c, 0f, -s, 0f,
                    0f, 1f, 0f, 0f,
                    s, 0f, c, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun rotationZ(radians: Float): Mat4 {
            val c = cos(radians)
            val s = sin(radians)
            return Mat4(
                floatArrayOf(
                    c, s, 0f, 0f,
                    -s, c, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }

        fun lookAt(
            eye: Vec3,
            target: Vec3,
            up: Vec3 = Vec3(0.0, 1.0, 0.0)
        ): Mat4 {
            val forward = normalize(
                Vec3(
                    eye.x - target.x,
                    eye.y - target.y,
                    eye.z - target.z
                )
            )
            val right = normalize(cross(up, forward))
            val cameraUp = cross(forward, right)

            return Mat4(
                floatArrayOf(
                    right.x.toFloat(), cameraUp.x.toFloat(), forward.x.toFloat(), 0f,
                    right.y.toFloat(), cameraUp.y.toFloat(), forward.y.toFloat(), 0f,
                    right.z.toFloat(), cameraUp.z.toFloat(), forward.z.toFloat(), 0f,
                    -dot(right, eye).toFloat(), -dot(cameraUp, eye).toFloat(), -dot(forward, eye).toFloat(), 1f
                )
            )
        }

        private fun cross(a: Vec3, b: Vec3): Vec3 {
            return Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
            )
        }

        private fun dot(a: Vec3, b: Vec3): Double {
            return a.x * b.x + a.y * b.y + a.z * b.z
        }

        private fun normalize(value: Vec3): Vec3 {
            val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
            if (length == 0.0) {
                return Vec3(0.0, 0.0, 0.0)
            }
            return Vec3(value.x / length, value.y / length, value.z / length)
        }
    }
}
