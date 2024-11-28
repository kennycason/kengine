package com.kengine.sdl

import com.kengine.context.useContextWithReturn

inline fun <R> useSDLEventContext(cleanup: Boolean = false, block: SDLEventContext.() -> R): R {
    return useContextWithReturn<SDLEventContext, R>(cleanup, block)
}