package com.kengine.context

/**
 * functional helper to use context
 *
 * useContext(SpriteContext.get()) {
 *     addSpriteSheet(...)
 * }
 */
inline fun <reified T : Context, R> useContext(context: T, cleanup: Boolean = false, block: T.() -> R): R {
    try {
        if (!ContextRegistry.isRegistered(context)) {
            ContextRegistry.register(context)
        }
        return context.block()
    } finally {
        if (cleanup) {
            context.cleanup()
        }
    }
}

/**
 * functional helper to use context.
 *
 * Important! Contexts must be registered in the ContextRegistry in order to use this function.
 *
 * I am unable to get the return type R configured such that calling functions can infer type R.
 *
 * useContext <SpriteContext> {
 *     addSpriteSheet(...)
 * }
 */
inline fun <reified T : Context> useContext(cleanup: Boolean = false, block: T.() -> Unit) {
    val context = ContextRegistry.get<T>()
    return try {
        context.block()
    } finally {
        if (cleanup) {
            context.cleanup()
        }
    }
}

/**
 * This function is designed to have a return type, and is used by other useContext helpers.
 */
inline fun <reified T : Context, R> useContextWithReturn(cleanup: Boolean = false, block: T.() -> R): R {
    val context = ContextRegistry.get<T>()
    return try {
        context.block()
    } finally {
        if (cleanup) {
            context.cleanup()
        }
    }
}

