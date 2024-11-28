package com.kengine.graphics

import com.kengine.context.useContextWithReturn

inline fun <R> useTextureContext(cleanup: Boolean = false, block: TextureContext.() -> R): R {
    return useContextWithReturn<TextureContext, R>(cleanup, block)
}