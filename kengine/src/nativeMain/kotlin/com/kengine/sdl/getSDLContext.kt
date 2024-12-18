package com.kengine.sdl

import com.kengine.hooks.context.ContextRegistry

fun getSDLContext(): SDL3Context {
    return ContextRegistry.get<SDL3Context>()
}
