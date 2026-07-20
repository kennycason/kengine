package com.kengine.three.ui

import com.kengine.graphics.Color
import com.kengine.math.Vec2
import kotlin.math.roundToInt

enum class GpuUiAlign3D {
    LEFT,
    CENTER,
    RIGHT
}

enum class GpuUiDirection3D {
    ROW,
    COLUMN
}

enum class GpuUiVerticalAlign3D {
    TOP,
    CENTER,
    BOTTOM
}

data class GpuUiRect3D(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
) {
    fun contains(
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        return mouseX >= x &&
            mouseX <= x + width &&
            mouseY >= y &&
            mouseY <= y + height
    }
}

open class GpuUiView3D(
    val id: String,
    var desiredX: Double = 0.0,
    var desiredY: Double = 0.0,
    var desiredWidth: Double = 0.0,
    var desiredHeight: Double = 0.0,
    var backgroundColor: Color? = null,
    var hoverColor: Color? = null,
    var direction: GpuUiDirection3D = GpuUiDirection3D.ROW,
    var padding: Double = 0.0,
    var spacing: Double = 0.0,
    var minWidth: Double = 0.0,
    var maxWidth: Double = Double.MAX_VALUE,
    var minHeight: Double = 0.0,
    var maxHeight: Double = Double.MAX_VALUE,
    var visible: Boolean = true,
    var onClick: (() -> Unit)? = null,
    var onHover: (() -> Unit)? = null,
    var onRelease: (() -> Unit)? = null
) {
    var layoutX: Double = 0.0
        private set
    var layoutY: Double = 0.0
        private set
    var layoutWidth: Double = 0.0
        private set
    var layoutHeight: Double = 0.0
        private set

    val children: List<GpuUiView3D>
        get() = mutableChildren

    private val mutableChildren = mutableListOf<GpuUiView3D>()
    private var hovered = false
    private var resolvedWidth: Double? = null
    private var resolvedHeight: Double? = null

    val bounds: GpuUiRect3D
        get() = GpuUiRect3D(layoutX, layoutY, layoutWidth, layoutHeight)

    fun isHovered(): Boolean {
        return hovered
    }

    open fun addChild(view: GpuUiView3D): GpuUiView3D {
        mutableChildren += view
        return view
    }

    private fun resolveLayoutSize(
        width: Double? = null,
        height: Double? = null
    ) {
        resolvedWidth = width
        resolvedHeight = height
    }

    open fun performLayout(
        offsetX: Double = 0.0,
        offsetY: Double = 0.0
    ) {
        val layoutDesiredWidth = resolvedWidth ?: desiredWidth
        val layoutDesiredHeight = resolvedHeight ?: desiredHeight
        resolvedWidth = null
        resolvedHeight = null

        layoutX = offsetX + desiredX
        layoutY = offsetY + desiredY
        layoutWidth = layoutDesiredWidth.coerceIn(minWidth, maxWidth)
        layoutHeight = layoutDesiredHeight.coerceIn(minHeight, maxHeight)

        if (mutableChildren.isNotEmpty()) {
            computeFlexibleChildren()
        }

        var childOffsetX = layoutX + padding
        var childOffsetY = layoutY + padding
        mutableChildren.forEach { child ->
            child.performLayout(childOffsetX, childOffsetY)
            if (direction == GpuUiDirection3D.ROW) {
                childOffsetX += child.layoutWidth + spacing
            } else {
                childOffsetY += child.layoutHeight + spacing
            }
        }
    }

    open fun draw(
        renderer: GpuUiRenderer3D,
        frameWidth: UInt,
        frameHeight: UInt
    ) {
        if (!visible) {
            return
        }

        val color = when {
            hovered && hoverColor != null -> hoverColor
            else -> backgroundColor
        }
        if (color != null) {
            renderer.rect(bounds, color, frameWidth, frameHeight)
        }
        mutableChildren.forEach { it.draw(renderer, frameWidth, frameHeight) }
    }

    open fun click(
        context: GpuUiContext3D,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        if (!visible || !isWithinBounds(mouseX, mouseY)) {
            return false
        }

        mutableChildren.asReversed().forEach { child ->
            if (child.click(context, mouseX, mouseY)) {
                return true
            }
        }
        onClick?.invoke()
        return true
    }

    open fun hover(
        context: GpuUiContext3D,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        if (!visible) {
            return false
        }

        val wasHovered = hovered
        hovered = isWithinBounds(mouseX, mouseY)
        if (hovered && !wasHovered) {
            onHover?.invoke()
        }

        if (!hovered) {
            mutableChildren.forEach { it.clearHover() }
            return false
        }

        mutableChildren.asReversed().forEach { child ->
            if (child.hover(context, mouseX, mouseY)) {
                return true
            }
        }
        return true
    }

    open fun release(
        context: GpuUiContext3D,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        if (!visible || !isWithinBounds(mouseX, mouseY)) {
            return false
        }

        mutableChildren.asReversed().forEach { child ->
            if (child.release(context, mouseX, mouseY)) {
                return true
            }
        }
        onRelease?.invoke()
        return true
    }

    open fun isWithinBounds(
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        return bounds.contains(mouseX, mouseY)
    }

    open fun clearHover() {
        hovered = false
        mutableChildren.forEach { it.clearHover() }
    }

    fun view(
        id: String,
        x: Double = 0.0,
        y: Double = 0.0,
        width: Double = 0.0,
        height: Double = 0.0,
        backgroundColor: Color? = null,
        hoverColor: Color? = null,
        direction: GpuUiDirection3D = GpuUiDirection3D.ROW,
        padding: Double = 0.0,
        spacing: Double = 0.0,
        onClick: (() -> Unit)? = null,
        block: GpuUiView3D.() -> Unit = {}
    ): GpuUiView3D {
        return addChild(
            GpuUiView3D(
                id = id,
                desiredX = x,
                desiredY = y,
                desiredWidth = width,
                desiredHeight = height,
                backgroundColor = backgroundColor,
                hoverColor = hoverColor,
                direction = direction,
                padding = padding,
                spacing = spacing,
                onClick = onClick
            ).apply(block)
        )
    }

    fun label(
        id: String,
        text: () -> String,
        width: Double,
        height: Double,
        x: Double = 0.0,
        y: Double = 0.0,
        color: Color = Color.white,
        align: GpuUiAlign3D = GpuUiAlign3D.LEFT,
        verticalAlign: GpuUiVerticalAlign3D = GpuUiVerticalAlign3D.CENTER
    ): GpuUiLabel3D {
        val label = GpuUiLabel3D(
            id = id,
            text = text,
            desiredX = x,
            desiredY = y,
            desiredWidth = width,
            desiredHeight = height,
            textColor = color,
            align = align,
            verticalAlign = verticalAlign
        )
        addChild(label)
        return label
    }

    fun button(
        id: String,
        text: () -> String,
        width: Double,
        height: Double,
        x: Double = 0.0,
        y: Double = 0.0,
        backgroundColor: Color,
        hoverColor: Color,
        pressColor: Color,
        textColor: Color = Color.white,
        onClick: () -> Unit
    ): GpuUiButton3D {
        val button = GpuUiButton3D(
            id = id,
            text = text,
            desiredX = x,
            desiredY = y,
            desiredWidth = width,
            desiredHeight = height,
            backgroundColor = backgroundColor,
            hoverColor = hoverColor,
            pressColor = pressColor,
            textColor = textColor,
            onClick = onClick
        )
        addChild(button)
        return button
    }

    fun slider(
        id: String,
        value: () -> Double,
        onValueChanged: (Double) -> Unit,
        width: Double,
        height: Double,
        x: Double = 0.0,
        y: Double = 0.0,
        min: Double = 0.0,
        max: Double = 1.0,
        trackColor: Color = Color.fromHex("313744"),
        fillColor: Color = Color.fromHex("5ca8ff"),
        handleColor: Color = Color.white
    ): GpuUiSlider3D {
        val slider = GpuUiSlider3D(
            id = id,
            value = value,
            onValueChanged = onValueChanged,
            desiredX = x,
            desiredY = y,
            desiredWidth = width,
            desiredHeight = height,
            min = min,
            max = max,
            trackColor = trackColor,
            fillColor = fillColor,
            handleColor = handleColor
        )
        addChild(slider)
        return slider
    }

    private fun computeFlexibleChildren() {
        var fixedWidth = 0.0
        var flexibleWidthCount = 0
        var fixedHeight = 0.0
        var flexibleHeightCount = 0

        mutableChildren.forEach { child ->
            if (child.desiredWidth == 0.0) {
                flexibleWidthCount++
            } else {
                fixedWidth += child.desiredWidth
            }
            if (child.desiredHeight == 0.0) {
                flexibleHeightCount++
            } else {
                fixedHeight += child.desiredHeight
            }
        }

        val contentWidth = layoutWidth - padding * 2.0 - spacing * (mutableChildren.size - 1)
        val contentHeight = layoutHeight - padding * 2.0 - spacing * (mutableChildren.size - 1)
        val remainingWidth = (contentWidth - fixedWidth).coerceAtLeast(0.0)
        val remainingHeight = (contentHeight - fixedHeight).coerceAtLeast(0.0)

        mutableChildren.forEach { child ->
            child.resolveLayoutSize(
                width = if (child.desiredWidth == 0.0 && flexibleWidthCount > 0) {
                    remainingWidth / flexibleWidthCount
                } else {
                    null
                },
                height = if (child.desiredHeight == 0.0 && flexibleHeightCount > 0) {
                    remainingHeight / flexibleHeightCount
                } else {
                    null
                }
            )
        }
    }
}

