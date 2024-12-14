package com.kengine.physics

import chipmunk.cpBodyFree
import chipmunk.cpShapeFree
import chipmunk.cpSpaceAddBody
import chipmunk.cpSpaceAddShape
import chipmunk.cpSpaceFree
import chipmunk.cpSpaceNew
import chipmunk.cpSpaceRemoveBody
import chipmunk.cpSpaceRemoveShape
import chipmunk.cpSpaceSetDamping
import chipmunk.cpSpaceSetGravity
import chipmunk.cpSpaceSetIterations
import chipmunk.cpSpaceStep
import chipmunk.cpv
import com.kengine.hooks.context.Context
import com.kengine.log.Logging
import com.kengine.math.Vec2
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class PhysicsContext private constructor() : Context(), Logging {
    private val space = cpSpaceNew()
    private val bodies = mutableListOf<Body>()
    private val shapes = mutableListOf<Shape>()
    private var isClearing = false

    var gravity: Vec2 = Vec2(0.0, 500.0)
        set(value) {
            field = value
            cpSpaceSetGravity(space, cpv(value.x, value.y))
        }

    var damping: Double = 0.8
        set(value) {
            field = value
            cpSpaceSetDamping(space, value)
        }

    var iterations: Int = 10
        set(value) {
            field = value
            cpSpaceSetIterations(space, value)
        }

    init {
        gravity = gravity
        damping = damping
        iterations = iterations
    }

    fun addObject(body: Body, shape: Shape) {
        if (isClearing) throw IllegalStateException("Can not add objects while clearing.")
        cpSpaceAddBody(space, body.handle)
        cpSpaceAddShape(space, shape.handle)
        bodies.add(body)
        shapes.add(shape)
    }

    internal fun removeFromSpace(body: Body) {
        if (bodies.remove(body)) {
            cpSpaceRemoveBody(space, body.handle)
            cpBodyFree(body.handle)
        }
    }

    internal fun removeFromSpace(shape: Shape) {
        if (shapes.remove(shape)) {
            cpSpaceRemoveShape(space, shape.handle)
            cpShapeFree(shape.handle)
        }
    }

    fun step(deltaTime: Double) {
        if (!isClearing) {
            cpSpaceStep(space, deltaTime)
        }
    }

    internal fun withClearing(block: () -> Unit) {
        isClearing = true
        try {
            block()
        } finally {
            isClearing = false
        }
    }

    override fun cleanup() {
        withClearing {
            shapes.forEach(Shape::destroy)
            bodies.forEach(Body::destroy)
            cpSpaceFree(space)
            shapes.clear()
            bodies.clear()
        }
    }

    companion object {
        private var currentContext: PhysicsContext? = null

        fun get(): PhysicsContext {
            if (currentContext == null) {
                currentContext = PhysicsContext()
            }
            return currentContext ?: throw IllegalStateException("Failed to create PhysicsContext")
        }
    }
}