package com.kengine.font

import com.kengine.context.useContextWithReturn

inline fun <R> useFontContext(cleanup: Boolean = false, block: FontContext.() -> R): R {
    return useContextWithReturn<FontContext, R>(cleanup, block)
}