package com.kengine.particle

import com.kengine.entity.Actor
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.math.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SacredGeometryOscillation(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val numShapes: Int = 6
) : Actor {
    private val centerX = x + width / 2.0
    private val centerY = y + height / 2.0
    private var time = 0.0
    private var frequency = 440.0
    private var detune = 0.0
    private val rainbow = Color.rainbow(256)
    private val maxShapes = numShapes.coerceIn(3, 5) // Limit recursion depth

    fun setFrequency(newFrequency: Double) {
        frequency = newFrequency
    }

    fun setDetune(newDetune: Double) {
        detune = newDetune
    }

    override fun update() {
        time += 0.01
    }

    override fun draw() {
        drawSacredGeometry(
            Vec2(centerX, centerY),
            initialRadius = width / 4.0,
            layer = 0
        )
    }

    private fun drawSacredGeometry(center: Vec2, initialRadius: Double, layer: Int) {
        if (layer >= maxShapes || initialRadius < 10.0) return // Skip too-small shapes or deep recursion

        val dynamicRadius = initialRadius * (1 + 0.1 * sin(time + layer * detune * 0.01))
        val numPoints = 6 + (layer % 2) * 4 // Fewer points for deeper layers

        useGeometryContext {
            for (i in 0 until numPoints) {
                val angle = 2 * PI * i / numPoints
                val x = center.x + dynamicRadius * cos(angle)
                val y = center.y + dynamicRadius * sin(angle)

                // Calculate color dynamically based on layer and point index
                val colorIndex = ((time * 50 + layer * 20 + i * 10) % rainbow.size).toInt()
                val color = rainbow[colorIndex]

                drawLine(center.x, center.y, x, y, color.r, color.g, color.b, 0xFFu)

                // Skip every other branch for layers > 2 to reduce density
                if (layer > 2 && i % 2 == 0) continue

                // Recursively draw smaller shapes
                drawSacredGeometry(Vec2(x, y), dynamicRadius / 2.0, layer + 1)
            }
        }
    }
}
