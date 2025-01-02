package com.kengine.ui

import com.kengine.font.Font
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.log.Logging
import com.kengine.time.getCurrentNanoseconds

enum class Align { LEFT, CENTER, RIGHT }
enum class FlexDirection { ROW, COLUMN }

fun useView(
    id: String? = null,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double = 100.0,
    h: Double = 100.0,
    bgColor: Color? = null,
    bgImage: Sprite? = null,
    text: String? = null,
    textColor: Color = Color.white,
    align: Align = Align.LEFT,
    direction: FlexDirection = FlexDirection.ROW,
    padding: Double = 0.0,
    spacing: Double = 0.0,
    onClick: (() -> Unit)? = null,
    onHover: (() -> Unit)? = null,
    parent: View? = null,
    block: View.() -> Unit = {}
): View {
    val view = View(
        id = id ?: "view-${getCurrentNanoseconds()}",
        x = x,
        y = y,
        w = w,
        h = h,
        bgColor = bgColor,
        bgImage = bgImage,
        text = text,
        textColor = textColor,
        align = align,
        direction = direction,
        padding = padding,
        spacing = spacing,
        onClick = onClick,
        onHover = onHover
    )

    // if there's a parent, add to parent, otherwise add to root context
    if (parent != null) {
        parent.addChild(view)
    } else {
        getViewContext().addView(view)
    }

    // apply configuration block which may contain children nodes
    view.apply(block)
    return view
}

open class View(
    val id: String = "",
    var x: Double = 0.0,
    var y: Double = 0.0,
    var w: Double = 100.0,
    var h: Double = 100.0,
    var bgColor: Color? = null,
    var bgImage: Sprite? = null,
    var textColor: Color = Color.white,
    var text: String? = null,
    var textFont: Font? = null,
    var align: Align = Align.LEFT,
    var direction: FlexDirection = FlexDirection.ROW,
    var padding: Double = 0.0,
    var spacing: Double = 0.0,
    var visible: Boolean = true,
    onClick: (() -> Unit)? = null,
    onHover: (() -> Unit)? = null,
) : Logging {

    private val children = mutableListOf<View>()
    private var drawCallback: ((View) -> Unit)? = null
    private var clickCallback: (() -> Unit)? = onClick
    private var hoverCallback: (() -> Unit)? = onHover

    fun addChild(view: View) {
        logger.debug { "Adding child ${view.id} to parent ${this.id}" }
        children.add(view)
    }

    fun render(parentX: Double = 0.0, parentY: Double = 0.0) {
        if (!visible) return

        // calculate this view's absolute position
        val absX = parentX + x
        val absY = parentY + y

        logger.debug { "Rendering view $id at ($absX, $absY) size: ${w}x${h}" }

        // draw this view
        if (bgColor != null) {
            useGeometryContext {
                fillRectangle(absX.toInt(), absY.toInt(), w.toInt(), h.toInt(), bgColor!!)
            }
        }

        bgImage?.draw(absX, absY)

        // calculate child positions during render
        var childX = absX + padding
        var childY = absY + padding

        children.forEach { child ->
            logger.debug { "Rendering child ${child.id} at ($childX, $childY)" }
            child.render(childX, childY)

            if (direction == FlexDirection.ROW) {
                childX += child.w + spacing
            } else {
                childY += child.h + spacing
            }
        }
    }

    fun view(
        id: String? = null,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double = 100.0,
        h: Double = 100.0,
        bgColor: Color? = null,
        bgImage: Sprite? = null,
        text: String? = null,
        textColor: Color = Color.white,
        align: Align = Align.LEFT,
        direction: FlexDirection = FlexDirection.ROW,
        padding: Double = 0.0,
        spacing: Double = 0.0,
        block: View.() -> Unit = {}
    ): View {
        return useView(
            id = id,
            x = x,
            y = y,
            w = w,
            h = h,
            bgColor = bgColor,
            bgImage = bgImage,
            text = text,
            textColor = textColor,
            align = align,
            direction = direction,
            padding = padding,
            spacing = spacing,
            parent = this, // Pass *this* as the parent!
            block = block
        )
    }

    fun cleanup() {
        children.forEach { it.cleanup() }
    }
}
