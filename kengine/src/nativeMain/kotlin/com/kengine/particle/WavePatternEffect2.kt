package com.kengine.particle

import com.kengine.entity.Actor
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.math.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class WavePatternEffect2(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val numWaves: Int = 256
) : Actor {
    private val points = mutableListOf<Vec2>()
    private val colors = Color.rainbow(numWaves)
    private var time = 0.0
    private var frequency = 440.0 // Default frequency

    init {
        // generate initial points
        for (i in 0 until numWaves) {
            val px = (i * width) / numWaves
            points.add(Vec2(px.toDouble(), height / 2.0))
        }
    }

    // update the frequency dynamically
    fun setFrequency(newFrequency: Double) {
        frequency = newFrequency
    }

    override fun update() {
        time += 0.1 // increment time for animation

        for (i in points.indices) {
            val point = points[i]

            // wave-like distortion based on frequency and time
            val amplitude = (height / 2.0 * sin(frequency * 0.01)) // oscillating amplitude
            val waveSpeed = (frequency * 0.002) // faster movement for higher frequency
            val waveOffset = sin(i * waveSpeed + time) * amplitude

            // distort Y-position with cosine waves and offsets
            point.y = height / 2.0 + waveOffset * cos(time + (PI * i / numWaves))
        }
    }

    override fun draw() {
        useGeometryContext {
            for (i in 1 until points.size) {
                val color = colors[i % colors.size]
                val start = points[i - 1]
                val end = points[i]

                drawLine(
                    x + start.x, y + start.y,
                    x + end.x, y + end.y,
                    color.r, color.g, color.b, 0xEEu
                )
            }
        }
    }
}
