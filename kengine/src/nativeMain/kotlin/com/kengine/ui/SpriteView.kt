package com.kengine.ui

import com.kengine.graphics.Color
import com.kengine.graphics.Sprite

class SpriteView(
    id: String,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,
    h: Double,
    padding: Double = 0.0,
    val sprite: Sprite,
    bgColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onHover: (() -> Unit)? = null,
    parent: View? = null
) : View(
    id = id,
    desiredX = x,
    desiredY = y,
    desiredW = w,
    desiredH = h,
    padding = padding,
    bgColor = bgColor,
    onClick = onClick,
    onHover = onHover,
    parent = parent
) {
    override fun draw() {
        if (!visible) return

        super.draw()
        // TODO figure out how to handle auto-scale (currently passed as constructor to Sprite)
        sprite.draw(layoutX, layoutY)
    }

    override fun click(mouseX: Double, mouseY: Double) {
        if (!visible || !isWithinBounds(mouseX, mouseY)) return
        onClick?.invoke()
    }

    override fun hover(mouseX: Double, mouseY: Double) {
        if (!visible) return
        if (isWithinBounds(mouseX, mouseY)) {
            onHover?.invoke()
        }
    }

    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val relX = mouseX - layoutX
        val relY = mouseY - layoutY
        return relX in 0.0..layoutW && relY in 0.0..layoutH
    }
}
