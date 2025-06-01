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
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class PhysicsContext private constructor() : Context(), Logging {
    private var space = cpSpaceNew()
    private val dynamicObjects = mutableListOf<PhysicsObject>()
    private val staticObjects = mutableListOf<PhysicsObject>()
    private var isClearing = false
    private var isSpaceDestroyed = false

    var gravity: Vec2 = Vec2(0.0, 500.0)
        set(value) {
            field = value
            if (!isSpaceDestroyed && space != null) {
                cpSpaceSetGravity(space, cpv(value.x, value.y))
            }
        }

    var damping: Double = 0.8
        set(value) {
            field = value
            if (!isSpaceDestroyed && space != null) {
                cpSpaceSetDamping(space, value)
            }
        }

    var iterations: Int = 10
        set(value) {
            field = value
            if (!isSpaceDestroyed && space != null) {
                cpSpaceSetIterations(space, value)
            }
        }

    init {
        gravity = gravity
        damping = damping
        iterations = iterations
    }

    fun addObject(obj: PhysicsObject) {
        if (isClearing) throw IllegalStateException("Can not add objects while clearing.")
        if (obj.isDestroyed) throw IllegalStateException("Cannot add destroyed object")
        if (isSpaceDestroyed || space == null) throw IllegalStateException("Physics space has been destroyed")

        val list = if (obj.body.isStatic) staticObjects else dynamicObjects
        if (!list.contains(obj)) {
            cpSpaceAddBody(space, obj.body.handle)
            cpSpaceAddShape(space, obj.shape.handle)
            list.add(obj)
        }
    }

    fun removeObject(obj: PhysicsObject) {
        if (logger.isDebugEnabled()) {
            logger.debug { "Removing object: $obj" }
            logger.debug { "Body destroyed: ${obj.body.isDestroyed}, Shape destroyed: ${obj.shape.isDestroyed}" }
        }

        if (!isSpaceDestroyed && space != null) {
            cpSpaceRemoveBody(space, obj.body.handle)
            cpSpaceRemoveShape(space, obj.shape.handle)
        }

        // free memory safely
        if (!obj.body.isDestroyed) {
            cpBodyFree(obj.body.handle)
            obj.body.isDestroyed = true
        }

        if (!obj.shape.isDestroyed) {
            cpShapeFree(obj.shape.handle)
            obj.shape.isDestroyed = true
        }

        // finally, remove from lists if still present
        dynamicObjects.remove(obj)
        staticObjects.remove(obj)
    }

    fun getDynamicObjects(): List<PhysicsObject> = dynamicObjects

    fun getStaticObjects(): List<PhysicsObject> = staticObjects

    fun step(deltaTime: Double) {
        if (!isClearing && !isSpaceDestroyed && space != null) {
            cpSpaceStep(space, deltaTime)
        }
    }

    fun clearDynamicObjects() {
        withClearing {
            val objectsToRemove = dynamicObjects.toList()   // take a snapshot
            objectsToRemove.forEach(::removeObject) // remove from physics space first
            dynamicObjects.clear()                          // THEN clear the list references...
        }
    }

    fun clearAll() {
        withClearing {
            val dynamicToRemove = dynamicObjects.toList()
            val staticToRemove = staticObjects.toList()

            dynamicObjects.clear()
            staticObjects.clear()

            dynamicToRemove.forEach(::removeObject)
            staticToRemove.forEach(::removeObject)
        }
    }

    override fun cleanup() {
        withClearing {
            clearAll()
            if (!isSpaceDestroyed && space != null) {
                logger.info { "Freeing physics space" }
                cpSpaceFree(space)
                isSpaceDestroyed = true
                space = null
            } else {
                logger.info { "Physics space already destroyed or null" }
            }

            // Reset the singleton instance to ensure a fresh context for the next test
            PhysicsContext.reset()
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

        fun reset() {
            currentContext = null
        }
    }
}
