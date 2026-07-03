package com.kengine.three

import com.kengine.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

data class OrbitCamera3D(
    val target: Vec3 = Vec3(0.0, 0.0, -4.0),
    val distance: Double = 4.6,
    val yawRadians: Float = 0f,
    val pitchRadians: Float = 0f,
    val fovDegrees: Float = 58f,
    val near: Float = 0.1f,
    val far: Float = 100f
) : Camera3D {
    override fun viewProjection(aspect: Float): Mat4 {
        val eye = position()
        return Mat4.perspective(fovDegrees, aspect, near, far) * Mat4.lookAt(eye, target)
    }

    fun position(): Vec3 {
        val yaw = yawRadians.toDouble()
        val pitch = pitchRadians.toDouble()
        val horizontalDistance = cos(pitch) * distance
        return Vec3(
            target.x + sin(yaw) * horizontalDistance,
            target.y + sin(pitch) * distance,
            target.z + cos(yaw) * horizontalDistance
        )
    }
}
