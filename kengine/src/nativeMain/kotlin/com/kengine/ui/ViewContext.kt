package com.kengine.ui

import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import com.kengine.math.Vec2

class ViewContext private constructor() : Context(), Logging {
    private val rootViews = mutableListOf<View>()

    /**
     * This will track whichever View is actively dragging.
     * If null, then no View has captured the mouse.
     */
    private var dragFocus: View? = null

    /**
     * This tracks whether the mouse was pressed during the previous handleMouseEvents() call.
     */
    private var wasPressed: Boolean = false

    fun addView(view: View) {
        rootViews.add(view)
    }

    fun performLayout() {
        for (root in rootViews) {
            root.performLayout(offsetX = 0.0, offsetY = 0.0)
        }
    }

    /**
     * A single method to handle mouse input each frame.
     *
     * @param mouseX current mouse X
     * @param mouseY current mouse Y
     * @param isCurrentlyPressed whether the left mouse button is down *this* frame
     * @param wasJustPressed whether a BUTTON_DOWN event occurred this frame (catches same-frame taps)
     */
    fun handleMouseEvents(
        mouseX: Double,
        mouseY: Double,
        isCurrentlyPressed: Boolean,
        wasJustPressed: Boolean = false
    ) {
        if (logger.isTraceEnabled()) {
            logger.trace {
                "handleMouseEvents: mouse=($mouseX, $mouseY), pressed=$isCurrentlyPressed, wasPressed=$wasPressed, justPressed=$wasJustPressed"
            }
        }

        when {
            // 1) Just pressed, or same-frame tap (DOWN+UP arrived in one poll cycle)
            !wasPressed && (isCurrentlyPressed || wasJustPressed) -> {
                dragFocus = null
                for (view in rootViews) {
                    view.click(mouseX, mouseY)
                    if (dragFocus != null) break
                }
            }

            // 2) Just released
            wasPressed && !isCurrentlyPressed -> {
                if (dragFocus != null) {
                    dragFocus?.release(mouseX, mouseY)
                    dragFocus = null
                } else {
                    rootViews.forEach { it.release(mouseX, mouseY) }
                }
            }

            // 3) Still pressed (held)
            wasPressed && isCurrentlyPressed -> {
                dragFocus?.hover(mouseX, mouseY)
            }

            // 4) Not pressed both frames => normal hover
            else -> {
                if (dragFocus != null) {
                    dragFocus?.hover(mouseX, mouseY)
                } else {
                    for (view in rootViews) {
                        view.hover(mouseX, mouseY)
                    }
                }
            }
        }

        // For same-frame taps, treat as still pressed so release fires next frame
        // (gives buttons one frame to show pressed visual state)
        wasPressed = isCurrentlyPressed || (!wasPressed && wasJustPressed)
    }

    /**
     * Called by a control that wants to capture mouse until release.
     */
    fun setDragFocus(view: View) {
        dragFocus = view
        if (logger.isDebugEnabled()) {
            logger.debug("setDragFocus -> ${view.id}")
        }
    }

    fun clearDragFocus(view: View) {
        if (dragFocus == view) {
            dragFocus = null
            if (logger.isDebugEnabled()) {
                logger.debug("clearDragFocus -> ${view.id}")
            }
        }
    }

    fun isDragging(view: View): Boolean = (dragFocus == view)

    fun render() {
        for (root in rootViews) {
            root.draw()
        }
    }

    fun clear() {
        rootViews.clear()
        dragFocus = null
        wasPressed = false
    }

    // Optional Vec2 helpers
    fun click(p: Vec2) = handleMouseEvents(p.x, p.y, true)
    fun hover(p: Vec2) = handleMouseEvents(p.x, p.y, false)

    override fun cleanup() {
        clear()
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
