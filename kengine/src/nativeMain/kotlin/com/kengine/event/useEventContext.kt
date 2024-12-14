package com.kengine.event

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useEventContext(cleanup: Boolean = false, block: EventContext.() -> R): R {
    return useContextWithReturn<EventContext, R>(cleanup, block)
}