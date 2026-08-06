package com.kengine.math

data class Point3(
    val x: Int,
    val y: Int,
    val z: Int
) {
    operator fun plus(other: Point3): Point3 = Point3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Point3): Point3 = Point3(x - other.x, y - other.y, z - other.z)

    fun toVec3(): Vec3 = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

    fun toVector3(): Vector3 = toVec3()
}
