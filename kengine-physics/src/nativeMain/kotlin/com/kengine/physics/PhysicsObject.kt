package com.kengine.physics

data class PhysicsObject(
    val body: Body,
    val shape: Shape
) {
    val isDestroyed: Boolean
        get() = body.isDestroyed || shape.isDestroyed
}
