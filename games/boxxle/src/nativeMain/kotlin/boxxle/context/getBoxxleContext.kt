package boxxle.context

import com.kengine.hooks.context.ContextRegistry

fun getBoxxleContext(): BoxxleContext {
    return ContextRegistry.get<BoxxleContext>()
}
