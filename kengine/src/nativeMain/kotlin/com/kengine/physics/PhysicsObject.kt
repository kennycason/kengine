package com.kengine.physics

data class PhysicsObject(
    val body: Body,
    val shape: Shape
) {
    fun destroy() {
        usePhysicsContext {
            withClearing {
                shape.destroy()
                body.destroy()
            }
        }
    }
}