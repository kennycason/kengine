package com.kengine.graphics

import com.kengine.context.ContextRegistry

inline fun getSpriteContext(): SpriteContext {
    return ContextRegistry.get<SpriteContext>()
}
