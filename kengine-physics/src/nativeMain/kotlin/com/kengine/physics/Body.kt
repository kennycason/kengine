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
import com.kengine.log.Logging
import com.kengine.math.Vec2
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class Body private constructor(
    internal val handle: CPointer<cnames.structs.cpBody>,
    val type: BodyType
) : Logging {

    var position: Vec2
        get() = if (!isDestroyed) cpBodyGetPosition(handle).toVec2() else Vec2()
        set(value) {
            if (!isDestroyed) cpBodySetPosition(handle, cpv(value.x, value.y))
        }

    var velocity: Vec2
        get() = if (!isDestroyed) cpBodyGetVelocity(handle).toVec2() else Vec2()
        set(value) {
            if (!isDestroyed) cpBodySetVelocity(handle, cpv(value.x, value.y))
        }

    var angle: Double
        get() = if (!isDestroyed) cpBodyGetAngle(handle) else 0.0
        set(value) {
            if (!isDestroyed) cpBodySetAngle(handle, value)
        }

    var angularVelocity: Double
        get() = if (!isDestroyed) cpBodyGetAngularVelocity(handle) else 0.0
        set(value) {
            if (!isDestroyed) cpBodySetAngularVelocity(handle, value)
        }

    fun applyForce(force: Vec2, point: Vec2) {
        if (!isDestroyed) {
            cpBodyApplyForceAtWorldPoint(handle, cpv(force.x, force.y), cpv(point.x, point.y))
        }
    }

    fun applyImpulse(impulse: Vec2, point: Vec2) {
        if (!isDestroyed) {
            cpBodyApplyImpulseAtWorldPoint(handle, cpv(impulse.x, impulse.y), cpv(point.x, point.y))
        }
    }

    val isStatic: Boolean get() = type == BodyType.STATIC
    val isDynamic: Boolean get() = type == BodyType.DYNAMIC
    val isKinematic: Boolean get() = type == BodyType.KINEMATIC

    var isDestroyed = false
        internal set

    companion object {
        fun createDynamic(mass: Double, moment: Double): Body {
            return Body(cpBodyNew(mass, moment)!!, BodyType.DYNAMIC)
        }

        fun createKinematic(): Body {
            return Body(cpBodyNewKinematic()!!, BodyType.KINEMATIC)
        }

        fun createStatic(): Body {
            return Body(cpBodyNewStatic()!!, BodyType.STATIC)
        }
    }
}
