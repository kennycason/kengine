package com.kengine.log

import com.kengine.hooks.context.ContextRegistry

fun getBoxxleContext(): LoggerContext {
    return ContextRegistry.get<LoggerContext>()
}
