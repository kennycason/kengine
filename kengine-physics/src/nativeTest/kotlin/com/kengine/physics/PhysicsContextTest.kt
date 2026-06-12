package com.kengine.physics

import com.kengine.math.Rect
import com.kengine.math.Vec2
import com.kengine.test.expectThat
import com.kengine.test.expectThrows
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PhysicsContextTest {

    @AfterTest
    fun cleanup() {
        PhysicsContext.get().cleanup()
    }

    @Test
    fun `add and retrieve dynamic object`() {
        val ctx = PhysicsContext.get()
        val body = Body.createDynamic(1.0, 1.0)
        val shape = Shape.Circle(body, 10.0)
        val obj = PhysicsObject(body, shape)

        ctx.addObject(obj)

        expectThat(ctx.getDynamicObjects().size).isEqualTo(1)
        expectThat(ctx.getStaticObjects().size).isEqualTo(0)
    }

    @Test
    fun `add and retrieve static object`() {
        val ctx = PhysicsContext.get()
        val body = Body.createStatic()
        val shape = Shape.Segment(body, Vec2(0.0, 0.0), Vec2(100.0, 0.0), 1.0)
        val obj = PhysicsObject(body, shape)

        ctx.addObject(obj)

        expectThat(ctx.getDynamicObjects().size).isEqualTo(0)
        expectThat(ctx.getStaticObjects().size).isEqualTo(1)
    }

    @Test
    fun `remove object marks it as destroyed`() {
        val ctx = PhysicsContext.get()
        val body = Body.createDynamic(1.0, 1.0)
        val shape = Shape.Circle(body, 5.0)
        val obj = PhysicsObject(body, shape)

        ctx.addObject(obj)
        expectThat(obj.isDestroyed).isFalse()

        ctx.removeObject(obj)
        expectThat(obj.isDestroyed).isTrue()
        expectThat(body.isDestroyed).isTrue()
        expectThat(shape.isDestroyed).isTrue()
        expectThat(ctx.getDynamicObjects().size).isEqualTo(0)
    }

    @Test
    fun `set gravity`() {
        val ctx = PhysicsContext.get()
        ctx.gravity = Vec2(0.0, -9.8)
        assertTrue(abs(ctx.gravity.y - (-9.8)) < 0.001)
    }

    @Test
    fun `set damping`() {
        val ctx = PhysicsContext.get()
        ctx.damping = 0.95
        assertTrue(abs(ctx.damping - 0.95) < 0.001)
    }

    @Test
    fun `set iterations`() {
        val ctx = PhysicsContext.get()
        ctx.iterations = 20
        expectThat(ctx.iterations).isEqualTo(20)
    }

    @Test
    fun `step advances simulation`() {
        val ctx = PhysicsContext.get()
        ctx.gravity = Vec2(0.0, 100.0)

        val body = Body.createDynamic(1.0, 1.0)
        body.position = Vec2(0.0, 0.0)
        val shape = Shape.Circle(body, 5.0)
        val obj = PhysicsObject(body, shape)
        ctx.addObject(obj)

        repeat(5) { ctx.step(1.0 / 60.0) }

        val pos = body.position
        assertTrue(pos.y > 0.0, "Body should have moved downward due to gravity, y=${pos.y}")
    }

    @Test
    fun `clearDynamicObjects removes only dynamic objects`() {
        val ctx = PhysicsContext.get()

        val dynamicBody = Body.createDynamic(1.0, 1.0)
        val dynamicShape = Shape.Circle(dynamicBody, 5.0)
        ctx.addObject(PhysicsObject(dynamicBody, dynamicShape))

        val staticBody = Body.createStatic()
        val staticShape = Shape.Segment(staticBody, Vec2(0.0, 0.0), Vec2(100.0, 0.0), 1.0)
        ctx.addObject(PhysicsObject(staticBody, staticShape))

        expectThat(ctx.getDynamicObjects().size).isEqualTo(1)
        expectThat(ctx.getStaticObjects().size).isEqualTo(1)

        ctx.clearDynamicObjects()

        expectThat(ctx.getDynamicObjects().size).isEqualTo(0)
        expectThat(ctx.getStaticObjects().size).isEqualTo(1)
        expectThat(dynamicBody.isDestroyed).isTrue()
    }

    @Test
    fun `clearAll removes all objects`() {
        val ctx = PhysicsContext.get()

        val body1 = Body.createDynamic(1.0, 1.0)
        ctx.addObject(PhysicsObject(body1, Shape.Circle(body1, 5.0)))

        val body2 = Body.createStatic()
        ctx.addObject(PhysicsObject(body2, Shape.Segment(body2, Vec2(), Vec2(10.0, 0.0), 1.0)))

        ctx.clearAll()

        expectThat(ctx.getDynamicObjects().size).isEqualTo(0)
        expectThat(ctx.getStaticObjects().size).isEqualTo(0)
    }

    @Test
    fun `cannot add destroyed object`() {
        val ctx = PhysicsContext.get()
        val body = Body.createDynamic(1.0, 1.0)
        val shape = Shape.Circle(body, 5.0)
        val obj = PhysicsObject(body, shape)

        ctx.addObject(obj)
        ctx.removeObject(obj)

        expectThrows<IllegalStateException> {
            ctx.addObject(obj)
        }
    }

    @Test
    fun `PhysicsWorld creates bodies and shapes`() {
        val world = PhysicsWorld()
        val body = world.createBody(BodyType.DYNAMIC, mass = 2.0, moment = 3.0)
        expectThat(body.isDynamic).isTrue()

        val circle = world.createCircle(body, radius = 8.0)
        assertTrue(abs(circle.radius - 8.0) < 0.001)

        val body2 = world.createBody(BodyType.STATIC)
        val box = world.createBox(body2, Rect(w = 50.0, h = 10.0))
        assertTrue(abs(box.width - 50.0) < 0.001)
        assertTrue(abs(box.height - 10.0) < 0.001)
    }

    @Test
    fun `multiple step calls accumulate motion`() {
        val ctx = PhysicsContext.get()
        ctx.gravity = Vec2(0.0, 100.0)

        val body = Body.createDynamic(1.0, 1.0)
        body.position = Vec2(0.0, 0.0)
        val shape = Shape.Circle(body, 5.0)
        ctx.addObject(PhysicsObject(body, shape))

        repeat(60) { ctx.step(1.0 / 60.0) }

        val pos = body.position
        assertTrue(pos.y > 10.0, "After 60 steps with gravity=100, body should have fallen significantly, y=${pos.y}")
    }
}
