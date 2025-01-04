package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.hooks.state.State

class Slider(
    id: String,
    x: Double,
    y: Double,
    w: Double = 0.0, // Expandable by default
    h: Double = 0.0, // Expandable by default
    padding: Double = 5.0, // Padding for track and handle
    val min: Double = 0.0,
    val max: Double = 100.0,
    private val state: State<Double>,
    private val onValueChanged: ((Double) -> Unit)? = null,
    bgColor: Color = Color.gray10,
    handleColor: Color = Color.white,
    parent: View? = null
) : View(
    id = id,
    x = x,
    y = y,
    w = w,
    h = h,
    padding = padding,
    bgColor = bgColor,
    parent = parent
) {

    // Track and handle dimensions, scaled based on width
    private var trackWidth = w * 0.2
    private var handleSize = w * 1.5
    private val handleColor = handleColor

    private var dragging = false

    init {
        calculateDimensions() // Auto-expand size if w/h = 0.0
    }

    // Calculate handle position based on value
    private fun calculateHandlePosition(): Double {
        val range = max - min
        val normalizedValue = (state.get() - min) / range
        val trackHeight = h - (2 * padding)
        return padding + trackHeight - (normalizedValue * trackHeight)
    }

    // Calculate value based on mouse Y position
    private fun valueAt(yPos: Double): Double {
        val trackHeight = h - (2 * padding)
        val clampedY = yPos.coerceIn(padding, padding + trackHeight)
        val range = max - min
        return max - ((clampedY - padding) / trackHeight) * range
    }

    override fun draw(parentX: Double, parentY: Double) {
        val absX = parentX + x
        val absY = parentY + y

        // Center the track and handle based on width/height
        val trackX = absX + (w / 2.0) - (trackWidth / 2.0)
        val trackY = absY + padding
        val trackHeight = h - (2 * padding)

        useGeometryContext {
            // Draw slider track
            fillRectangle(trackX, trackY, trackWidth, trackHeight, bgColor!!)

            // Draw slider handle
            val handleY = calculateHandlePosition()
            val handleX = absX + (w / 2.0) - (handleSize / 2.0)
            fillRectangle(handleX, absY + handleY, handleSize, handleSize, handleColor)
        }
    }

    override fun click(x: Double, y: Double) {
        val handleY = calculateHandlePosition()
        val handleX = (w / 2.0) - (handleSize / 2.0)

        // Detect clicks on handle
        val isWithinHandle =
            x >= handleX && x <= handleX + handleSize &&
                y >= handleY && y <= handleY + handleSize

        if (isWithinHandle) {
            dragging = true // Start dragging
        } else {
            val newValue = valueAt(y)
            state.set(newValue)
            onValueChanged?.invoke(newValue)
        }
    }

    override fun hover(x: Double, y: Double) {
        // Dragging logic during mouse movement
        if (dragging) {
            val newValue = valueAt(y)
            state.set(newValue)
            onValueChanged?.invoke(newValue)
        }
    }

    fun release() {
        // Stop dragging on mouse release
        dragging = false
    }
}
