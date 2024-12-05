package com.kengine.input.mouse

import com.kengine.context.ContextRegistry

fun getMouseContext(): MouseContext {
    return ContextRegistry.get<MouseContext>()
}
