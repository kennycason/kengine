package com.kengine.particle

import com.kengine.entity.Actor
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.math.Vec2
import kotlin.math.abs

class RainbowLinesWithFrequencyEffect(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val numLines: Int = 256,
    private var frequency: Double = 0.0
) : Actor {
    private val colors = Color.rainbow(numLines, maxHue = 360f) // generate rainbow colors
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

    fun setFrequency(frequency: Double) {
      //  this.frequency = frequency
    }

    override fun draw() {
        useGeometryContext {
            for (i in 0 until numLines) {
                // The line index we start from, considering the 'offset'
                val startIdx = (i + offset) % numLines
                val startBase = startPoints[startIdx]
                val color = colors[i]

                // We'll do a horizontal wave effect by shifting the line's X a bit,
                // based on frequency and the line index.
                // If you want a vertical wave, you can do it on y or both.
                val wave = kotlin.math.sin((startBase.x + i) * 0.05 + frequency * 0.01)
                val waveShift = wave * (frequency * 0.25)   // amplitude scale

                // Then compute final start/end points.
                // 1) The "base" position is x + startBase.x for X, y + startBase.y for Y.
                // 2) We'll add waveShift to the X coordinates to create side-to-side wobble.
                val startX = x + startBase.x + waveShift
                val startY = y + startBase.y

                val endX = x + startBase.x + waveShift
                val endY = y + height.toDouble()   // Directly below, ignoring wave for Y

                // Draw the line
                drawLine(
                    startX, startY,
                    endX,   endY,
                    color.r, color.g, color.b, 0xEEu
                )
            }
        }
    }
}
