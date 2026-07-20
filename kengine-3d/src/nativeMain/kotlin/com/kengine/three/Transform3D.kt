package com.kengine.three

import com.kengine.math.Vec3

data class Transform3D(
    val position: Vec3 = Vec3(0.0, 0.0, 0.0),
    val rotation: Vec3 = Vec3(0.0, 0.0, 0.0),
    val scale: Vec3 = Vec3(1.0, 1.0, 1.0)
) {
    operator fun times(child: Transform3D): Transform3D {
        return Transform3D(
            position = transformPoint(child.position),
            rotation = Vec3(
                rotation.x + child.rotation.x,
                rotation.y + child.rotation.y,
                rotation.z + child.rotation.z
            ),
            scale = Vec3(
                scale.x * child.scale.x,
                scale.y * child.scale.y,
                scale.z * child.scale.z
            )
        )
    }

    fun transformPoint(point: Vec3): Vec3 {
        val scaled = Vec3(
            point.x * scale.x,
            point.y * scale.y,
            point.z * scale.z
        )
        val rotatedX = Vec3.rotateX(scaled, rotation.x)
        val rotatedY = Vec3.rotateY(rotatedX, rotation.y)
        val rotatedZ = Vec3.rotateZ(rotatedY, rotation.z)
        return Vec3(
            position.x + rotatedZ.x,
            position.y + rotatedZ.y,
            position.z + rotatedZ.z
        )
    }

    fun matrix(): Mat4 {
        return Mat4.translation(position) *
            Mat4.rotationZ(rotation.z.toFloat()) *
            Mat4.rotationY(rotation.y.toFloat()) *
            Mat4.rotationX(rotation.x.toFloat()) *
            Mat4.scale(scale)
    }

    companion object {
        fun positionYaw(
            position: Vec3,
            yawRadians: Double,
            yOffset: Double = 0.0,
            yawOffsetRadians: Double = 0.0,
            scale: Vec3 = Vec3(1.0, 1.0, 1.0)
        ): Transform3D {
            return Transform3D(
                position = Vec3(position.x, position.y + yOffset, position.z),
                rotation = Vec3(0.0, yawRadians + yawOffsetRadians, 0.0),
                scale = scale
            )
        }
    }
}
