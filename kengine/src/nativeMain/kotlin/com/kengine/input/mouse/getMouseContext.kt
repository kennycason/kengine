package com.kengine.input.mouse

import com.kengine.hooks.context.ContextRegistry

fun getMouseContext(): MouseContext {
    return ContextRegistry.get<MouseContext>()
}
