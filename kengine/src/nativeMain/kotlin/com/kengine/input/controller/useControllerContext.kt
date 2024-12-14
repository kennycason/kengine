package com.kengine.input.controller

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useControllerContext(cleanup: Boolean = false, block: ControllerContext.() -> R): R {
    return useContextWithReturn<ControllerContext, R>(cleanup, block)
}