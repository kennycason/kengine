package com.kengine.action

import com.kengine.context.ContextRegistry

fun getActionContext(): ActionContext {
    return ContextRegistry.get<ActionContext>()
}
