package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.hooks.state.State

class ToggleButton(
    id: String,
    x: Double,
    y: Double,
    w: Double,  // Required width
    h: Double,  // Required height
    padding: Double = 5.0,
    private val state: State<Boolean>,
    private val onToggle: ((Boolean) -> Unit)? = null,
    private val onHover: (() -> Unit)? = null,
    bgColor: Color? = null,
    bgSprite: Sprite? = null,
    private val hoverColor: Color? = null,
    private val activeColor: Color? = null,
    private val activeHoverColor: Color? = null,
    private val isCircle: Boolean = false,
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
    private var isHovered: Boolean = false

    /**
     * Draws the toggle button with the appropriate colors based on state.
     */
    override fun draw(parentX: Double, parentY: Double) {
        if (!visible) return

        val absX = parentX + x
        val absY = parentY + y

        // Determine current color based on state and hover
        val currentColor = when {
            state.get() && isHovered -> activeHoverColor
            state.get() -> activeColor
            isHovered -> hoverColor
            else -> bgColor
        }

        if (currentColor != null) {
            useGeometryContext {
                if (isCircle) {
                    val radius = kotlin.math.min(w, h) / 2.0
                    val centerX = absX + w / 2.0
                    val centerY = absY + h / 2.0
                    fillCircle(centerX, centerY, radius.toInt(), currentColor)
                } else {
                    fillRectangle(absX, absY, w, h, currentColor)
                }
            }
        }

        // Draw background image if specified
        bgImage?.draw(absX, absY)
    }

    /**
     * Handles click events to toggle state.
     */
    override fun click(x: Double, y: Double) {
        if (!visible || !isWithinBounds(x, y)) return

        // Toggle state and notify
        val newState = !state.get()
        state.set(newState)
        onToggle?.invoke(newState)
    }

    /**
     * Handles hover events to update hover state.
     */
    override fun hover(x: Double, y: Double) {
        if (!visible) return

        // Update hover state
        val wasHovered = isHovered
        isHovered = isWithinBounds(x, y)

        // Notify only when hover state changes
        if (isHovered != wasHovered) {
            onHover?.invoke()
        }
    }

    /**
     * Handles release events to clear active state.
     */
    override fun release(x: Double, y: Double) {
        if (activeDragView == this) {
            activeDragView = null // Clear drag state
        }
        super.release(x, y)
    }

    /**
     * Determines if a point is within the toggle button's bounds.
     */
    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val (absX, absY) = getAbsolutePosition()

        return if (isCircle) {
            // Calculate distance from center for circular bounds
            val radius = kotlin.math.min(w, h) / 2.0
            val centerX = absX + w / 2.0
            val centerY = absY + h / 2.0
            val dx = mouseX - centerX
            val dy = mouseY - centerY
            (dx * dx + dy * dy) <= (radius * radius)
        } else {
            // Rectangular bounds check
            mouseX >= absX && mouseX <= absX + w &&
                mouseY >= absY && mouseY <= absY + h
        }
    }
}
