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
     * Handles mouse events based on position and press state
     */
    fun handleMouseEvents(mouseX: Double, mouseY: Double, isPressed: Boolean) {
        if (logger.isTraceEnabled()) {
            logger.trace { "Mouse event at ($mouseX, $mouseY) - Pressed: $isPressed" }
        }

        // Handle click or hover based on the press state
        rootViews.forEach { view ->
            if (isPressed) {
                view.click(mouseX, mouseY)
            } else {
                view.hover(mouseX, mouseY)
            }
        }
    }

    fun isMousePressed(): Boolean {
        return rootViews.any { it is Slider && it.isDragging }
    }

    /**
     * Handles mouse release events
     */
    fun releaseMouseEvents(mouseX: Double, mouseY: Double) {
        if (logger.isTraceEnabled()) {
            logger.trace { "Mouse release at ($mouseX, $mouseY)" }
        }

        // Handle release events for all root views
        rootViews.forEach { view ->
            view.release(mouseX, mouseY)
        }
    }

    /**
     * Renders all views in the context
     */
    fun render() {
        rootViews.forEach { rootView ->
            rootView.draw()
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

    /**
     * Click handling without dragging checks
     */
    fun click(x: Double, y: Double) {
        rootViews.forEach { view ->
            view.click(x, y)
        }
    }

    /**
     * Hover handling without dragging checks
     */
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
