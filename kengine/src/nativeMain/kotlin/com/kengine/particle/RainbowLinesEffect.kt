package com.kengine.particle

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.math.Vec2

class RainbowLinesEffect(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val numLines: Int = 256
) {
    private val colors = Color.rainbow(numLines) // generate rainbow colors
    private val startPoints = mutableListOf<Vec2>()
    private var offset = 0 // Tracks position for rotation

    init {
        // generate initial points
        for (i in 0 until numLines) {
            val x1 = (i * width) / numLines
            val y1 = 0
            startPoints.add(Vec2(x1.toDouble(), y1.toDouble()))
        }
    }

    fun update() {
        // rotate the points clockwise
        offset = (offset + 1) % numLines
    }

    fun draw() {
        useGeometryContext {
            for (i in 0 until numLines) {
                val startIdx = (i + offset) % numLines
                val start = startPoints[startIdx]
                val end = Vec2(start.x, height.toDouble()) // End directly below start

                val color = colors[i]
                drawLine(
                    x + start.x.toInt(), y + start.y.toInt(),
                    x + end.x.toInt(), y + end.y.toInt(),
                    color.r, color.g, color.b, 0xEEu
                )
            }
        }
    }
}
