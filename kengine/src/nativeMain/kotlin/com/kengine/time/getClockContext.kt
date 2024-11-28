package com.kengine.time

import com.kengine.context.ContextRegistry

fun getClockContext(): ClockContext {
    return ContextRegistry.get<ClockContext>()
}
