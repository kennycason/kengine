package boxxle.context

import com.kengine.hooks.context.useContextWithReturn

inline fun <R> useBoxxleContext(cleanup: Boolean = false, block: BoxxleContext.() -> R): R {
    return useContextWithReturn<BoxxleContext, R>(cleanup, block)
}