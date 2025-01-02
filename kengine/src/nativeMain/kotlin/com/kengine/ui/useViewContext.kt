package com.kengine.ui

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useViewContext(cleanup: Boolean = false, block: ViewContext.() -> R): R {
    return useContextWithReturn<ViewContext, R>(cleanup, block)
}
