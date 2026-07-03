package com.kengine.three

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useGpuContext(cleanup: Boolean = false, block: GpuContext.() -> R): R {
    return useContextWithReturn<GpuContext, R>(cleanup, block)
}
