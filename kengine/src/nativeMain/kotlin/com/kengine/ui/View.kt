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
    private val onRelease: (() -> Unit)? = null
) : Logging {

    private val children = mutableListOf<View>()

    // calculate dimensions based on parent constraints
    protected fun calculateDimensions() {
        if (children.isEmpty()) return

        // width calculation
        var fixedWidth = 0.0
        var flexibleWidthChildren = 0

        // height calculation
        var fixedHeight = 0.0
        var flexibleHeightChildren = 0

        // First pass: count flexible children and sum fixed dimensions
        children.forEach { child ->
            if (child.w == 0.0) {
                flexibleWidthChildren++
            } else {
                fixedWidth += child.w
            }

            if (child.h == 0.0) {
                flexibleHeightChildren++
            } else {
                fixedHeight += child.h
            }
        }

        // Calculate available space
        val remainingWidth = w - padding * 2 - spacing * (children.size - 1) - fixedWidth
        val remainingHeight = h - padding * 2 - spacing * (children.size - 1) - fixedHeight

        // Calculate auto dimensions
        val autoWidth = if (flexibleWidthChildren > 0) remainingWidth / flexibleWidthChildren else 0.0
        val autoHeight = if (flexibleHeightChildren > 0) remainingHeight / flexibleHeightChildren else 0.0

        // Second pass: assign dimensions to flexible children
        children.forEach { child ->
            if (child.w == 0.0) {
                child.w = autoWidth.coerceAtLeast(0.0)
            }
            if (child.h == 0.0) {
                child.h = autoHeight.coerceAtLeast(0.0)
            }
        }
    }

    /**
     * Handles release events, adjusted for absolute positioning.
     */
    open fun release(x: Double, y: Double) {
        // Clear active drag lock if this view was active
        if (activeDragView == this) {
            activeDragView = null
        }

        if (!visible) return // Don't process further if invisible

        val (absX, absY) = getAbsolutePosition()

        // Trigger release callback
        if (x >= absX && x <= absX + w && y >= absY && y <= absY + h) {
            onRelease?.invoke()
        }

        // Propagate release to children
        children.forEach { child ->
            child.release(x, y)
        }
    }

    fun addChild(view: View) {
        logger.debug { "Adding child ${view.id} to parent ${this.id}" }
        children.add(view)
        calculateDimensions() // recalculate sizes whenever a child is added
    }

    open fun draw(parentX: Double = 0.0, parentY: Double = 0.0) {
        if (!visible) return

        // Calculate absolute coordinates including any x/y offset
        val absX = parentX + x
        val absY = parentY + y

        if (logger.isTraceEnabled()) {
            logger.trace { "Rendering view $id at ($absX, $absY) size: ${w}x${h}, parent: ${parent?.id}" }
        }

        // Draw background and image
        if (bgColor != null) {
            useGeometryContext {
                fillRectangle(absX, absY, w, h, bgColor)
            }
        }
        bgImage?.draw(absX, absY)

        // Track relative position for child layout
        var nextChildX = 0.0
        var nextChildY = 0.0

        // Add padding only once at the start of child positioning
        if (children.isNotEmpty()) {
            nextChildX += padding
            nextChildY += padding
        }

        children.forEach { child ->
            // Pass absolute coordinates to child
            child.draw(absX + nextChildX, absY + nextChildY)

            // Update next position based on direction
            if (direction == FlexDirection.ROW) {
                nextChildX += child.w + spacing
            } else {
                nextChildY += child.h + spacing
            }
        }
    }

    fun click(p: Vec2) {
        click(p.x, p.y)
    }

    /**
     * Handles click events, adjusted for absolute positioning.
     */
    open fun click(x: Double, y: Double) {
        if (!visible) return

        // Calculate absolute coordinates like in draw()
        val absX = if (parent != null) x else 0.0
        val absY = if (parent != null) y else 0.0

        var nextChildX = padding
        var nextChildY = padding

        children.forEach { child ->
            child.click(x - (absX + nextChildX), y - (absY + nextChildY))

            if (direction == FlexDirection.ROW) {
                nextChildX += child.w + spacing
            } else {
                nextChildY += child.h + spacing
            }
        }

        if (isWithinBounds(x, y)) {
            onClick?.invoke()
        }
    }

    fun hover(p: Vec2) {
        hover(p.x, p.y)
    }

    /**
     * Handles hover events, adjusted for absolute positioning.
     */
    open fun hover(x: Double, y: Double) {
        if (!visible) return

        // Ignore hover events if another view is active
        if (activeDragView != null && activeDragView != this) return

        val (absX, absY) = getAbsolutePosition()

        // Check bounds for the current view
        if (x >= absX && x <= absX + w && y >= absY && y <= absY + h) {
            onHover?.invoke()
        }

        // Propagate hover to children with relative offsets
        children.forEach { child ->
            child.hover(x, y)
        }
    }

    /**
     * Checks if a point (mouseX, mouseY) is within this view's bounds, using absolute position.
     */
    open fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        val (absX, absY) = getAbsolutePosition()
        return mouseX >= absX && mouseX <= absX + w &&
            mouseY >= absY && mouseY <= absY + h
    }

    /**
     * Computes the absolute position by traversing up the parent hierarchy.
     */
    fun getAbsolutePosition(): Pair<Double, Double> {
        var absX = x
        var absY = y
        var parentView = parent

        while (parentView != null) {
            absX += parentView.x
            absY += parentView.y
            parentView = parentView.parent
        }
        return Pair(absX, absY)
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
        w: Double,  // Required
        h: Double,  // Required
        min: Double = 0.0,
        max: Double = 100.0,
        state: State<Double>,
        padding: Double = 0.0,
        bgColor: Color? = null,
        bgSprite: Sprite? = null,
        trackWidth: Double? = null,
        trackColor: Color = Color.gray10,
        handleWidth: Double? = null,
        handleHeight: Double? = null,
        handleColor: Color = Color.white,
        handleSprite: Sprite? = null,
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
            bgColor = bgColor,
            bgSprite = bgSprite,
            trackWidth = trackWidth,
            trackColor = trackColor,
            handleWidth = handleWidth,
            handleHeight = handleHeight,
            handleColor = handleColor,
            handleSprite = handleSprite,
            onValueChanged = onValueChanged,
            parent = this
        )
        addChild(slider)
        return slider
    }

    fun button(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double,  // Required
        h: Double,  // Required
        onClick: (() -> Unit)? = null,
        onHover: (() -> Unit)? = null,
        onRelease: (() -> Unit)? = null,
        padding: Double = 0.0,
        bgColor: Color? = null,
        bgSprite: Sprite? = null,
        hoverColor: Color? = null,
        pressColor: Color? = null,
        isCircle: Boolean = false
    ): Button {
        val button = Button(
            id = id,
            x = x,
            y = y,
            w = w,
            h = h,
            onClick = onClick,
            onHover = onHover,
            onRelease = onRelease,
            padding = padding,
            bgColor = bgColor,
            bgSprite = bgSprite,
            hoverColor = hoverColor,
            pressColor = pressColor,
            isCircle = isCircle,
            parent = this
        )
        addChild(button)
        return button
    }

    fun toggleButton(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double,  // Required
        h: Double,  // Required
        state: State<Boolean>,
        onToggle: ((Boolean) -> Unit)? = null,
        onHover: (() -> Unit)? = null,
        padding: Double = 0.0,
        bgColor: Color? = null,
        bgSprite: Sprite? = null,
        hoverColor: Color? = null,
        activeColor: Color? = null,
        activeHoverColor: Color? = null,
        isCircle: Boolean = false
    ): ToggleButton {
        val button = ToggleButton(
            id = id,
            x = x,
            y = y,
            w = w,
            h = h,
            state = state,
            onToggle = onToggle,
            onHover = onHover,
            padding = padding,
            bgColor = bgColor,
            bgSprite = bgSprite,
            hoverColor = hoverColor,
            activeColor = activeColor,
            activeHoverColor = activeHoverColor,
            isCircle = isCircle,
            parent = this
        )
        addChild(button)
        return button
    }

    fun knob(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double,  // Required
        h: Double,  // Required
        min: Double = 0.0,
        max: Double = 100.0,
        stepSize: Double? = null,
        state: State<Double>,
        padding: Double = 0.0,
        bgColor: Color? = null,
        bgSprite: Sprite? = null,
        knobColor: Color = Color.gray10,
        indicatorColor: Color = Color.white,
        onValueChanged: ((Double) -> Unit)? = null
    ): Knob {
        val knob = Knob(
            id = id,
            x = x,
            y = y,
            w = w,
            h = h,
            min = min,
            max = max,
            stepSize = stepSize,
            state = state,
            padding = padding,
            bgColor = bgColor,
            bgSprite = bgSprite,
            knobColor = knobColor,
            indicatorColor = indicatorColor,
            onValueChanged = onValueChanged,
            parent = this
        )
        addChild(knob)
        return knob
    }

    fun image(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double,  // Required
        h: Double,  // Required
        sprite: Sprite,
        padding: Double = 0.0,
        bgColor: Color? = null,
        onClick: (() -> Unit)? = null,
        onHover: (() -> Unit)? = null
    ): Image {
        val image = Image(
            id = id,
            x = x,
            y = y,
            w = w,
            h = h,
            padding = padding,
            sprite = sprite,
            bgColor = bgColor,
            onClick = onClick,
            onHover = onHover,
            parent = this
        )
        addChild(image)
        return image
    }

    fun cleanup() {
        children.forEach { it.cleanup() }
    }

    companion object {
        var activeDragView: View? = null // Tracks the currently active view
    }
}
