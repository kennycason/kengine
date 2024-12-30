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
    private val dynamicObjects = mutableListOf<PhysicsObject>()
    private val staticObjects = mutableListOf<PhysicsObject>()
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

    fun addObject(obj: PhysicsObject) {
        if (isClearing) throw IllegalStateException("Can not add objects while clearing.")
        if (obj.isDestroyed) throw IllegalStateException("Cannot add destroyed object")

        val list = if (obj.body.isStatic) staticObjects else dynamicObjects
        if (!list.contains(obj)) {
            cpSpaceAddBody(space, obj.body.handle)
            cpSpaceAddShape(space, obj.shape.handle)
            list.add(obj)
        }
    }

    fun removeObject(obj: PhysicsObject) {
        val list = if (obj.body.isStatic) staticObjects else dynamicObjects
        if (list.remove(obj)) {
            if (!isClearing) {
                cpSpaceRemoveBody(space, obj.body.handle)
                cpSpaceRemoveShape(space, obj.shape.handle)
            }
            cpBodyFree(obj.body.handle)
            obj.body.isDestroyed = true

            cpShapeFree(obj.shape.handle)
            obj.shape.isDestroyed = true
        }
    }

    fun getDynamicObjects(): List<PhysicsObject> = dynamicObjects

    fun getStaticObjects(): List<PhysicsObject> = staticObjects

    fun step(deltaTime: Double) {
        if (!isClearing) {
            cpSpaceStep(space, deltaTime)
        }
    }

    fun clearDynamicObjects() {
        withClearing {
            dynamicObjects.clear()
            dynamicObjects.forEach(::removeObject)
        }
    }

    fun clearAll() {
        withClearing {
            dynamicObjects.clear()
            staticObjects.clear()
            dynamicObjects.forEach(::removeObject)
            staticObjects.forEach(::removeObject)
        }
    }

    override fun cleanup() {
        withClearing {
            clearAll()
            cpSpaceFree(space)
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

    companion object {
        private var currentContext: PhysicsContext? = null

        fun get(): PhysicsContext {
            return currentContext ?: PhysicsContext().also {
                currentContext = it
            }
        }
    }
}
