package com.kengine.input

import com.kengine.context.ContextRegistry

fun getKeyboardContext(): KeyboardContext {
    return ContextRegistry.get<KeyboardContext>()
}
