package com.kengine.geometry

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useGeometryContext(cleanup: Boolean = false, block: GeometryContext.() -> R): R {
    return useContextWithReturn<GeometryContext, R>(cleanup, block)
}
