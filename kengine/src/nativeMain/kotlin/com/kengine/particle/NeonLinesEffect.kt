package com.kengine.particle

import com.kengine.entity.Actor
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.math.Vec2
import kotlin.math.abs

class NeonLinesEffect(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val numLines: Int = 256,
) : Actor {
    private val colors = Color.neon(numLines) // generate neon colors
    private val startPoints = mutableListOf<Vec2>()
    var offset = 0 // Tracks position for rotation

    init {
        // generate initial points
        for (i in 0 until numLines) {
            val x1 = (i * width) / numLines
            val y1 = 0
            startPoints.add(Vec2(x1.toDouble(), y1.toDouble()))
        }
    }

    override fun update() {
        // Rotate the points clockwise
        offset = (offset + 1) % numLines
    }

    fun setOffset(newOffset: Int) {
        offset = abs(newOffset) % numLines // Ensure offset wraps correctly
    }

    override fun draw() {
        useGeometryContext {
            for (i in 0 until numLines) {
                val startIdx = (i + offset) % numLines
                val start = startPoints[startIdx]
                val end = Vec2(start.x, height.toDouble()) // End directly below start

                val color = colors[i]
                drawLine(
                    x + start.x, y + start.y,
                    x + end.x, y + end.y,
                    color.r, color.g, color.b, 0xEEu
                )
            }
        }
    }
}
