package boxxle

import com.kengine.context.useContextWithReturn

inline fun <R> useBoxxleContext(cleanup: Boolean = false, block: BoxxleContext.() -> R): R {
    return useContextWithReturn<BoxxleContext, R>(cleanup, block)
}