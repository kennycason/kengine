package com.kengine.ui

import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import com.kengine.math.Vec2

class ViewContext private constructor() : Context(), Logging {
    private val rootViews = mutableListOf<View>()

    fun addView(view: View) {
        rootViews.add(view)
    }

    /**
     * Optionally call this each frame or whenever
     * the layout might need to be re-flowed.
     */
    fun performLayout() {
        rootViews.forEach { root ->
            // Start each root at (0,0) or some parent offset
            root.performLayout(offsetX = 0.0, offsetY = 0.0)
        }
    }

    /**
     * Handles mouse events based on position and press state.
     * We do not do the layout logic here—this just routes input
     * to the correct views.
     */
    fun handleMouseEvents(mouseX: Double, mouseY: Double, isPressed: Boolean) {
        if (logger.isTraceEnabled()) {
            logger.trace { "Mouse event at ($mouseX, $mouseY) - Pressed: $isPressed" }
        }

        // If pressed, treat as click; otherwise, treat as hover
        rootViews.forEach { view ->
            if (isPressed) {
                view.click(mouseX, mouseY)
            } else {
                view.hover(mouseX, mouseY)
            }
        }
    }

    /**
     * If you’re using e.g. Sliders that track dragging,
     * you can check if any is dragging.
     */
    fun isMousePressed(): Boolean {
        // If you wanted to check any view is dragging:
        return rootViews.any { it is Slider && it.isDragging }
    }

    /**
     * Handles mouse release events
     */
    fun releaseMouseEvents(mouseX: Double, mouseY: Double) {
        if (logger.isTraceEnabled()) {
            logger.trace { "Mouse release at ($mouseX, $mouseY)" }
        }
        rootViews.forEach { view ->
            view.release(mouseX, mouseY)
        }
    }

    /**
     * Renders all views in the context.
     * Optionally call `performLayout()` first each frame
     * if your UI layout changes dynamically.
     */
    fun render() {
        // If you want to re-flow each frame:
        // performLayout()

        rootViews.forEach { rootView ->
            rootView.draw() // uses final layout geometry
        }
    }

    /**
     * Clears all views from the context
     */
    fun clear() {
        rootViews.clear()
    }

    /**
     * Convenience methods for Vec2 input handling
     */
    fun click(p: Vec2) = click(p.x, p.y)
    fun hover(p: Vec2) = hover(p.x, p.y)

    fun click(x: Double, y: Double) {
        rootViews.forEach { view ->
            view.click(x, y)
        }
    }

    fun hover(x: Double, y: Double) {
        rootViews.forEach { view ->
            view.hover(x, y)
        }
    }

    /**
     * Cleanup resources
     */
    override fun cleanup() {
        rootViews.forEach { it.cleanup() }
        rootViews.clear()
    }

    companion object {
        private var currentContext: ViewContext? = null

        fun get(): ViewContext {
            return currentContext ?: ViewContext().also {
                currentContext = it
            }
        }
    }
}
