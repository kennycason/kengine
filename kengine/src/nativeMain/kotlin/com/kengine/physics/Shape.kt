package com.kengine.physics

import chipmunk.cpBoxShapeNew
import chipmunk.cpCircleShapeNew
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
import com.kengine.math.Rect
import com.kengine.math.Vec2
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
sealed class Shape(internal val handle: CPointer<cnames.structs.cpShape>) {
    init {
        usePhysicsContext {
            addShape(handle)
        }
    }
    var friction: Double
        get() = cpShapeGetFriction(handle)
        set(value) = cpShapeSetFriction(handle, value)
    
    var elasticity: Double
        get() = cpShapeGetElasticity(handle)
        set(value) = cpShapeSetElasticity(handle, value)
    
    var density: Double
        get() = cpShapeGetDensity(handle)
        set(value) = cpShapeSetDensity(handle, value)
    
    var sensor: Boolean
        get() = cpShapeGetSensor(handle).toBoolean()
        set(value) = cpShapeSetSensor(handle, value.toCpBool())
    
    class Circle(
        body: Body,
        val radius: Double,
        offset: Vec2 = Vec2()
    ) : Shape(cpCircleShapeNew(body.handle, radius, cpv(offset.x, offset.y))!!)
    
    class Box(
        body: Body,
        rect: Rect,
        radius: Double = 0.0
    ) : Shape(cpBoxShapeNew(body.handle, rect.w, rect.h, radius)!!)
    
    class Segment(
        body: Body,
        a: Vec2,
        b: Vec2,
        radius: Double
    ) : Shape(cpSegmentShapeNew(body.handle, cpv(a.x, a.y), cpv(b.x, b.y), radius)!!)
}