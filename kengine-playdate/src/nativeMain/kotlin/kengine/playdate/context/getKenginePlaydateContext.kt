package kengine.playdate.context

import com.kengine.context.ContextRegistry

fun getKenginePlaydateContext(): KenginePlaydateContext {
    return ContextRegistry.get<KenginePlaydateContext>()
}
