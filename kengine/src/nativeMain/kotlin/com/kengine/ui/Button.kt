package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite

class Button(
    id: String,
    x: Double,
    y: Double,
    w: Double,  // Required width
    h: Double,  // Required height
    padding: Double = 5.0,
    private val onClick: (() -> Unit)? = null,
    private val onHover: (() -> Unit)? = null,
    private val onRelease: (() -> Unit)? = null,
    bgColor: Color? = null,
    bgSprite: Sprite? = null,
    private val hoverColor: Color? = null,
    private val pressColor: Color? = null,
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
    private var isPressed: Boolean = false

    /**
     * Renders the button with its visual state (normal, hover, press)
     */
    override fun draw(parentX: Double, parentY: Double) {
        if (!visible) return

        // Compute absolute position
        val absX = parentX + x
        val absY = parentY + y

        // Determine the current color based on the button's state
        val currentColor = when {
            isPressed && View.activeDragView == this -> pressColor
            isHovered -> hoverColor
            else -> bgColor
        }

        // Render the button
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

        bgImage?.draw(absX, absY)
    }

    /**
     * Handles click events
     */
    override fun click(x: Double, y: Double) {
        if (!visible) return

        // Check if another view is already active
        if (View.activeDragView != null && View.activeDragView != this) return

        // Get absolute position
        val (absX, absY) = getAbsolutePosition()

        // Verify bounds
        if (!isWithinBounds(x, y)) return

        // Activate this view as the current drag view
        View.activeDragView = this
        isPressed = true
        onClick?.invoke()
    }

    /**
     * Handles hover events
     */
    override fun hover(x: Double, y: Double) {
        if (!visible) return

        // Compute absolute position
        val (absX, absY) = getAbsolutePosition()

        // Check hover state
        val wasHovered = isHovered
        isHovered = isWithinBounds(x, y)

        // Trigger hover event if the state changed
        if (isHovered != wasHovered) {
            onHover?.invoke()
        }
    }

    /**
     * Handles release events
     */
    override fun release(x: Double, y: Double) {
        if (isPressed && View.activeDragView == this) {
            isPressed = false
            onRelease?.invoke()
            View.activeDragView = null
        }
        super.release(x, y)
    }

    /**
     * Checks whether the mouse coordinates are within the button bounds
     */
    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val (absX, absY) = getAbsolutePosition()

        return if (isCircle) {
            // Circle bounds check
            val centerX = absX + w / 2.0
            val centerY = absY + h / 2.0
            val radius = kotlin.math.min(w, h) / 2.0

            val distSquared = (mouseX - centerX) * (mouseX - centerX) +
                (mouseY - centerY) * (mouseY - centerY)
            distSquared <= radius * radius
        } else {
            // Rectangle bounds check
            mouseX >= absX && mouseX <= absX + w &&
                mouseY >= absY && mouseY <= absY + h
        }
    }
}
