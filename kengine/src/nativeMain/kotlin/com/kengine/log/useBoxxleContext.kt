package com.kengine.log

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useBoxxleContext(cleanup: Boolean = false, block: LoggerContext.() -> R): R {
    return useContextWithReturn<LoggerContext, R>(cleanup, block)
}