package com.kengine

import com.kengine.hooks.context.useContext
import com.kengine.log.Logger
import com.kengine.sdl.RenderBackend

inline fun <R> createGameContext(
    title: String,
    width: Int,
    height: Int,
    logLevel: Logger.Level = Logger.Level.INFO,
    renderBackend: RenderBackend = RenderBackend.SDL_RENDERER_2D,
    block: GameContext.() -> R): R {
    return useContext(
        GameContext.create(
            title = title,
            width = width,
            height = height,
            logLevel = logLevel,
            renderBackend = renderBackend
        ),
        cleanup = true
    ) {
        block()
    }
}
