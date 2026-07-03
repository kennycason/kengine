package com.kengine.three

import com.kengine.math.Vec3

data class PerspectiveCamera(
    val position: Vec3 = Vec3(0.0, 0.0, 0.0),
    val fovDegrees: Float = 60f,
    val near: Float = 0.1f,
    val far: Float = 100f
) {
    fun viewProjection(aspect: Float): Mat4 {
        val projection = Mat4.perspective(fovDegrees, aspect, near, far)
        val view = Mat4.translation(
            Vec3(
                -position.x,
                -position.y,
                -position.z
            )
        )
        return projection * view
    }
}
