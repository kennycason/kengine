package com.kengine.three

import com.kengine.math.Vec3

data class Transform3D(
    val position: Vec3 = Vec3(0.0, 0.0, 0.0),
    val rotation: Vec3 = Vec3(0.0, 0.0, 0.0),
    val scale: Vec3 = Vec3(1.0, 1.0, 1.0)
) {
    fun matrix(): Mat4 {
        return Mat4.translation(position) *
            Mat4.rotationZ(rotation.z.toFloat()) *
            Mat4.rotationY(rotation.y.toFloat()) *
            Mat4.rotationX(rotation.x.toFloat()) *
            Mat4.scale(scale)
    }
}
