package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite

class Button(
    id: String,
    // For the new layout approach, these are the "desired" values:
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,  // Required width
    h: Double,  // Required height
    padding: Double = 5.0,
    onClick: (() -> Unit)? = null,
    onHover: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null,

    bgColor: Color? = null,
    bgSprite: Sprite? = null,
    private val hoverColor: Color? = null,
    private val pressColor: Color? = null,
    private val isCircle: Boolean = false,
    parent: View? = null
) : View(
    id = id,
    desiredX = x,
    desiredY = y,
    desiredW = w,
    desiredH = h,
    bgColor = bgColor,
    bgImage = bgSprite,
    padding = padding,
    onClick = onClick,
    onHover = onHover,
    onRelease = onRelease,
    parent = parent
) {
    private var isHovered: Boolean = false
    private var isPressed: Boolean = false

    override fun draw() {
        // Don’t draw if invisible
        if (!visible) return

        // We rely on layoutX, layoutY, layoutW, layoutH for final positions
        val absX = layoutX
        val absY = layoutY

        if (logger.isTraceEnabled()) {
            logger.trace(
                "Drawing Button `$id` at ($absX, $absY) size=(${layoutW} x $layoutH), parent=${parent?.id}"
            )
        }

        // Decide which color to use based on states
        val currentColor = when {
            isPressed -> pressColor
            isHovered -> hoverColor
            else      -> bgColor
        }

        // Draw background shape
        if (currentColor != null) {
            useGeometryContext {
                if (isCircle) {
                    val radius = kotlin.math.min(layoutW, layoutH) / 2.0
                    val centerX = absX + (layoutW / 2.0)
                    val centerY = absY + (layoutH / 2.0)
                    fillCircle(centerX, centerY, radius.toInt(), currentColor)
                } else {
                    fillRectangle(absX, absY, layoutW, layoutH, currentColor)
                }
            }
        }

        // Draw optional sprite on top
        bgImage?.draw(absX, absY)
    }

    override fun click(mouseX: Double, mouseY: Double) {
        if (!visible) return

        // If in bounds => press
        if (isWithinBounds(mouseX, mouseY)) {
            isPressed = true
            onClick?.invoke()
            if (logger.isInfoEnabled()) {
                logger.info("Button `$id` => isPressed=true, onClick invoked.")
            }
        }
    }

    override fun hover(mouseX: Double, mouseY: Double) {
        if (!visible) return

        val wasHovered = isHovered
        isHovered = isWithinBounds(mouseX, mouseY)

        if (isHovered != wasHovered) {
            onHover?.invoke()
            if (logger.isInfoEnabled()) {
                logger.info("Button `$id` => isHovered=$isHovered, onHover invoked.")
            }
        }
    }

    override fun release(mouseX: Double, mouseY: Double) {
        if (logger.isInfoEnabled()) {
            logger.info("Button `$id` => release, wasPressed=$isPressed")
        }

        // If it was pressed, call onRelease
        if (isPressed) {
            onRelease?.invoke()
            if (logger.isInfoEnabled()) {
                logger.info("Button `$id` => onRelease invoked, isPressed=false.")
            }
        }
        // Always un-press on release (not a toggle)
        isPressed = false

        super.release(mouseX, mouseY)
    }

    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        // Compare to final layout position & size
        val relX = mouseX - layoutX
        val relY = mouseY - layoutY

        return if (isCircle) {
            // Check distance from center
            val radius = kotlin.math.min(layoutW, layoutH) / 2.0
            val centerX = layoutW / 2.0
            val centerY = layoutH / 2.0
            val dx = relX - centerX
            val dy = relY - centerY
            (dx * dx + dy * dy) <= radius * radius
        } else {
            // Check rectangular bounds
            relX in 0.0..layoutW &&
                relY in 0.0..layoutH
        }
    }
}
