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
    /**
     * Handles click events based on absolute positions.
     */
    override fun click(mouseX: Double, mouseY: Double) {
        if (!visible) return

        // Compute absolute and relative positions for logging
        val (absX, absY) = getAbsolutePosition()
        val relX = mouseX - absX
        val relY = mouseY - absY

        if (logger.isInfoEnabled()) {
            logger.info("Button $id - CLICK event at ($mouseX, $mouseY). " +
                "AbsPos=($absX, $absY), RelPos=($relX, $relY)")
        }

        // Check if the pointer is within bounds
        if (isWithinBounds(mouseX, mouseY)) {
            isPressed = true
            onClick?.invoke()
            if (logger.isInfoEnabled()) {
                logger.info("Button $id isPressed=true after click, onClick invoked.")
            }
        }
    }

    override fun hover(mouseX: Double, mouseY: Double) {
        if (!visible) return

        val (absX, absY) = getAbsolutePosition()
        val relX = mouseX - absX
        val relY = mouseY - absY

        if (logger.isInfoEnabled()) {
            logger.info("Button $id - HOVER event at ($mouseX, $mouseY). " +
                "AbsPos=($absX, $absY), RelPos=($relX, $relY)")
        }

        val wasHovered = isHovered
        isHovered = isWithinBounds(mouseX, mouseY)

        // Only trigger onHover if hover state changes
        if (isHovered != wasHovered) {
            onHover?.invoke()
            if (logger.isInfoEnabled()) {
                logger.info("Button $id isHovered=$isHovered, onHover invoked.")
            }
        }
    }

    override fun release(mouseX: Double, mouseY: Double) {
        if (logger.isInfoEnabled()) {
            logger.info("Button $id - RELEASE event at ($mouseX, $mouseY). Pressed=$isPressed")
        }

        if (isPressed) {
            onRelease?.invoke()
            if (logger.isInfoEnabled()) {
                logger.info("Button $id onRelease invoked, isPressed=false.")
            }
        }

        isPressed = false
        super.release(mouseX, mouseY)
    }

    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        // We compute absolute position and transform mouseX/Y to relative
        val (absX, absY) = getAbsolutePosition()
        val relX = mouseX - absX
        val relY = mouseY - absY

        return if (isCircle) {
            val radius = kotlin.math.min(w, h) / 2.0
            val centerX = w / 2.0
            val centerY = h / 2.0
            val dx = relX - centerX
            val dy = relY - centerY
            (dx * dx + dy * dy) <= (radius * radius)
        } else {
            relX >= 0 && relX <= w &&
                relY >= 0 && relY <= h
        }
    }

}
