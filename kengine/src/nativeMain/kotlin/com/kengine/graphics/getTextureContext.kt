package com.kengine.graphics

import com.kengine.context.ContextRegistry

inline fun getTextureContext(): TextureContext {
    return ContextRegistry.get<TextureContext>()
}
