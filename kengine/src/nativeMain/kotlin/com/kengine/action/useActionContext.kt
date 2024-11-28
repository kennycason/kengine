package com.kengine.action

import com.kengine.context.useContextWithReturn

inline fun <R> useActionContext(cleanup: Boolean = false, block: ActionContext.() -> R): R {
    return useContextWithReturn<ActionContext, R>(cleanup, block)
}