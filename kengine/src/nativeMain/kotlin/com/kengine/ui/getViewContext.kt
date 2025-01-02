package com.kengine.ui

import com.kengine.hooks.context.ContextRegistry

fun getViewContext(): ViewContext {
    return ContextRegistry.get<ViewContext>()
}
