package com.kengine.input.mouse

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useMouseContext(cleanup: Boolean = false, block: MouseContext.() -> R): R {
    return useContextWithReturn<MouseContext, R>(cleanup, block)
}