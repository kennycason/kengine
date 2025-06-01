package com.kengine.hooks.context

/**
 * functional helper to get context. Ideal for quick one-line includes
 *
 * getContext<SpriteContext>().addSpriteSheet(...)
 */
inline fun <reified T : Context> getContext(): T {
    return ContextRegistry.get<T>()
}
