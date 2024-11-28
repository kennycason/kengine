package com.kengine.geometry

import com.kengine.context.ContextRegistry

inline fun getGeometryContext(): GeometryContext {
    return ContextRegistry.get<GeometryContext>()
}
