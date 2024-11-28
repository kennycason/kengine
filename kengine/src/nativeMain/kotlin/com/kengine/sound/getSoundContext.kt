package com.kengine.sound

import com.kengine.context.ContextRegistry

inline fun getSoundContext(): SoundContext {
    return ContextRegistry.get<SoundContext>()
}
