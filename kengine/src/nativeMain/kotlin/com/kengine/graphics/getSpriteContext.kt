package com.kengine.graphics

import com.kengine.context.ContextRegistry

fun getSpriteContext(): SpriteContext {
    return ContextRegistry.get<SpriteContext>()
}
