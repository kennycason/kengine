package com.kengine.input.controller

import com.kengine.hooks.context.ContextRegistry

fun getControllerContext(): ControllerContext {
    return ContextRegistry.get<ControllerContext>()
}
