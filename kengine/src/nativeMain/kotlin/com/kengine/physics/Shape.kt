package com.kengine.physics

import chipmunk.cpBoxShapeNew
import chipmunk.cpCircleShapeNew
import chipmunk.cpSegmentShapeGetNormal
import chipmunk.cpSegmentShapeNew
import chipmunk.cpShapeGetDensity
import chipmunk.cpShapeGetElasticity
import chipmunk.cpShapeGetFriction
import chipmunk.cpShapeGetSensor
import chipmunk.cpShapeSetDensity
import chipmunk.cpShapeSetElasticity
import chipmunk.cpShapeSetFriction
import chipmunk.cpShapeSetSensor
import chipmunk.cpv
import com.kengine.math.Math
import com.kengine.math.Rect
import com.kengine.math.Vec2
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
sealed class Shape(internal val handle: CPointer<cnames.structs.cpShape>) {

    var friction: Double
        get() = if (!isDestroyed) cpShapeGetFriction(handle) else 0.0
        set(value) {
            if (!isDestroyed) cpShapeSetFriction(handle, value)
        }

    var elasticity: Double
        get() = if (!isDestroyed) cpShapeGetElasticity(handle) else 0.0
        set(value) {
            if (!isDestroyed) cpShapeSetElasticity(handle, value)
        }

    var density: Double
        get() = if (!isDestroyed) cpShapeGetDensity(handle) else 0.0
        set(value) {
            if (!isDestroyed) cpShapeSetDensity(handle, value)
        }

    var sensor: Boolean
        get() = if (!isDestroyed) cpShapeGetSensor(handle).toBoolean() else false
        set(value) {
            if (!isDestroyed) cpShapeSetSensor(handle, value.toCpBool())
        }

    var isDestroyed = false
        private set

    fun destroy() {
        if (!isDestroyed) {
            isDestroyed = true
            usePhysicsContext {
                removeFromSpace(this@Shape)
            }
        }
    }

    class Circle(
        body: Body,
        val radius: Double,
        offset: Vec2 = Vec2()
    ) : Shape(cpCircleShapeNew(body.handle, radius, cpv(offset.x, offset.y))!!) {
        val area: Double get() = Math.PI * radius * radius
        val circumference: Double get() = 2 * Math.PI * radius
    }

    class Box(
        body: Body,
        val rect: Rect,
        val cornerRadius: Double = 0.0
    ) : Shape(cpBoxShapeNew(body.handle, rect.w, rect.h, cornerRadius)!!) {
        val area: Double get() = rect.area()
        val width: Double get() = rect.w
        val height: Double get() = rect.h

    }

    class Segment(
        body: Body,
        val a: Vec2,
        val b: Vec2,
        val thickness: Double
    ) : Shape(cpSegmentShapeNew(body.handle, cpv(a.x, a.y), cpv(b.x, b.y), thickness)!!) {
        val length: Double get() = (b - a).magnitude()
        val normal: Vec2 get() = cpSegmentShapeGetNormal(handle).toVec2()
    }
}