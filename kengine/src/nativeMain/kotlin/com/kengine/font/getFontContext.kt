package com.kengine.font

import com.kengine.context.ContextRegistry

fun getFontContext(): FontContext {
    return ContextRegistry.get<FontContext>()
}
