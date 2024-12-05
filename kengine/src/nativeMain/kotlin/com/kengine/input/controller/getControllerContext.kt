package com.kengine.input.controller

import com.kengine.context.ContextRegistry

fun getControllerContext(): ControllerContext {
    return ContextRegistry.get<ControllerContext>()
}
