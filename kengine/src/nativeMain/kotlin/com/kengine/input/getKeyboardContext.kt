package com.kengine.input

import com.kengine.context.ContextRegistry

inline fun getKeyboardContext(): KeyboardContext {
    return ContextRegistry.get<KeyboardContext>()
}
