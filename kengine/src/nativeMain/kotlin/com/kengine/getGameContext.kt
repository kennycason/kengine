package com.kengine

import com.kengine.hooks.context.ContextRegistry

fun getGameContext(): GameContext {
    return ContextRegistry.get<GameContext>()
}
