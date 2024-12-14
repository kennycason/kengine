package com.kengine.action

import com.kengine.hooks.context.ContextRegistry

fun getActionContext(): ActionContext {
    return ContextRegistry.get<ActionContext>()
}
