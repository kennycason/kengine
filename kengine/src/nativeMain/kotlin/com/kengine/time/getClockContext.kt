package com.kengine.time

import com.kengine.context.ContextRegistry

inline fun getClockContext(): ClockContext {
    return ContextRegistry.get<ClockContext>()
}
