package com.kengine

import com.kengine.context.ContextRegistry

fun getGameContext(): GameContext {
    return ContextRegistry.get<GameContext>()
}
