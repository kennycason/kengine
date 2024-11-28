package com.kengine.event

import com.kengine.context.ContextRegistry

inline fun getEventContext(): EventContext {
    return ContextRegistry.get<EventContext>()
}
