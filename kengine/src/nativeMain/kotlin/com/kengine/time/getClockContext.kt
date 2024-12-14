package com.kengine.time

import com.kengine.hooks.context.ContextRegistry

fun getClockContext(): ClockContext {
    return ContextRegistry.get<ClockContext>()
}
