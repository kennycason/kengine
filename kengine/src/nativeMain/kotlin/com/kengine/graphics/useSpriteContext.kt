package com.kengine.graphics

import com.kengine.context.useContextWithReturn

inline fun <R> useSpriteContext(cleanup: Boolean = false, block: SpriteContext.() -> R): R {
    return useContextWithReturn<SpriteContext, R>(cleanup, block)
}