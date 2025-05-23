package com.kengine.ui

import com.kengine.font.Font
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.graphics.Sprite
import com.kengine.hooks.state.State
import com.kengine.hooks.state.useState
import com.kengine.log.Logging
import com.kengine.math.Vec2
import com.kengine.time.getCurrentNanoseconds

enum class Align { LEFT, CENTER, RIGHT }
enum class FlexDirection { ROW, COLUMN }

/**
 * Creates a new View and attaches it to the parent or root context.
 * After building the tree, call `parent.performLayout()` on the top-level
 * or root views to finalize layout.
 */
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
    hoverColor: Color? = null,
    parent: View? = null,
    block: View.() -> Unit = {}
): View {
    val view = View(
        id = id ?: "view-${getCurrentNanoseconds()}",
        desiredX = x,
        desiredY = y,
        desiredW = w,
        desiredH = h,
        bgColor = bgColor,
        bgSprite = bgImage,
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
        parent = parent,
        hoverColor = hoverColor
    )

    if (parent != null) {
        parent.addChild(view)
    } else {
        getViewContext().addView(view)
    }

    view.apply(block)
    return view
}

/**
 * TODO in future consider caching rendered views. First draw to an intermediate buffer, dynamically re-render as needed.
 */
open class View(
    val id: String = "",
    /**
     * User-specified or "desired" position and size.
     * We'll store final computed layout in layoutX, layoutY, layoutW, layoutH.
     */
    var desiredX: Double = 0.0,
    var desiredY: Double = 0.0,
    var desiredW: Double = 0.0,
    var desiredH: Double = 0.0,

    val bgColor: Color? = null,
    val bgSprite: Sprite? = null,
    val textFont: Font? = null,
    val align: Align = Align.LEFT,
    val direction: FlexDirection = FlexDirection.ROW,
    val padding: Double = 0.0,
    val spacing: Double = 0.0,
    val minWidth: Double = 0.0,
    val maxWidth: Double = Double.MAX_VALUE,
    val minHeight: Double = 0.0,
    val maxHeight: Double = Double.MAX_VALUE,
    var visible: Boolean = true,
    val parent: View? = null,
    protected val onClick: (() -> Unit)? = null,
    protected val onHover: (() -> Unit)? = null,
    protected val onRelease: (() -> Unit)? = null,
    open val hoverColor: Color? = null
) : Logging {

    /**
     * Final computed layout position and size after performing layout.
     * By default, these start at zero until `performLayout()` is called.
     */
    var layoutX: Double = 0.0
    var layoutY: Double = 0.0
    var layoutW: Double = 0.0
    var layoutH: Double = 0.0

    val children = mutableListOf<View>()

    /**
     * Add child, then possibly recalc layout if you want immediate results.
     */
    open fun addChild(view: View) {
        logger.debug { "Adding child ${view.id} to parent $id" }
        children.add(view)
        // Optionally call performLayout() here if you want incremental relayout.
    }

    /**
     * A top-down recursive layout pass:
     *  - sets this view's layoutX/Y/W/H from `desiredX/Y/W/H` (or parent's arrangement).
     *  - arranges children according to direction, padding, spacing, etc.
     *  - calls each child's performLayout() so they place themselves/descendants.
     */
    open fun performLayout(offsetX: Double = 0.0, offsetY: Double = 0.0) {
        // 1) This view’s final layout is: offset + desired
        layoutX = offsetX + desiredX
        layoutY = offsetY + desiredY
        // If you want a minWidth..maxWidth clamp:
        layoutW = desiredW.coerceIn(minWidth, maxWidth)
        layoutH = desiredH.coerceIn(minHeight, maxHeight)

        // 2) Now arrange children
        //    We'll do a simple "flex row" or "flex column" approach:
        //    Start childOffsetX/Y at our content area
        var childOffsetX = layoutX + padding
        var childOffsetY = layoutY + padding

        // First, we can do the "auto dimension" logic
        if (children.isNotEmpty()) {
            computeAutoDimensionsForChildren()
        }

        // Then position each child
        children.forEach { child ->
            child.performLayout(childOffsetX, childOffsetY)

            if (direction == FlexDirection.ROW) {
                // Move offset right by child’s layoutW + spacing
                childOffsetX += child.layoutW + spacing
            } else {
                // Move offset down by child’s layoutH + spacing
                childOffsetY += child.layoutH + spacing
            }
        }
    }

    /**
     * (Optional) If you want that "auto dimension" logic for children that have 0.0 w/h
     * we can do it similarly to your old logic:
     */
    protected fun computeAutoDimensionsForChildren() {
        // Calculate how many children need flexible width, etc.
        var fixedW = 0.0
        var flexibleCountW = 0
        var fixedH = 0.0
        var flexibleCountH = 0

        children.forEach { c ->
            if (c.desiredW == 0.0) flexibleCountW++ else fixedW += c.desiredW
            if (c.desiredH == 0.0) flexibleCountH++ else fixedH += c.desiredH
        }

        // Available space for children inside this view:
        val contentW = layoutW - padding * 2 - spacing * (children.size - 1)
        val contentH = layoutH - padding * 2 - spacing * (children.size - 1)

        val remainW = (contentW - fixedW).coerceAtLeast(0.0)
        val remainH = (contentH - fixedH).coerceAtLeast(0.0)

        // Assign each child's desiredW / desiredH if needed
        children.forEach { c ->
            if (c.desiredW == 0.0 && flexibleCountW > 0) {
                c.desiredW = remainW / flexibleCountW
            }
            if (c.desiredH == 0.0 && flexibleCountH > 0) {
                c.desiredH = remainH / flexibleCountH
            }
        }
    }

    /**
     * A separate pass for actually rendering, reading from layoutX, layoutY, etc.
     */
    open fun draw() {
        if (!visible) return

        if (logger.isTraceEnabled()) {
            logger.trace {
                "Drawing view $id at " +
                    "($layoutX, $layoutY), size: ${layoutW}x${layoutH}, parent=$parent"
            }
        }

        // 1) Draw background
        val currentBgColor = when {
            isHovered() && hoverColor != null -> hoverColor
            else -> bgColor
        }
        
        if (currentBgColor != null) {
            useGeometryContext {
                fillRectangle(layoutX, layoutY, layoutW, layoutH, currentBgColor)
            }
        }
        bgSprite?.draw(layoutX, layoutY)

        // 2) Draw children
        children.forEach { child ->
            child.draw()
        }
    }

    // ----------------------------------------------------------
    // Input Handling
    // ----------------------------------------------------------

    fun click(p: Vec2) = click(p.x, p.y)

    open fun click(mouseX: Double, mouseY: Double) {
        if (!visible) return
        if (isWithinBounds(mouseX, mouseY)) {
            onClick?.invoke()
        }
        // Pass to children
        children.forEach { it.click(mouseX, mouseY) }
    }

    private var isHovered = false
    
    fun hover(p: Vec2) = hover(p.x, p.y)

    open fun hover(mouseX: Double, mouseY: Double) {
        if (!visible) return
        val wasHovered = isHovered
        isHovered = isWithinBounds(mouseX, mouseY)
        
        if (isHovered && !wasHovered) {
            onHover?.invoke()
        }
        children.forEach { it.hover(mouseX, mouseY) }
    }
    
    fun isHovered(): Boolean = isHovered

    open fun release(mouseX: Double, mouseY: Double) {
        if (!visible) return
        if (isWithinBounds(mouseX, mouseY)) {
            onRelease?.invoke()
        }
        children.forEach { it.release(mouseX, mouseY) }
    }

    open fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= layoutX && mouseX <= layoutX + layoutW &&
            mouseY >= layoutY && mouseY <= layoutY + layoutH
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
        hoverColor: Color? = null,
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
            hoverColor = hoverColor,
            block = block
        )
    }

    fun text(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double,
        h: Double,
        text: String,
        font: Font,
        textColor: Color = Color.white,
        align: Align = Align.LEFT,
        padding: Double = 0.0,
        bgColor: Color? = null
    ): TextView {
        val textView = TextView(
            id = id,
            x = x,
            y = y,
            w = w,
            h = h,
            text = text,
            align = align,
            font = font,
            textColor = textColor,
            padding = padding,
            bgColor = bgColor,
            parent = this
        )
        addChild(textView)
        return textView
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
        w: Double,
        h: Double,
        onClick: (() -> Unit)? = null,
        onHover: (() -> Unit)? = null,
        onRelease: (() -> Unit)? = null,
        padding: Double = 0.0,
        bgColor: Color? = null,
        bgSprite: Sprite? = null,
        hoverColor: Color? = null,
        pressColor: Color? = null,
        isCircle: Boolean = false,
        isToggle: Boolean = false,
        onToggle: ((Boolean) -> Unit)? = null,
        isPressed: State<Boolean> = useState(false),
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
            buttonHoverColor = hoverColor,
            pressColor = pressColor,
            isCircle = isCircle,
            isToggle = isToggle,
            onToggle = onToggle,
            isPressed = isPressed,
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
        state: State<Double>,
        padding: Double = 0.0,
        bgColor: Color? = null,
        bgSprite: Sprite? = null,
        knobColor: Color = Color.gray10,
        indicatorColor: Color = Color.white,
        onValueChanged: ((Double) -> Unit)? = null,
        dragScale: Double = 200.0,
    ): Knob {
        val knob = Knob(
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
            knobColor = knobColor,
            indicatorColor = indicatorColor,
            onValueChanged = onValueChanged,
            dragScale = dragScale,
            parent = this
        )
        addChild(knob)
        return knob
    }

    fun sprite(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double,
        h: Double,
        sprite: Sprite,
        padding: Double = 0.0,
        bgColor: Color? = null,
        onClick: (() -> Unit)? = null,
        onHover: (() -> Unit)? = null
    ): SpriteView {
        val spriteView = SpriteView(
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
        addChild(spriteView)
        return spriteView
    }

    fun drawer(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        w: Double,
        h: Double,
        direction: DrawerDirection = DrawerDirection.DOWN,
        trigger: DrawerTrigger = DrawerTrigger.CLICK,
        bgColor: Color? = null,
        hoverColor: Color? = null,
        activeColor: Color? = null,
        padding: Double = 5.0,
        spacing: Double = 2.0,
        onClick: (() -> Unit)? = null,
        onHover: (() -> Unit)? = null,
        isOpen: State<Boolean> = useState(false),
        block: Drawer.() -> Unit = {}
    ): Drawer {
        val drawer = Drawer(
            id = id,
            x = x,
            y = y,
            w = w,
            h = h,
            drawerDirection = direction,
            trigger = trigger,
            bgColor = bgColor,
            drawerHoverColor = hoverColor,
            activeColor = activeColor,
            padding = padding,
            spacing = spacing,
            parent = this,
            onClick = onClick,
            onHover = onHover,
            isOpen = isOpen
        )

        addChild(drawer)
        drawer.block()
        return drawer
    }

    fun cleanup() {
        // Create a defensive copy of children to avoid concurrent modification issues
        val childrenCopy = children.toList()
        childrenCopy.forEach { it.cleanup() }
        children.clear()
    }
}
