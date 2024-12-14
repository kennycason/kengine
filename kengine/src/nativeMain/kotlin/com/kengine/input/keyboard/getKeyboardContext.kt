package com.kengine.input.keyboard

import com.kengine.hooks.context.ContextRegistry

fun getKeyboardContext(): KeyboardContext {
    return ContextRegistry.get<KeyboardContext>()
}
