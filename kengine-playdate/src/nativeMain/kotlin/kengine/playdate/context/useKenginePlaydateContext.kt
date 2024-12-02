package kengine.playdate.context

import com.kengine.context.useContextWithReturn

inline fun <R> useKenginePlaydateContext(cleanup: Boolean = false, block: KenginePlaydateContext.() -> R): R {
    return useContextWithReturn<KenginePlaydateContext, R>(cleanup, block)
}