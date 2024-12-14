// getPhysicsContext.kt
package com.kengine.physics

import com.kengine.hooks.context.ContextRegistry

fun getPhysicsContext(): PhysicsContext {
    return ContextRegistry.get<PhysicsContext>()
}