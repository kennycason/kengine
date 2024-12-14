package com.kengine.hooks.context

import kotlin.reflect.KClass

/**
 * This is what allows the below syntax:
 *
 * useContext<KeyboardContext> { // or useKeyboardContext {
 *     if (keyboard.isReturnPressed()) { }
 * }
 */
object ContextRegistry {
    private val contexts = mutableMapOf<KClass<out Context>, Context>()

    @PublishedApi
    internal fun getContextForClass(kClass: KClass<out Context>): Context? {
        return contexts[kClass]
    }

    inline fun <reified T : Context> get(): T {
        return (getContextForClass(T::class) as? T)
            ?: throw IllegalStateException("${T::class.simpleName} not registered.")
    }

    fun register(context: Context) {
        contexts[context::class] = context
    }

    fun isRegistered(context: Context): Boolean {
        return getContextForClass(context::class) != null
    }

    fun clearAll() {
        contexts.clear()
    }
}