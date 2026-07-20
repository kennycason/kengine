package com.kengine.three.ui

import com.kengine.input.mouse.MouseInputEventSubscriber
import com.kengine.three.GpuFrame

class GpuUiContext3D {
    private val rootViews = mutableListOf<GpuUiView3D>()
    private var dragFocus: GpuUiView3D? = null
    private var wasPressed = false

    val roots: List<GpuUiView3D>
        get() = rootViews

    fun addView(view: GpuUiView3D): GpuUiView3D {
        rootViews += view
        return view
    }

    fun clear() {
        rootViews.clear()
        dragFocus = null
        wasPressed = false
    }

    fun performLayout() {
        rootViews.forEach { root ->
            root.performLayout()
        }
    }

    fun handleMouse(mouse: MouseInputEventSubscriber): Boolean {
        val cursor = mouse.cursor()
        return handleMouseEvents(
            mouseX = cursor.x,
            mouseY = cursor.y,
            isCurrentlyPressed = mouse.isLeftPressed(),
            wasJustPressed = mouse.wasLeftJustPressed()
        )
    }

    fun handleMouseEvents(
        mouseX: Double,
        mouseY: Double,
        isCurrentlyPressed: Boolean,
        wasJustPressed: Boolean = false
    ): Boolean {
        val handled = when {
            !wasPressed && isCurrentlyPressed -> {
                dragFocus = null
                rootViews.asReversed().any { it.click(this, mouseX, mouseY) }
            }

            !wasPressed && wasJustPressed -> {
                dragFocus = null
                val clicked = rootViews.asReversed().any { it.click(this, mouseX, mouseY) }
                releaseFocusedOrTopmost(mouseX, mouseY)
                clicked
            }

            wasPressed && !isCurrentlyPressed -> {
                releaseFocusedOrTopmost(mouseX, mouseY)
            }

            wasPressed && isCurrentlyPressed -> {
                dragFocus?.hover(this, mouseX, mouseY) ?: false
            }

            else -> {
                updateHover(mouseX, mouseY)
                false
            }
        }

        wasPressed = isCurrentlyPressed
        return handled
    }

    fun render(
        renderer: GpuUiRenderer3D,
        frame: GpuFrame
    ) {
        rootViews.forEach { root ->
            root.draw(renderer, frame.width, frame.height)
        }
    }

    fun setDragFocus(view: GpuUiView3D) {
        dragFocus = view
    }

    fun clearDragFocus(view: GpuUiView3D) {
        if (dragFocus == view) {
            dragFocus = null
        }
    }

    fun isDragging(view: GpuUiView3D): Boolean {
        return dragFocus == view
    }

    private fun releaseFocusedOrTopmost(
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        val focused = dragFocus
        return if (focused != null) {
            focused.release(this, mouseX, mouseY)
        } else {
            rootViews.asReversed().any { it.release(this, mouseX, mouseY) }
        }
    }

    private fun updateHover(
        mouseX: Double,
        mouseY: Double
    ) {
        rootViews.asReversed().forEach { view ->
            val handledByView = view.hover(this, mouseX, mouseY)
            if (!handledByView) {
                view.clearHover()
            }
        }
    }
}
