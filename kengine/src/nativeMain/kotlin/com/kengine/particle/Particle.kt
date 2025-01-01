package com.kengine.particle

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.math.Vec2
import com.kengine.time.getClockContext

class Particle(
    var position: Vec2,
    var velocity: Vec2,
    var color: Color = Color(0xFFu, 0x0u, 0x0u, 0xFFu), // Red by default
    var lifetime: Double = 1.0
) {
    var age = 0.0

    fun update() {
        val delta = getClockContext().deltaTimeSec
        age += delta
        position.set(position + (velocity * delta))
    }

    fun draw() {
        if (age < lifetime) {
            useGeometryContext {
                drawRectangle(position.x.toInt(), position.y.toInt(), 10, 10, color.toUInt()) // 10x10 particle
            }
        }
    }
}
