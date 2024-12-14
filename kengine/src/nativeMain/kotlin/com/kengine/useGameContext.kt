package com.kengine

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useGameContext(cleanup: Boolean = false, block: GameContext.() -> R): R {
    return useContextWithReturn<GameContext, R>(cleanup, block)
}