package com.kengine.physics

import chipmunk.cpBodyApplyForceAtWorldPoint
import chipmunk.cpBodyApplyImpulseAtWorldPoint
import chipmunk.cpBodyGetAngle
import chipmunk.cpBodyGetAngularVelocity
import chipmunk.cpBodyGetPosition
import chipmunk.cpBodyGetVelocity
import chipmunk.cpBodyNew
import chipmunk.cpBodyNewKinematic
import chipmunk.cpBodyNewStatic
import chipmunk.cpBodySetAngle
import chipmunk.cpBodySetAngularVelocity
import chipmunk.cpBodySetPosition
import chipmunk.cpBodySetVelocity
import chipmunk.cpv
import com.kengine.math.Vec2
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class Body private constructor(internal val handle: CPointer<cnames.structs.cpBody>) {
    init {
        usePhysicsContext {
            addBody(handle)
        }
    }
    var position: Vec2
        get() = cpBodyGetPosition(handle).toVec2()
        set(value) = cpBodySetPosition(handle, cpv(value.x, value.y))

    var velocity: Vec2
        get() = cpBodyGetVelocity(handle).toVec2()
        set(value) = cpBodySetVelocity(handle, cpv(value.x, value.y))
    
    var angle: Double
        get() = cpBodyGetAngle(handle)
        set(value) = cpBodySetAngle(handle, value)
    
    var angularVelocity: Double
        get() = cpBodyGetAngularVelocity(handle)
        set(value) = cpBodySetAngularVelocity(handle, value)
    
    fun applyForce(force: Vec2, point: Vec2) {
        cpBodyApplyForceAtWorldPoint(handle, cpv(force.x, force.y), cpv(point.x, point.y))
    }
    
    fun applyImpulse(impulse: Vec2, point: Vec2) {
        cpBodyApplyImpulseAtWorldPoint(handle, cpv(impulse.x, impulse.y), cpv(point.x, point.y))
    }
    
    companion object {
        fun createDynamic(mass: Double, moment: Double): Body {
            return Body(cpBodyNew(mass, moment)!!)
        }
        
        fun createKinematic(): Body {
            return Body(cpBodyNewKinematic()!!)
        }
        
        fun createStatic(): Body {
            return Body(cpBodyNewStatic()!!)
        }
    }
}