package com.kengine.physics

import com.kengine.math.Math
import com.kengine.math.Rect
import com.kengine.math.Vec2
import com.kengine.test.expectThat
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ShapeTest {

    private val ctx = PhysicsContext.get()

    @AfterTest
    fun cleanup() {
        ctx.cleanup()
    }

    @Test
    fun `circle shape geometry`() {
        val body = Body.createDynamic(1.0, 1.0)
        val circle = Shape.Circle(body, radius = 5.0)
        assertTrue(abs(circle.radius - 5.0) < 0.001)
        assertTrue(abs(circle.area - Math.PI * 25.0) < 0.001)
        assertTrue(abs(circle.circumference - 2 * Math.PI * 5.0) < 0.001)
    }

    @Test
    fun `box shape geometry`() {
        val body = Body.createDynamic(1.0, 1.0)
        val box = Shape.Box(body, Rect(w = 10.0, h = 20.0))
        assertTrue(abs(box.width - 10.0) < 0.001)
        assertTrue(abs(box.height - 20.0) < 0.001)
        assertTrue(abs(box.area - 200.0) < 0.001)
    }

    @Test
    fun `segment shape geometry`() {
        val body = Body.createDynamic(1.0, 1.0)
        val segment = Shape.Segment(body, Vec2(0.0, 0.0), Vec2(3.0, 4.0), thickness = 1.0)
        assertTrue(abs(segment.length - 5.0) < 0.001)
        assertTrue(abs(segment.thickness - 1.0) < 0.001)
    }

    @Test
    fun `set and get friction`() {
        val body = Body.createDynamic(1.0, 1.0)
        val circle = Shape.Circle(body, 5.0)
        circle.friction = 0.7
        assertTrue(abs(circle.friction - 0.7) < 0.001)
    }

    @Test
    fun `set and get elasticity`() {
        val body = Body.createDynamic(1.0, 1.0)
        val circle = Shape.Circle(body, 5.0)
        circle.elasticity = 0.9
        assertTrue(abs(circle.elasticity - 0.9) < 0.001)
    }

    @Test
    fun `set and get sensor`() {
        val body = Body.createDynamic(1.0, 1.0)
        val circle = Shape.Circle(body, 5.0)
        expectThat(circle.sensor).isFalse()
        circle.sensor = true
        expectThat(circle.sensor).isTrue()
    }

    @Test
    fun `destroyed shape returns defaults`() {
        val body = Body.createDynamic(1.0, 1.0)
        val shape = Shape.Circle(body, 5.0)
        val obj = PhysicsObject(body, shape)
        ctx.addObject(obj)
        ctx.removeObject(obj)

        expectThat(shape.isDestroyed).isTrue()
        expectThat(shape.friction).isEqualTo(0.0)
        expectThat(shape.elasticity).isEqualTo(0.0)
        expectThat(shape.sensor).isFalse()
    }
}
