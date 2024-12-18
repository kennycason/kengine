package com.kengine.sdl

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useSDLContext(cleanup: Boolean = false, block: SDL3Context.() -> R): R {
    return useContextWithReturn<SDL3Context, R>(cleanup, block)
}
