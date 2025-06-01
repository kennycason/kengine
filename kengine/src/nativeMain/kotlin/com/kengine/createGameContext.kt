package com.kengine

import com.kengine.hooks.context.useContext
import com.kengine.log.Logger

inline fun <R> createGameContext(
    title: String,
    width: Int,
    height: Int,
    logLevel: Logger.Level = Logger.Level.INFO,
    block: GameContext.() -> R): R {
    return useContext(
        GameContext.create(
            title = title,
            width = width,
            height = height,
            logLevel = logLevel
        ),
        cleanup = true
    ) {
        block()
    }
}
