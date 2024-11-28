package com.kengine.sdl

import com.kengine.context.ContextRegistry

fun getSDLContext(): SDLContext {
    return ContextRegistry.get<SDLContext>()
}
