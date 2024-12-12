// getPhysicsContext.kt
package com.kengine.physics

import com.kengine.context.ContextRegistry

fun getPhysicsContext(): PhysicsContext {
    return ContextRegistry.get<PhysicsContext>()
}