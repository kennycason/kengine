package com.kengine.sdl

import com.kengine.context.ContextRegistry

inline fun getSDLContext(): SDLContext {
    return ContextRegistry.get<SDLContext>()
}
