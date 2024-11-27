package com.kengine.entity

import com.kengine.math.Vec2

abstract class Entity(
    val p: Vec2 = Vec2(),
    val v: Vec2 = Vec2(),
    val a: Vec2 = Vec2(),
    val width: Int,
    val height: Int,
    val active: Boolean = true
) {
    abstract fun update(elapsedSeconds: Double)
    abstract fun draw(elapsedSeconds: Double)
    open fun overlaps(entity: Entity): Boolean = false
    abstract fun cleanup()
}
