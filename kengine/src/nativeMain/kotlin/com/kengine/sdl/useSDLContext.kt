package com.kengine.sdl

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useSDLContext(cleanup: Boolean = false, block: SDLContext.() -> R): R {
    return useContextWithReturn<SDLContext, R>(cleanup, block)
}