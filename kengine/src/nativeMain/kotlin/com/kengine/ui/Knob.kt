package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.hooks.state.State

/**
 * A rotary knob that updates a State<Double> between [min, max].
 * Allows continuous dragging while the mouse is held, even outside the knob.
 */
class Knob(
    id: String,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,
    h: Double,
    padding: Double = 5.0,
    val min: Double = 0.0,
    val max: Double = 100.0,
    private var dragScale: Double = 200.0,
    private val state: State<Double>,
    private val onValueChanged: ((Double) -> Unit)? = null,
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
    bgSprite = bgSprite,
    parent = parent
) {
    private var dragStartY = 0.0
    private var dragStartValue = 0.0
    private val rotationRange = 270.0
    private val startAngle = 135.0

    override fun draw() {
        if (!visible) return

        super.draw()

        // draw the knob circle + indicator
        val cx = layoutX + (layoutW / 2.0)
        val cy = layoutY + (layoutH / 2.0)
        val radius = (kotlin.math.min(layoutW, layoutH) / 2.0) - padding

        useGeometryContext {
            // knob circle
            fillCircle(cx, cy, radius.toInt(), knobColor)

            // indicator line
            val angleDeg = valueToAngle(state.get())
            val angleRad = angleDeg * (kotlin.math.PI / 180.0)
            val length = radius * 0.8
            val ex = cx + length * kotlin.math.cos(angleRad)
            val ey = cy + length * kotlin.math.sin(angleRad)

            drawLine(cx, cy, ex, ey, indicatorColor)
        }
    }

    private fun valueToAngle(value: Double): Double {
        val normalized = (value - min) / (max - min)
        return startAngle + (normalized * rotationRange)
    }

    private fun calcValueFromDrag(currentY: Double): Double {
        val deltaY = dragStartY - currentY
        val offset = (deltaY / dragScale) * (max - min)
        return (dragStartValue + offset).coerceIn(min, max)
    }

    override fun hover(mouseX: Double, mouseY: Double) {
        if (ViewContext.get().isDragging(this)) {
            val newValue = calcValueFromDrag(mouseY)
            if (newValue != state.get()) {
                state.set(newValue)
                onValueChanged?.invoke(newValue)
            }
        }
    }

    override fun click(mouseX: Double, mouseY: Double) {
        if (!visible || !isWithinBounds(mouseX, mouseY)) return
        ViewContext.get().setDragFocus(this)
        dragStartY = mouseY
        dragStartValue = state.get().coerceIn(min, max)
    }

    override fun release(mouseX: Double, mouseY: Double) {
        ViewContext.get().clearDragFocus(this)
        super.release(mouseX, mouseY)
    }

    /**
     * Simple circle-based collision. Consider square bounds check
     */
    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val cx = layoutX + (layoutW / 2.0)
        val cy = layoutY + (layoutH / 2.0)
        val r = kotlin.math.min(layoutW, layoutH) / 2.0
        val dx = mouseX - cx
        val dy = mouseY - cy
        return (dx * dx + dy * dy) <= (r * r)
    }
}
