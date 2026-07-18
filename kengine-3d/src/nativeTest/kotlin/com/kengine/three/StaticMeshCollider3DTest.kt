package com.kengine.three

import com.kengine.math.Vec3
import kotlin.test.Test
import kotlin.test.assertTrue

class StaticMeshCollider3DTest {
    @Test
    fun ignoresWalkableFloorContacts() {
        val collider = StaticMeshCollider3D.fromPositions(
            listOf(
                Vec3(-2.0, 0.0, -2.0),
                Vec3(2.0, 0.0, -2.0),
                Vec3(0.0, 0.0, 2.0)
            )
        )

        val contacts = collider.collideCapsule(
            capsule = CapsuleCollider3D(
                start = Vec3(0.0, 0.35, 0.0),
                end = Vec3(0.0, 1.35, 0.0),
                radius = 0.35
            )
        )

        assertTrue(contacts.isEmpty(), "Floor contacts should stay delegated to TerrainMeshCollider3D.")
    }

    @Test
    fun keepsWalkableFloorIgnoredDuringUpwardMovement() {
        val collider = StaticMeshCollider3D.fromPositions(
            listOf(
                Vec3(-2.0, 0.0, -2.0),
                Vec3(2.0, 0.0, -2.0),
                Vec3(0.0, 0.0, 2.0)
            )
        )

        val contacts = collider.collideCapsule(
            capsule = CapsuleCollider3D(
                start = Vec3(0.0, 0.36, 0.0),
                end = Vec3(0.0, 1.36, 0.0),
                radius = 0.35
            ),
            movement = Vec3(0.0, 1.0, 0.0)
        )

        assertTrue(contacts.isEmpty(), "Upward movement should not turn floor skin contact into a ceiling hit.")
    }

    @Test
    fun resolvesVerticalWallAgainstMovement() {
        val collider = StaticMeshCollider3D.fromPositions(
            listOf(
                Vec3(0.0, -1.0, -2.0),
                Vec3(0.0, 3.0, -2.0),
                Vec3(0.0, 3.0, 2.0)
            )
        )

        val result = collider.resolveCapsule(
            position = Vec3(-0.25, 1.0, 0.0),
            halfHeight = 1.0,
            radius = 0.5,
            movement = Vec3(1.0, 0.0, 0.0)
        )

        assertTrue(result.collided, "Capsule should collide with the wall.")
        assertTrue(result.position.x < 0.05, "Wall collision should push back against movement.")
        assertTrue(result.contacts.any { it.normal.x < -0.9 }, "Contact normal should point away from the wall.")
    }

    @Test
    fun resolvesCeilingAgainstUpwardMovement() {
        val collider = StaticMeshCollider3D.fromPositions(
            listOf(
                Vec3(-2.0, 2.0, -2.0),
                Vec3(2.0, 2.0, -2.0),
                Vec3(0.0, 2.0, 2.0)
            )
        )

        val result = collider.resolveCapsule(
            position = Vec3(0.0, 1.4, 0.0),
            halfHeight = 1.0,
            radius = 0.35,
            movement = Vec3(0.0, 1.0, 0.0)
        )

        assertTrue(result.collided, "Capsule should collide with the ceiling.")
        assertTrue(result.position.y < 1.4, "Ceiling collision should push down against upward movement.")
        assertTrue(result.contacts.any { it.normal.y < -0.9 }, "Contact normal should point down from the ceiling.")
    }
}
