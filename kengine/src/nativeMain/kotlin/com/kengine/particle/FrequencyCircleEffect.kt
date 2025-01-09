package com.kengine.particle

import com.kengine.entity.Actor
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.time.getCurrentMilliseconds

class FrequencyCircleEffect(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private var frequency: Double = 0.0
) : Actor {

    override fun update() {
    }

    override fun draw() {
        val timeMs = getCurrentMilliseconds()

        // Combine freq + time in a playful way, e.g. sine wave:
        // wave ∈ [-1..1], shift & scale for a nice offset
        val wave = kotlin.math.sin(frequency * 0.011 + (timeMs * 0.001))
        val wave2 = kotlin.math.cos(frequency * 0.05 + (timeMs * 0.0015))

        // Use wave to modulate radius or color or position
        val radius = (10.0 + 100.0 * wave).coerceAtLeast(2.0)
        val circleX = x + width / 2.0 + 15.0 * wave2
        val circleY = y + height / 2.0 + 15.0 * wave

        val dynamicColor = Color(
            r = (128 + 127 * wave).toInt().coerceIn(0, 255).toUByte(),
            g = (128 + 127 * wave2).toInt().coerceIn(0, 255).toUByte(),
            b = 200.toUByte(),
        )

        useGeometryContext {
            fillCircle(circleX, circleY, radius.toInt(), dynamicColor)
        }
    }

    fun setFrequency(frequency: Double) {
        this.frequency = frequency
    }

}
