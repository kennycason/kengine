package boxxle.context

import com.kengine.context.ContextRegistry

fun getBoxxleContext(): BoxxleContext {
    return ContextRegistry.get<BoxxleContext>()
}
