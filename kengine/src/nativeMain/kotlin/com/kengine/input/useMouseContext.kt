package com.kengine.input

import com.kengine.context.useContextWithReturn

inline fun <R> useMouseContext(cleanup: Boolean = false, block: MouseContext.() -> R): R {
    return useContextWithReturn<MouseContext, R>(cleanup, block)
}