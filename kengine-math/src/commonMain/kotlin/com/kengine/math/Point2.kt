package com.kengine.math

data class Point2(
    val x: Int,
    val y: Int
) {
    operator fun plus(other: Point2): Point2 = Point2(x + other.x, y + other.y)

    operator fun minus(other: Point2): Point2 = Point2(x - other.x, y - other.y)

    fun toVec2(): Vec2 = Vec2(x.toDouble(), y.toDouble())

    fun toVector2(): Vector2 = toVec2()
}
