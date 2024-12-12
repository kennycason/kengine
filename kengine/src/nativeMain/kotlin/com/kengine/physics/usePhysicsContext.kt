// usePhysicsContext.kt
package com.kengine.physics

import com.kengine.context.useContextWithReturn

inline fun <R> usePhysicsContext(cleanup: Boolean = false, block: PhysicsContext.() -> R): R {
    return useContextWithReturn<PhysicsContext, R>(cleanup, block)
}