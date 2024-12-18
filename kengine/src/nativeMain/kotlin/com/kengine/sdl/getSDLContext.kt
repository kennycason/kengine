package com.kengine.sdl

import com.kengine.hooks.context.ContextRegistry

fun getSDLContext(): SDLContext {
    return ContextRegistry.get<SDLContext>()
}
