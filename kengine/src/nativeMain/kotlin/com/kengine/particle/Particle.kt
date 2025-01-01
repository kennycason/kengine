package com.kengine.particle

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.math.Vec2
import com.kengine.time.getClockContext

class Particle(
    var position: Vec2,
    var velocity: Vec2,
    var color: Color = Color.red,
    var lifetime: Double = 1.0,
    var size: Double = 10.0,
    var rotation: Double = 0.0,
    var rotationSpeed: Double = 0.0,
    var scaleSpeed: Double = 0.0,
    var gravity: Vec2 = Vec2(0.0, 0.0),
    var behaviors: List<(Particle) -> Unit> = listOf()
) {
    var age = 0.0

    fun update() {
        val delta = getClockContext().deltaTimeSec
        age += delta

        // apply physics
        velocity.plusAssign (gravity * delta)
        position.plusAssign(velocity * delta)
        rotation += rotationSpeed * delta
        size += scaleSpeed * delta

        // apply behaviors
        behaviors.forEach { it(this) }
    }

    fun draw() {
        if (age < lifetime) {
            useGeometryContext {
                fillRectangle(
                    position.x.toInt(), position.y.toInt(),
                    size.toInt(), size.toInt(),
                 //   rotation,
                    color.r, color.g, color.b, color.a
                )
            }
        }
    }
}
