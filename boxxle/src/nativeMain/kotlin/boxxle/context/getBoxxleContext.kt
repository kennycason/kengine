package boxxle.context

import boxxle.context.BoxxleContext
import com.kengine.context.ContextRegistry

inline fun getBoxxleContext(): BoxxleContext {
    return ContextRegistry.get<BoxxleContext>()
}
