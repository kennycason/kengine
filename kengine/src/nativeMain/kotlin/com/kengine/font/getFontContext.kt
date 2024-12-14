package com.kengine.font

import com.kengine.hooks.context.ContextRegistry

fun getFontContext(): FontContext {
    return ContextRegistry.get<FontContext>()
}