class GpuUiLabel3D(
    id: String,
    var text: () -> String,
    desiredX: Double = 0.0,
    desiredY: Double = 0.0,
    desiredWidth: Double,
    desiredHeight: Double,
    var textColor: Color = Color.white,
    var align: GpuUiAlign3D = GpuUiAlign3D.LEFT,
    var verticalAlign: GpuUiVerticalAlign3D = GpuUiVerticalAlign3D.CENTER
) : GpuUiView3D(
    id = id,
    desiredX = desiredX,
    desiredY = desiredY,
    desiredWidth = desiredWidth,
    desiredHeight = desiredHeight
) {
    override fun draw(
        renderer: GpuUiRenderer3D,
        frameWidth: UInt,
        frameHeight: UInt
    ) {
        if (!visible) {
            return
        }
        renderer.text(
            text = text(),
            rect = bounds,
            color = textColor,
            align = align,
            verticalAlign = verticalAlign,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
        children.forEach { it.draw(renderer, frameWidth, frameHeight) }
    }
}

class GpuUiButton3D(
    id: String,
    var text: () -> String,
    desiredX: Double = 0.0,
    desiredY: Double = 0.0,
    desiredWidth: Double,
    desiredHeight: Double,
    backgroundColor: Color,
    hoverColor: Color,
    private val pressColor: Color,
    var textColor: Color = Color.white,
    onClick: () -> Unit
) : GpuUiView3D(
    id = id,
    desiredX = desiredX,
    desiredY = desiredY,
    desiredWidth = desiredWidth,
    desiredHeight = desiredHeight,
    backgroundColor = backgroundColor,
    hoverColor = hoverColor,
    onClick = onClick
) {
    private var pressed = false

    override fun draw(
        renderer: GpuUiRenderer3D,
        frameWidth: UInt,
        frameHeight: UInt
    ) {
        if (!visible) {
            return
        }
        val color = when {
            pressed -> pressColor
            isHovered() -> hoverColor
            else -> backgroundColor
        }
        renderer.rect(bounds, color ?: Color.fromHex("303744"), frameWidth, frameHeight)
        renderer.text(
            text = text(),
            rect = bounds,
            color = textColor,
            align = GpuUiAlign3D.CENTER,
            verticalAlign = GpuUiVerticalAlign3D.CENTER,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
        children.forEach { it.draw(renderer, frameWidth, frameHeight) }
    }

    override fun click(
        context: GpuUiContext3D,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        if (!visible || !isWithinBounds(mouseX, mouseY)) {
            return false
        }
        pressed = true
        context.setDragFocus(this)
        return super.click(context, mouseX, mouseY)
    }

    override fun release(
        context: GpuUiContext3D,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        pressed = false
        context.clearDragFocus(this)
        return super.release(context, mouseX, mouseY)
    }
}

class GpuUiSlider3D(
    id: String,
    var value: () -> Double,
    var onValueChanged: (Double) -> Unit,
    desiredX: Double = 0.0,
    desiredY: Double = 0.0,
    desiredWidth: Double,
    desiredHeight: Double,
    var min: Double = 0.0,
    var max: Double = 1.0,
    var trackColor: Color = Color.fromHex("313744"),
    var fillColor: Color = Color.fromHex("5ca8ff"),
    var handleColor: Color = Color.white
) : GpuUiView3D(
    id = id,
    desiredX = desiredX,
    desiredY = desiredY,
    desiredWidth = desiredWidth,
    desiredHeight = desiredHeight
) {
    private var dragging = false

    override fun draw(
        renderer: GpuUiRenderer3D,
        frameWidth: UInt,
        frameHeight: UInt
    ) {
        if (!visible) {
            return
        }

        val trackHeight = 6.0
        val trackY = layoutY + (layoutHeight - trackHeight) * 0.5
        val fraction = fractionForValue(value())
        val handleSize = 14.0
        val handleCenterX = layoutX + layoutWidth * fraction

        renderer.rect(GpuUiRect3D(layoutX, trackY, layoutWidth, trackHeight), trackColor, frameWidth, frameHeight)
        renderer.rect(GpuUiRect3D(layoutX, trackY, layoutWidth * fraction, trackHeight), fillColor, frameWidth, frameHeight)
        renderer.rect(
            GpuUiRect3D(
                x = handleCenterX - handleSize * 0.5,
                y = layoutY + (layoutHeight - handleSize) * 0.5,
                width = handleSize,
                height = handleSize
            ),
            handleColor,
            frameWidth,
            frameHeight
        )
    }

    override fun click(
        context: GpuUiContext3D,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        if (!visible || !isWithinBounds(mouseX, mouseY)) {
            return false
        }
        dragging = true
        context.setDragFocus(this)
        updateValue(mouseX)
        return true
    }

    override fun hover(
        context: GpuUiContext3D,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        if (dragging && context.isDragging(this)) {
            updateValue(mouseX)
            return true
        }
        return super.hover(context, mouseX, mouseY)
    }

    override fun release(
        context: GpuUiContext3D,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        if (dragging) {
            dragging = false
            context.clearDragFocus(this)
            updateValue(mouseX)
            return true
        }
        return super.release(context, mouseX, mouseY)
    }

    private fun updateValue(mouseX: Double) {
        val fraction = ((mouseX - layoutX) / layoutWidth).coerceIn(0.0, 1.0)
        val nextValue = min + (max - min) * fraction
        onValueChanged(nextValue)
    }

    private fun fractionForValue(currentValue: Double): Double {
        if (max == min) {
            return 0.0
        }
        return ((currentValue - min) / (max - min)).coerceIn(0.0, 1.0)
    }
}

internal fun Double.roundedUiInt3D(): Int {
    return roundToInt()
}
