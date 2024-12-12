package com.kengine.physics

import chipmunk.cpSpaceAddBody
import chipmunk.cpSpaceAddShape
import chipmunk.cpSpaceFree
import chipmunk.cpSpaceNew
import chipmunk.cpSpaceSetDamping
import chipmunk.cpSpaceSetGravity
import chipmunk.cpSpaceSetIterations
import chipmunk.cpSpaceStep
import chipmunk.cpv
import com.kengine.context.Context
import com.kengine.log.Logging
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class PhysicsContext private constructor() : Context(), Logging {
    private val space = cpSpaceNew()

    init {
        cpSpaceSetGravity(space, cpv(0.0, 100.0)) // Positive Y is down in SDL
        cpSpaceSetDamping(space, 0.8)  // Add some air resistance
        cpSpaceSetIterations(space, 10)
    }

    fun addBody(body: CPointer<cnames.structs.cpBody>) {
        cpSpaceAddBody(space, body)
    }

    fun addShape(shape: CPointer<cnames.structs.cpShape>) {
        cpSpaceAddShape(space, shape)
    }
    
    fun step(deltaTime: Double) {
        cpSpaceStep(space, deltaTime)
    }
    
    internal fun getSpace() = space
    
    override fun cleanup() {
        cpSpaceFree(space)
        currentContext = null
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