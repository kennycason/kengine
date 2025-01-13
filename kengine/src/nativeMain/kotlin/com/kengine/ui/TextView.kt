package com.kengine.ui

import com.kengine.font.Font
import com.kengine.graphics.Color

class TextView(
    id: String,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,
    h: Double,
    val text: String,
    val font: Font,
    val textColor: Color = Color.white,
    align: Align = Align.LEFT,
    padding: Double = 0.0,
    bgColor: Color? = null,
    parent: View? = null
) : View(
    id = id,
    desiredX = x,
    desiredY = y,
    desiredW = w,
    desiredH = h,
    padding = padding,
    bgColor = bgColor,
    align = align,
    parent = parent
) {
    override fun draw() {
        super.draw()

        // Calculate the text's starting position based on alignment
        val textWidth = font.measureTextWidth(text)
        val adjustedX = when (align) {
            Align.LEFT -> layoutX.toInt() + padding.toInt()
            Align.CENTER -> (layoutX + (desiredW - textWidth) / 2).toInt()
            Align.RIGHT -> (layoutX + desiredW - textWidth - padding).toInt()
        }
        val adjustedY = layoutY.toInt() + padding.toInt() // Y alignment can be extended if needed

        // Draw the text
        font.drawText(
            text = text,
            x = adjustedX,
            y = adjustedY,
            color = textColor,
            caching = true
        )
    }
}
