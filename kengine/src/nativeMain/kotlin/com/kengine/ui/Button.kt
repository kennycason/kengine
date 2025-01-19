package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.hooks.state.State
import com.kengine.hooks.state.useState

class Button(
    id: String,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,
    h: Double,
    padding: Double = 5.0,
    onClick: (() -> Unit)? = null,
    onHover: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null,
    bgColor: Color? = null,
    bgSprite: Sprite? = null,
    private val hoverColor: Color? = null,
    private val pressColor: Color? = null,
    private val isCircle: Boolean = false,
    private val isToggle: Boolean = false,
    private val onToggle: ((Boolean) -> Unit)? = null,
    private val isPressed: State<Boolean> = useState(false),
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
    onClick = onClick,
    onHover = onHover,
    onRelease = onRelease,
    parent = parent
) {
    private var isHovered = false

    override fun draw() {
        if (!visible) return

        val currentColor = when {
            isHovered -> hoverColor
            isToggle && isPressed.get() -> pressColor ?: hoverColor ?: bgColor
            else -> bgColor
        }

        if (currentColor != null) {
            useGeometryContext {
                if (isCircle) {
                    val radius = kotlin.math.min(layoutW, layoutH) / 2.0
                    val centerX = layoutX + (layoutW / 2.0)
                    val centerY = layoutY + (layoutH / 2.0)
                    fillCircle(centerX, centerY, radius.toInt(), currentColor)
                } else {
                    fillRectangle(layoutX, layoutY, layoutW, layoutH, currentColor)
                }
            }
        }

        bgSprite?.draw(layoutX, layoutY)
    }

    override fun click(mouseX: Double, mouseY: Double) {
        if (!visible || !isWithinBounds(mouseX, mouseY)) return

        if (isToggle) {
            // toggle the state and invoke the callback
            isPressed.set(!isPressed.get())
            onToggle?.invoke(isPressed.get())
        } else {
            onClick?.invoke()
        }
    }

    override fun hover(mouseX: Double, mouseY: Double) {
        if (!visible) return

        val wasHovered = isHovered
        isHovered = isWithinBounds(mouseX, mouseY)
        if (isHovered != wasHovered) {
            onHover?.invoke()
        }
    }

    override fun release(mouseX: Double, mouseY: Double) {
        if (!isToggle) {
            onRelease?.invoke()
        }
        super.release(mouseX, mouseY)
    }

    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val relX = mouseX - layoutX
        val relY = mouseY - layoutY

        return if (isCircle) {
            val radius = kotlin.math.min(layoutW, layoutH) / 2.0
            val centerX = layoutW / 2.0
            val centerY = layoutH / 2.0
            val dx = relX - centerX
            val dy = relY - centerY
            (dx * dx + dy * dy) <= (radius * radius)
        } else {
            relX in 0.0..layoutW &&
                relY in 0.0..layoutH
        }
    }

}
