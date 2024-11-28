package com.kengine.sound

import com.kengine.context.useContextWithReturn

inline fun <R> useSoundContext(cleanup: Boolean = false, block: SoundContext.() -> R): R {
    return useContextWithReturn<SoundContext, R>(cleanup, block)
}