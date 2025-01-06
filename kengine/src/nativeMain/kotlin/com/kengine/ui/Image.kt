package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite

class Image(
    id: String,
    x: Double,
    y: Double,
    w: Double,  // Required width
    h: Double,  // Required height
    padding: Double = 0.0,
    private val sprite: Sprite,
    bgColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onHover: (() -> Unit)? = null,
    parent: View? = null
) : View(
    id = id,
    x = x,
    y = y,
    w = w,
    h = h,
    padding = padding,
    bgColor = bgColor,
    parent = parent,
    onClick = onClick,
    onHover = onHover
) {
    private var isHovered: Boolean = false

    override fun draw(parentX: Double, parentY: Double) {
        if (!visible) return

        val absX = parentX + x
        val absY = parentY + y

        // Draw background color if specified
        if (bgColor != null) {
            useGeometryContext {
                fillRectangle(absX, absY, w, h, bgColor)
            }
        }

        // Draw the sprite
        sprite.draw(absX, absY)
    }


}
