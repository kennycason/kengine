package com.kengine.event

import com.kengine.context.ContextRegistry

fun getEventContext(): EventContext {
    return ContextRegistry.get<EventContext>()
}
