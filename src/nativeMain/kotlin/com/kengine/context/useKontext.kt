package com.kengine.context

/**
 * functional helper to get context
 */
inline fun <T : Kontext, R> useKontext(context: T, block: (T) -> R): R {
    try {
        return block(context)
    } finally {
        context.cleanup()
    }
}