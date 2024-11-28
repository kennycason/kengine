package com.kengine.sdl

import com.kengine.context.ContextRegistry

inline fun getSDLEventContext(): SDLEventContext {
    return ContextRegistry.get<SDLEventContext>()
}
