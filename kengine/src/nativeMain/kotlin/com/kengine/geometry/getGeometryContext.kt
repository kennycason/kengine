package com.kengine.geometry

import com.kengine.context.ContextRegistry

fun getGeometryContext(): GeometryContext {
    return ContextRegistry.get<GeometryContext>()
}
