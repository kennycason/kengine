package com.kengine.physics

import com.kengine.math.Vec2
import com.kengine.test.expectThat
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class BodyTest {

    private val ctx = PhysicsContext.get()

    @AfterTest
    fun cleanup() {
        ctx.cleanup()
    }

    @Test
    fun `create dynamic body`() {
        val body = Body.createDynamic(mass = 5.0, moment = 10.0)
        expectThat(body.type).isEqualTo(BodyType.DYNAMIC)
        expectThat(body.isDynamic).isTrue()
        expectThat(body.isStatic).isFalse()
        expectThat(body.isKinematic).isFalse()
        expectThat(body.isDestroyed).isFalse()
    }

    @Test
    fun `create static body`() {
        val body = Body.createStatic()
        expectThat(body.type).isEqualTo(BodyType.STATIC)
        expectThat(body.isStatic).isTrue()
        expectThat(body.isDynamic).isFalse()
    }

    @Test
    fun `create kinematic body`() {
        val body = Body.createKinematic()
        expectThat(body.type).isEqualTo(BodyType.KINEMATIC)
        expectThat(body.isKinematic).isTrue()
        expectThat(body.isDynamic).isFalse()
    }

    @Test
    fun `set and get position`() {
        val body = Body.createDynamic(1.0, 1.0)
        body.position = Vec2(100.0, 200.0)
        val pos = body.position
        assertTrue(abs(pos.x - 100.0) < 0.001, "x: expected 100.0, got ${pos.x}")
        assertTrue(abs(pos.y - 200.0) < 0.001, "y: expected 200.0, got ${pos.y}")
    }

    @Test
    fun `set and get velocity`() {
        val body = Body.createDynamic(1.0, 1.0)
        body.velocity = Vec2(50.0, -30.0)
        val vel = body.velocity
        assertTrue(abs(vel.x - 50.0) < 0.001)
        assertTrue(abs(vel.y - (-30.0)) < 0.001)
    }

    @Test
    fun `set and get angle`() {
        val body = Body.createDynamic(1.0, 1.0)
        body.angle = 1.5
        assertTrue(abs(body.angle - 1.5) < 0.001)
    }

    @Test
    fun `set and get angular velocity`() {
        val body = Body.createDynamic(1.0, 1.0)
        body.angularVelocity = 3.14
        assertTrue(abs(body.angularVelocity - 3.14) < 0.001)
    }

    @Test
    fun `destroyed body returns defaults`() {
        val body = Body.createDynamic(1.0, 1.0)
        val shape = Shape.Circle(body, 10.0)
        val obj = PhysicsObject(body, shape)
        ctx.addObject(obj)
        ctx.removeObject(obj)

        expectThat(body.isDestroyed).isTrue()
        val pos = body.position
        expectThat(pos.x).isEqualTo(0.0)
        expectThat(pos.y).isEqualTo(0.0)
        expectThat(body.angle).isEqualTo(0.0)
        expectThat(body.angularVelocity).isEqualTo(0.0)
    }
}
