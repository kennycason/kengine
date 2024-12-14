package com.kengine.event

import com.kengine.hooks.context.ContextRegistry

fun getEventContext(): EventContext {
    return ContextRegistry.get<EventContext>()
}
