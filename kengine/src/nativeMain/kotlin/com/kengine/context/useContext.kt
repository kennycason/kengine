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
 * useContext <SpriteContext> {
 *     addSpriteSheet(...)
 * }
 * TODO resolve typing issues ca
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
