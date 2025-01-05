package com.kengine.ui

import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import com.kengine.math.Vec2

class ViewContext private constructor(): Context(), Logging {
    private val rootViews = mutableListOf<View>()

    fun addView(view: View) {
        rootViews.add(view)
    }

    fun handleMouseEvents(mouseX: Double, mouseY: Double, isPressed: Boolean) {
        if (isPressed) {
            rootViews.forEach { it.click(mouseX, mouseY) }
        }
        rootViews.forEach { it.hover(mouseX, mouseY) }
    }

    fun releaseMouseEvents(mouseX: Double, mouseY: Double) {
        rootViews.forEach { it.release(mouseX, mouseY) }
    }

    fun render() {
        rootViews.forEach { rootView ->
            if (logger.isTraceEnabled()) {
                logger.trace { "Rendering root view ${rootView.id} at (${rootView.x}, ${rootView.y}) size: ${rootView.w}x${rootView.h}" }
            }
            rootView.draw()
        }
    }

    fun clear() {
        rootViews.clear()
    }

    fun click(p: Vec2) {
        rootViews.forEach { it.click(p) }
    }

    fun click(x: Double, y: Double) {
        rootViews.forEach { it.click(x, y) }
    }

    fun hover(p: Vec2) {
        rootViews.forEach { it.hover(p) }
    }

    fun hover(x: Double, y: Double) {
        rootViews.forEach { it.hover(x, y) }
    }

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
