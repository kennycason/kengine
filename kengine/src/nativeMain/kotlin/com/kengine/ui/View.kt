package com.kengine.ui

import com.kengine.font.Font
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.hooks.state.State
import com.kengine.log.Logging
import com.kengine.math.Vec2
import com.kengine.time.getCurrentNanoseconds

enum class Align { LEFT, CENTER, RIGHT }
enum class FlexDirection { ROW, COLUMN }

fun useView(
    id: String? = null,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double = 0.0,
    h: Double = 0.0,
    bgColor: Color? = null,
    bgImage: Sprite? = null,
    text: String? = null,
    textColor: Color = Color.white,
    align: Align = Align.LEFT,
    direction: FlexDirection = FlexDirection.ROW,
    padding: Double = 0.0,
    spacing: Double = 0.0,
    minWidth: Double = 0.0,
    maxWidth: Double = Double.MAX_VALUE,
    minHeight: Double = 0.0,
    maxHeight: Double = Double.MAX_VALUE,
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
        minWidth = minWidth,
        maxWidth = maxWidth,
        minHeight = minHeight,
        maxHeight = maxHeight,
        onClick = onClick,
        onHover = onHover,
        parent = parent
    )

    // attach to parent or root context
    if (parent != null) {
        parent.addChild(view)
    } else {
        getViewContext().addView(view)
    }

    view.apply(block)
    return view
}

open class View(
    val id: String = "",
    val x: Double = 0.0,
    val y: Double = 0.0,
    var w: Double = 0.0,
    var h: Double = 0.0,
    val bgColor: Color? = null,
    val bgImage: Sprite? = null,
    val textColor: Color = Color.white,
    val text: String? = null,
    val textFont: Font? = null,
    val align: Align = Align.LEFT,
    val direction: FlexDirection = FlexDirection.ROW,
    val padding: Double = 0.0,
    val spacing: Double = 0.0,
    val minWidth: Double = 0.0,
    val maxWidth: Double = Double.MAX_VALUE,
    val minHeight: Double = 0.0,
    val maxHeight: Double = Double.MAX_VALUE,
    val visible: Boolean = true,
    val parent: View? = null,
    private val onClick: (() -> Unit)? = null,
    private val onHover: (() -> Unit)? = null,
) : Logging {

    private val children = mutableListOf<View>()

    // calculate dimensions based on parent constraints
    protected fun calculateDimensions() {
        if (children.isEmpty()) return

        // width calculation
        var fixedWidth = 0.0
        var flexibleWidthChildren = 0

        children.forEach { child ->
            if (child.w == 0.0) flexibleWidthChildren++ else fixedWidth += child.w
        }

        val remainingWidth = w - padding * 2 - spacing * (children.size - 1) - fixedWidth
        val autoWidth = if (flexibleWidthChildren > 0) remainingWidth / flexibleWidthChildren else 0.0

        // height calculation
        val parentHeight = h - padding * 2 // parent's effective height
        children.forEach { child ->
            if (child.w == 0.0) {
                child.w = autoWidth
            }
            children.forEach { child ->
                if (child.w == 0.0) child.w = autoWidth
                if (child.h == 0.0) child.h = parentHeight.coerceAtLeast(0.0)
            }
        }
    }

    fun addChild(view: View) {
        logger.debug { "Adding child ${view.id} to parent ${this.id}" }
        children.add(view)
        calculateDimensions() // recalculate sizes whenever a child is added
    }

    open fun draw(parentX: Double = 0.0, parentY: Double = 0.0) {
        if (!visible) return

        val absX = parentX + x
        val absY = parentY + y

        if (logger.isTraceEnabled()) {
            logger.trace { "Rendering view $id at ($absX, $absY) size: ${w}x${h}, parent: ${parent?.id}" }
        }

        // draw this view
        if (bgColor != null) {
            useGeometryContext {
                fillRectangle(absX, absY, w, h, bgColor)
            }
        }
        bgImage?.draw(absX, absY)

        var childX = absX + padding
        var childY = absY + padding

        children.forEach { child ->
            child.draw(childX, childY)
            if (direction == FlexDirection.ROW) {
                childX += child.w + spacing
            } else {
                childY += child.h + spacing
            }
        }
    }

    fun click(p: Vec2) {
        click(p.x, p.y)
    }

    open fun click(x: Double, y: Double) {
        if (!visible) return

        val absX = this.x
        val absY = this.y

        // check bounds for the current view
        if (x >= absX && x <= absX + w && y >= absY && y <= absY + h) {
            onClick?.invoke()
        }

        // propagate click to children with relative offsets
        var childX = absX + padding
        var childY = absY + padding

        children.forEach { child ->
            child.click(x - childX, y - childY)

            if (direction == FlexDirection.ROW) {
                childX += child.w + spacing
            } else {
                childY += child.h + spacing
            }
        }
    }

    fun hover(p: Vec2) {
        hover(p.x, p.y)
    }

    open fun hover(x: Double, y: Double) {
        if (!visible) return

        val absX = this.x
        val absY = this.y

        // check bounds for the current view
        if (x >= absX && x <= absX + w && y >= absY && y <= absY + h) {
            onHover?.invoke()
        }

        // propagate hover to children with relative offsets
        var childX = absX + padding
        var childY = absY + padding

        children.forEach { child ->
            child.hover(x - childX, y - childY)

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
        w: Double = 0.0,
        h: Double = 0.0,
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
            parent = this,
            onClick = onClick,
            onHover = onHover,
            block = block
        )
    }

    fun slider(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double = 16.0,
        h: Double = 100.0,
        min: Double = 0.0,
        max: Double = 100.0,
        state: State<Double>,
        padding: Double = 0.0,
        bgColor: Color = Color.gray10,
        handleColor: Color = Color.white,
        onValueChanged: ((Double) -> Unit)? = null
    ): Slider {
        val slider = Slider(
            id = id,
            x = x,
            y = y,
            w = w,
            h = h,
            min = min,
            max = max,
            state = state,
            padding = padding,
            onValueChanged = onValueChanged,
            bgColor = bgColor,
            handleColor = handleColor,
            parent = this // Attach to current parent View
        )
        addChild(slider) // Add slider as a child of the current view
        return slider
    }

    fun cleanup() {
        children.forEach { it.cleanup() }
    }
}
