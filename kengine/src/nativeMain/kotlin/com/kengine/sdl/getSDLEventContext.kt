package com.kengine.sdl

import com.kengine.context.ContextRegistry

fun getSDLEventContext(): SDLEventContext {
    return ContextRegistry.get<SDLEventContext>()
}
