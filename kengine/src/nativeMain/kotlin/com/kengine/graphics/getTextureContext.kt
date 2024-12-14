package com.kengine.graphics

import com.kengine.hooks.context.ContextRegistry

fun getTextureContext(): TextureContext {
    return ContextRegistry.get<TextureContext>()
}
