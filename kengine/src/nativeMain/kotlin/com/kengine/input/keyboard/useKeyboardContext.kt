package com.kengine.input.keyboard

import com.kengine.context.useContextWithReturn

inline fun <R> useKeyboardContext(cleanup: Boolean = false, block: KeyboardContext.() -> R): R {
    return useContextWithReturn<KeyboardContext, R>(cleanup, block)
}