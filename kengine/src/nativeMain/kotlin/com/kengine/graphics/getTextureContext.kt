package com.kengine.graphics

import com.kengine.context.ContextRegistry

fun getTextureContext(): TextureContext {
    return ContextRegistry.get<TextureContext>()
}
