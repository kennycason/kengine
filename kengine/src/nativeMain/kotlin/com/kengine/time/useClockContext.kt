package com.kengine.time

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useClockContext(cleanup: Boolean = false, block: ClockContext.() -> R): R {
    return useContextWithReturn<ClockContext, R>(cleanup, block)
}