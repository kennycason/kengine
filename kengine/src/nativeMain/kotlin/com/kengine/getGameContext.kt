package com.kengine

import com.kengine.context.ContextRegistry

inline fun getGameContext(): GameContext {
    return ContextRegistry.get<GameContext>()
}
