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
    var isDragging = false

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
        val (_, absY) = getAbsolutePosition()
        val trackStart = absY + padding
        val trackEnd = absY + effectiveTrackHeight

        val normalizedY = (absMouseY - trackStart) / (trackEnd - trackStart)
        val clampedY = normalizedY.coerceIn(0.0, 1.0)

        return max - clampedY * (max - min)
    }

    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val (absX, absY) = getAbsolutePosition()
        // Allow clicks on full track height
        val handleX = absX + (w / 2.0) - (handleWidth / 2.0)
        return mouseX >= handleX && mouseX <= handleX + handleWidth &&
            mouseY >= absY + padding && mouseY <= absY + effectiveTrackHeight
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
        if (!visible || !isWithinBounds(x, y)) return
        isDragging = true
        updateValue(y)
    }

    /**
     * Handles hover events while dragging the handle.
     */
    override fun hover(x: Double, y: Double) {
        if (!visible) return
        if (!getViewContext().isMousePressed()) {
            isDragging = false
            return
        }
        if (isDragging) {
            updateValue(y)
        }
    }

    /**
     * Handles release events, clearing active drag state.
     */
    override fun release(x: Double, y: Double) {
        isDragging = false
        super.release(x, y)
    }

    private fun updateValue(y: Double) {
        val newValue = valueAt(y)
        state.set(newValue.coerceIn(min, max))
        onValueChanged?.invoke(state.get())
    }

}
