package boxxle

import com.kengine.context.ContextRegistry

inline fun getBoxxleContext(): BoxxleContext {
    return ContextRegistry.get<BoxxleContext>()
}
