package com.kengine.ui

import com.kengine.hooks.context.Context
import com.kengine.log.Logging

class ViewContext private constructor(): Context(), Logging {
    private val rootViews = mutableListOf<View>()

    fun addView(view: View) {
        rootViews.add(view)
    }

    fun render() {
        rootViews.forEach { rootView ->
            logger.debug { "Rendering root view ${rootView.id} at (${rootView.x}, ${rootView.y}) size: ${rootView.w}x${rootView.h}" }
            rootView.render()
        }
    }

    fun clear() {
        rootViews.clear()
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
