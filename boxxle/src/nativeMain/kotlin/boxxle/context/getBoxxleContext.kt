package boxxle.context

import com.kengine.context.ContextRegistry

inline fun getBoxxleContext(): BoxxleContext {
    return ContextRegistry.get<BoxxleContext>()
}
