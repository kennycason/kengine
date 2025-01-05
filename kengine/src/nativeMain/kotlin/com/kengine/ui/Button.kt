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

    override fun draw(parentX: Double, parentY: Double) {
        if (!visible) return

        val absX = parentX + x
        val absY = parentY + y

        if (logger.isTraceEnabled()) {
            logger.trace { "Rendering view $id at ($absX, $absY) size: ${w}x${h}, parent: ${parent?.id}" }
        }

        // Determine current color based on state
        val currentColor = when {
            isPressed -> pressColor
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

        bgImage?.draw(absX, absY)
    }

    override fun click(x: Double, y: Double) {
        if (!visible) return

        val relativeX = x - this.x
        val relativeY = y - this.y

        if (isWithinBounds(relativeX, relativeY)) {
            isPressed = true
            onClick?.invoke()
        }
    }

    override fun hover(x: Double, y: Double) {
        if (!visible) return

        val relativeX = x - this.x
        val relativeY = y - this.y

        val wasHovered = isHovered
        isHovered = isWithinBounds(relativeX, relativeY)

        if (isHovered != wasHovered) {
            onHover?.invoke()
        }
    }

    override fun release(x: Double, y: Double) {
        if (isPressed) {
            onRelease?.invoke()
        }
        isPressed = false
        super.release(x, y)
    }

    private fun isWithinBounds(relativeX: Double, relativeY: Double): Boolean {
        return if (isCircle) {
            val radius = kotlin.math.min(w, h) / 2.0
            val centerX = w / 2.0
            val centerY = h / 2.0
            val dx = relativeX - centerX
            val dy = relativeY - centerY
            (dx * dx + dy * dy) <= (radius * radius)
        } else {
            relativeX >= 0 && relativeX <= w && relativeY >= 0 && relativeY <= h
        }
    }
}
