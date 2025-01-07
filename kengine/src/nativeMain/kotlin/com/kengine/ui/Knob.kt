package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.hooks.state.State

/**
 * A rotary knob that updates a State<Double> between [min.. max].
 * Allows continuous dragging while the mouse is held, even outside the knob.
 */
class Knob(
    id: String,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,  // Required width
    h: Double,  // Required height
    padding: Double = 5.0,

    val min: Double = 0.0,
    val max: Double = 100.0,
    private val state: State<Double>,
    private val onValueChanged: ((Double) -> Unit)? = null,

    stepSize: Double? = null,   // Optional user-supplied step
    bgColor: Color? = null,
    bgSprite: Sprite? = null,
    private val knobColor: Color = Color.gray10,
    private val indicatorColor: Color = Color.white,
    parent: View? = null
) : View(
    id = id,
    desiredX = x,
    desiredY = y,
    desiredW = w,
    desiredH = h,
    padding = padding,
    bgColor = bgColor,
    bgImage = bgSprite,
    parent = parent
) {

    // -------------------------------------------------------
    // Internal drag state
    // -------------------------------------------------------

    private var dragStartY = 0.0
    private var dragStartValue = 0.0

    // For display
    private val rotationRange = 270.0
    private val startAngle    = 135.0

    // Possibly adapt user step or fallback to an auto step
    private val stepSize: Double       = adaptStepSize(stepSize, min, max)
    private val dragSensitivity: Double = calcDragSensitivity(this.stepSize, min, max)

    // -------------------------------------------------------
    // Step size & sensitivity logic
    // -------------------------------------------------------
    private fun adaptStepSize(userStep: Double?, min: Double, max: Double): Double {
        userStep?.let { return it }

        val range = (max - min).coerceAtLeast(0.0)
        return when {
            // For extremely small ranges like volume 0..1
            range <= 1.0  -> range / 1000.0     // e.g. => step=0.01 if range=1
            // Up to 10
            range > 10 && range <= 100      -> range / 25
            // Up to 100
            range > 100 && range <= 1000    -> range / 50
            // Over 1000
            range > 1000                    -> range / 100
            // Default if within [1..10] or something not caught by above
            else                            -> range / 25
        }
    }

    private fun calcDragSensitivity(step: Double, min: Double, max: Double): Double {
        val range = (max - min).coerceAtLeast(0.0)
        return when {
            // Very small range => keep it direct
            range <= 1.0   -> step * 8
            // Up to 10 => multiply by ~2
            range <= 10.0  -> step * 2
            // Up to 100 => multiply ~5
            range <= 100.0 -> step * 5
            // Over 100 => scale down so user doesn’t jump too fast
            else           -> step / 10.0
        }
    }

    // -------------------------------------------------------
    // Value <-> angle mapping
    // -------------------------------------------------------

    private fun valueToAngle(value: Double): Double {
        val normalized = (value - min) / (max - min)
        return startAngle + (normalized * rotationRange)
    }

    /**
     * Continuous approach: compute how far we've dragged in “units,”
     * then apply it to `dragStartValue`.
     */
    private fun calcValueFromDrag(currentY: Double): Double {
        val deltaY = dragStartY - currentY
        // This is a float offset:
        val offset = (deltaY / dragSensitivity) * stepSize
        return (dragStartValue + offset).coerceIn(min, max)
    }

    // -------------------------------------------------------
    // Rendering
    // -------------------------------------------------------

    override fun draw() {
        if (!visible) return

        // If you want to fill the background or have children, do it here:
        super.draw()

        // Then draw the knob circle + indicator
        val cx     = layoutX + (layoutW / 2.0)
        val cy     = layoutY + (layoutH / 2.0)
        val radius = (kotlin.math.min(layoutW, layoutH) / 2.0) - padding

        useGeometryContext {
            // Knob circle
            fillCircle(cx, cy, radius.toInt(), knobColor)

            // Indicator line
            val angleDeg = valueToAngle(state.get())
            val angleRad = angleDeg * (kotlin.math.PI / 180.0)
            val length   = radius * 0.8
            val ex       = cx + length * kotlin.math.cos(angleRad)
            val ey       = cy + length * kotlin.math.sin(angleRad)

            drawLine(cx, cy, ex, ey, indicatorColor)
        }
    }

    // -------------------------------------------------------
    // Input events
    // -------------------------------------------------------

    /**
     * If we click inside, claim dragFocus so we can move outside the circle.
     */
    override fun click(mouseX: Double, mouseY: Double) {
        if (!visible || !isWithinBounds(mouseX, mouseY)) return

        // Claim dragFocus from our custom ViewContext
        ViewContext.get().setDragFocus(this)

        dragStartY = mouseY
        dragStartValue = state.get().coerceIn(min, max)
    }

    /**
     * If we’re the active drag, keep updating as the user moves the mouse.
     */
    override fun hover(mouseX: Double, mouseY: Double) {
        // We only drag if the context says we’re the one holding the mouse
        if (ViewContext.get().isDragging(this)) {
            logger.info { "hover: stepSize: $stepSize, dragSensitivity: $dragSensitivity" }
            val newValue = calcValueFromDrag(mouseY)
            if (newValue != state.get()) {
                state.set(newValue)
                onValueChanged?.invoke(newValue)
            }
        }
    }

    /**
     * On release, stop dragging if we had the focus.
     */
    override fun release(mouseX: Double, mouseY: Double) {
        // Clear our dragFocus if we had it
        ViewContext.get().clearDragFocus(this)

        super.release(mouseX, mouseY)
    }

    /**
     * Simple circle-based hit test. If you prefer a bounding box, do that.
     */
    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val cx = layoutX + (layoutW / 2.0)
        val cy = layoutY + (layoutH / 2.0)
        val r  = kotlin.math.min(layoutW, layoutH) / 2.0
        val dx = mouseX - cx
        val dy = mouseY - cy
        return (dx * dx + dy * dy) <= (r * r)
    }
}
