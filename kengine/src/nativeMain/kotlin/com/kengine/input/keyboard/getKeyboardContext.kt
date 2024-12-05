package com.kengine.input.keyboard

import com.kengine.context.ContextRegistry

fun getKeyboardContext(): KeyboardContext {
    return ContextRegistry.get<KeyboardContext>()
}
