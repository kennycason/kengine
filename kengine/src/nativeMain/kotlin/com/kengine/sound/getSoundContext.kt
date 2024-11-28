package com.kengine.sound

import com.kengine.context.ContextRegistry

fun getSoundContext(): SoundContext {
    return ContextRegistry.get<SoundContext>()
}
