package com.kengine.physics

import com.kengine.math.Rect
import com.kengine.math.Vec2

class PhysicsWorld {
    private val bodies = mutableListOf<Body>()
    private val shapes = mutableListOf<Shape>()
    
    fun createBody(type: BodyType = BodyType.DYNAMIC, mass: Double = 1.0, moment: Double = 1.0): Body {
        return when(type) {
            BodyType.DYNAMIC -> Body.createDynamic(mass, moment)
            BodyType.KINEMATIC -> Body.createKinematic()
            BodyType.STATIC -> Body.createStatic()
        }.also { bodies.add(it) }
    }
    
    fun createCircle(body: Body, radius: Double, offset: Vec2 = Vec2()): Shape.Circle {
        return Shape.Circle(body, radius, offset).also { shapes.add(it) }
    }
    
    fun createBox(body: Body, rect: Rect, radius: Double = 0.0): Shape.Box {
        return Shape.Box(body, rect, radius).also { shapes.add(it) }
    }
    
    fun createSegment(body: Body, a: Vec2, b: Vec2, radius: Double): Shape.Segment {
        return Shape.Segment(body, a, b, radius).also { shapes.add(it) }
    }
}