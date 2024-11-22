package com.kengine.context

/**
 * functional helper to get context
 */
inline fun <T : Context, R> useContext(context: T, cleanup: Boolean = false, block: T.() -> R): R {
    try {
        return context.block()
    } finally {
        if (cleanup) {
            context.cleanup()
        }
    }
}