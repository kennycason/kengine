package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.hooks.state.State

class Slider(
    id: String,
    x: Double,
    y: Double,
    w: Double,  // Required width
    h: Double,  // Required height
    padding: Double = 5.0,
    val min: Double = 0.0,
    val max: Double = 100.0,
    private val state: State<Double>,
    private val onValueChanged: ((Double) -> Unit)? = null,
    bgColor: Color? = null,
    bgSprite: Sprite? = null,
    trackWidth: Double? = null,
    private val trackColor: Color = Color.gray10,
    handleWidth: Double? = null,
    handleHeight: Double? = null,
    private val handleColor: Color = Color.white,
    private val handleSprite: Sprite? = null,
    parent: View? = null
) : View(
    id = id,
    x = x,
    y = y,
    w = w,
    h = h,
    padding = padding,
    bgColor = bgColor,
    bgImage = bgSprite,
    parent = parent
) {
    // Dimensions
    private val trackWidth: Double = trackWidth ?: (w * 0.2)
    private val handleWidth: Double = handleWidth ?: (w * 0.4)
    private val handleHeight: Double = handleHeight ?: handleWidth!!

    // Track height considering padding
    private val effectiveTrackHeight: Double get() = h - (padding * 2)

    /**
     * Calculates the handle position based on the state value.
     */
    private fun calculateHandlePosition(): Double {
        val range = max - min
        val normalizedValue = (state.get() - min) / range
        val availableHeight = effectiveTrackHeight - handleHeight
        return padding + ((1.0 - normalizedValue) * availableHeight)
    }

    /**
     * Computes the slider value at a given position.
     */
    private fun valueAt(absMouseY: Double): Double {
        val (_, absY) = getAbsolutePosition() // Compute absolute position
        val relativeY = absMouseY - absY      // Translate to local coordinates

        val availableHeight = effectiveTrackHeight - handleHeight
        val clampedY = (relativeY - padding).coerceIn(0.0, availableHeight)
        val range = max - min
        return max - (clampedY / availableHeight) * range
    }

    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val (absX, absY) = getAbsolutePosition() // Compute absolute position
        val handleY = absY + calculateHandlePosition()
        val handleX = absX + (w / 2.0) - (handleWidth / 2.0)

        return mouseX >= handleX && mouseX <= handleX + handleWidth &&
            mouseY >= handleY && mouseY <= handleY + handleHeight
    }

    override fun draw(parentX: Double, parentY: Double) {
        if (!visible) return

        val absX = parentX + x
        val absY = parentY + y

        // Draw background if specified
        super.draw(parentX, parentY)

        // Center the track horizontally
        val trackX = absX + (w / 2.0) - (trackWidth / 2.0)
        val trackY = absY + padding

        useGeometryContext {
            // Draw slider track
            fillRectangle(trackX, trackY, trackWidth, effectiveTrackHeight, trackColor)

            // Draw slider handle
            val handleY = absY + calculateHandlePosition()
            val handleX = absX + (w / 2.0) - (handleWidth / 2.0)

            if (handleSprite != null) {
                handleSprite.draw(handleX, handleY)
            } else {
                fillRectangle(handleX, handleY, handleWidth, handleHeight, handleColor)
            }
        }
    }

    /**
     * Handles click events, calculates value based on mouse position, and triggers change events.
     */
    override fun click(x: Double, y: Double) {
        val (absX, absY) = getAbsolutePosition()
        if (!isWithinBounds(x, y)) return

        // Set active drag view
        activeDragView = this

        // Update value based on click position
        val newValue = valueAt(y)
        state.set(newValue.coerceIn(min, max))
        onValueChanged?.invoke(state.get())
    }

    /**
     * Handles hover events while dragging the handle.
     */
    override fun hover(x: Double, y: Double) {
        if (activeDragView != this) return // Only update while dragging

        // Update value based on hover position
        val newValue = valueAt(y)
        state.set(newValue.coerceIn(min, max))
        onValueChanged?.invoke(state.get())
    }

    /**
     * Handles release events, clearing active drag state.
     */
    override fun release(x: Double, y: Double) {
        if (activeDragView == this) {
            activeDragView = null // Clear drag state
        }
        super.release(x, y)
    }
}
