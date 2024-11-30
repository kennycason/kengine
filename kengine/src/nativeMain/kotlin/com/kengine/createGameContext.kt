package com.kengine

import com.kengine.context.useContext

inline fun <R> createGameContext(
    title: String,
    width: Int,
    height: Int,
    block: GameContext.() -> R): R {
    return useContext(
        GameContext.create(
            title = title,
            width = width,
            height = height
        )
    ) {
        block()
    }
}