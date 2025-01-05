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
    // Track dimensions
    private val trackWidth: Double = trackWidth ?: (w * 0.2)

    // Handle dimensions - default to square based on track width if not specified
    private val handleWidth: Double = handleWidth ?: (w * 0.4)
    private val handleHeight: Double = handleHeight ?: handleWidth!!

    private var dragging: Boolean = false

    // The actual usable track height considering padding
    private val effectiveTrackHeight: Double get() = h - (padding * 2)

    private fun calculateHandlePosition(): Double {
        val range = max - min
        val normalizedValue = (state.get() - min) / range
        val availableHeight = effectiveTrackHeight - handleHeight
        return padding + ((1.0 - normalizedValue) * availableHeight)
    }

    private fun valueAt(absY: Double, parentY: Double): Double {
        val relativeY = absY - parentY - y
        val availableHeight = effectiveTrackHeight - handleHeight
        val clampedY = (relativeY - padding).coerceIn(0.0, availableHeight)
        val range = max - min
        return max - (clampedY / availableHeight) * range
    }

    override fun draw(parentX: Double, parentY: Double) {
        if (!visible) return

        val absX = parentX + x
        val absY = parentY + y

        if (logger.isTraceEnabled()) {
            logger.trace { "Rendering view $id at ($absX, $absY) size: ${w}x${h}, parent: ${parent?.id}" }
        }

        // Draw background if specified
        super.draw(parentX, parentY)

        // Center the track horizontally
        val trackX = absX + (w / 2.0) - (trackWidth / 2.0)
        val trackY = absY + padding

        useGeometryContext {
            // Draw slider track within padded area
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

    override fun click(x: Double, y: Double) {
        val relativeX = x - this.x
        val relativeY = y - this.y

        val trackX = (w / 2.0) - (trackWidth / 2.0)
        val handleY = calculateHandlePosition()
        val handleX = (w / 2.0) - (handleWidth / 2.0)

        // Check if click is within handle bounds
        val isWithinHandle = relativeX >= handleX && relativeX <= handleX + handleWidth &&
            relativeY >= handleY && relativeY <= handleY + handleHeight

        // Check if click is within track bounds (respecting padding)
        val isWithinTrack = relativeX >= trackX && relativeX <= trackX + trackWidth &&
            relativeY >= padding && relativeY <= h - padding

        if (isWithinHandle) {
            dragging = true
        } else if (isWithinTrack) {
            val newValue = valueAt(y, 0.0)
            state.set(newValue.coerceIn(min, max))
            onValueChanged?.invoke(state.get())
        }
    }

    override fun hover(x: Double, y: Double) {
        if (!dragging) return
        val newValue = valueAt(y, 0.0)
        state.set(newValue.coerceIn(min, max))
        onValueChanged?.invoke(state.get())
    }

    override fun release(x: Double, y: Double) {
        dragging = false
        super.release(x, y)
    }
}
