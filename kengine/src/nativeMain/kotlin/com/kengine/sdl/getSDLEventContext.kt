package com.kengine.sdl

import com.kengine.hooks.context.ContextRegistry

fun getSDLEventContext(): SDLEventContext {
    return ContextRegistry.get<SDLEventContext>()
}
