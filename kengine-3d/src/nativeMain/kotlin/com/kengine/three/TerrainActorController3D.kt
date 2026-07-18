package com.kengine.three

import com.kengine.math.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

data class TerrainSurfaceHit3D(
    val position: Vec3,
    val normal: Vec3
)

class TerrainMeshCollider3D private constructor(
    private val triangles: List<TerrainTriangle3D>
) {
    fun groundYAt(
        x: Double,
        z: Double,
        minGroundY: Double? = null,
        maxGroundY: Double? = null
    ): Double? {
        return groundAt(x, z, minGroundY, maxGroundY)?.position?.y
    }

    fun groundAt(
        x: Double,
        z: Double,
        minGroundY: Double? = null,
        maxGroundY: Double? = null
    ): TerrainSurfaceHit3D? {
        var bestHit: TerrainSurfaceHit3D? = null
        triangles.forEach { triangle ->
            val height = triangle.heightAt(x, z) ?: return@forEach
            if (minGroundY != null && height < minGroundY) {
                return@forEach
            }
            if (maxGroundY != null && height > maxGroundY) {
                return@forEach
            }
            if (bestHit == null || height > bestHit!!.position.y) {
                bestHit = TerrainSurfaceHit3D(
                    position = Vec3(x, height, z),
                    normal = triangle.normal
                )
            }
        }
        return bestHit
    }

    companion object {
        fun fromLitVertices(vertices: List<LitVertex3D>): TerrainMeshCollider3D {
            return fromPositions(vertices.map { it.position })
        }

        fun fromPositions(vertices: List<Vec3>): TerrainMeshCollider3D {
            val triangles = mutableListOf<TerrainTriangle3D>()
            for (index in 0 until vertices.size - 2 step 3) {
                triangles += TerrainTriangle3D(
                    a = vertices[index],
                    b = vertices[index + 1],
                    c = vertices[index + 2]
                )
            }
            return TerrainMeshCollider3D(triangles)
        }
    }
}

data class TerrainActorControllerSettings3D(
    val halfHeight: Double = 0.5,
    val maxStepUp: Double = 0.5,
    val maxStepDown: Double = 1.0,
    val groundContactEpsilon: Double = 0.05
)

data class TerrainActorMoveResult3D(
    val position: Vec3,
    val moved: Boolean,
    val isGrounded: Boolean,
    val ground: TerrainSurfaceHit3D?
)

data class TerrainActorVerticalResult3D(
    val position: Vec3,
    val velocityY: Double,
    val isGrounded: Boolean,
    val ground: TerrainSurfaceHit3D?
)

class TerrainActorController3D(
    private val terrain: TerrainMeshCollider3D,
    private val settings: TerrainActorControllerSettings3D = TerrainActorControllerSettings3D()
) {
    fun actorYAt(
        x: Double,
        z: Double,
        minActorY: Double? = null,
        maxActorY: Double? = null
    ): Double? {
        return terrain.groundYAt(
            x = x,
            z = z,
            minGroundY = minActorY?.minus(settings.halfHeight),
            maxGroundY = maxActorY?.minus(settings.halfHeight)
        )?.plus(settings.halfHeight)
    }

    fun moveHorizontal(
        position: Vec3,
        deltaX: Double,
        deltaZ: Double,
        isGrounded: Boolean,
        allowLeaveGround: Boolean = true
    ): TerrainActorMoveResult3D {
        if (abs(deltaX) < 0.000001 && abs(deltaZ) < 0.000001) {
            val ground = groundNearActor(position)
            return TerrainActorMoveResult3D(
                position = position,
                moved = false,
                isGrounded = ground != null,
                ground = ground
            )
        }

        fun candidate(
            xDelta: Double,
            zDelta: Double
        ): TerrainActorMoveResult3D? {
            val nextX = position.x + xDelta
            val nextZ = position.z + zDelta
            val feetY = position.y - settings.halfHeight

            if (!isGrounded) {
                val blockingGround = terrain.groundAt(nextX, nextZ)
                if (blockingGround != null && blockingGround.position.y > feetY + settings.maxStepUp) {
                    return null
                }

                return TerrainActorMoveResult3D(
                    position = Vec3(nextX, position.y, nextZ),
                    moved = true,
                    isGrounded = false,
                    ground = null
                )
            }

            val reachableGround = terrain.groundAt(
                x = nextX,
                z = nextZ,
                minGroundY = feetY - settings.maxStepDown,
                maxGroundY = feetY + settings.maxStepUp
            )
            if (reachableGround != null) {
                return TerrainActorMoveResult3D(
                    position = Vec3(nextX, reachableGround.position.y + settings.halfHeight, nextZ),
                    moved = true,
                    isGrounded = true,
                    ground = reachableGround
                )
            }

            val highestGround = terrain.groundAt(nextX, nextZ)
            if (highestGround != null && highestGround.position.y > feetY + settings.maxStepUp) {
                return null
            }
            if (!allowLeaveGround) {
                return null
            }

            return TerrainActorMoveResult3D(
                position = Vec3(nextX, position.y, nextZ),
                moved = true,
                isGrounded = false,
                ground = null
            )
        }

        return candidate(deltaX, deltaZ)
            ?: candidate(deltaX, 0.0)
            ?: candidate(0.0, deltaZ)
            ?: TerrainActorMoveResult3D(
                position = position,
                moved = false,
                isGrounded = isGrounded,
                ground = groundNearActor(position)
            )
    }

    fun applyGravity(
        position: Vec3,
        velocityY: Double,
        deltaSeconds: Double,
        gravity: Double,
        terminalVelocityY: Double? = null
    ): TerrainActorVerticalResult3D {
        val rawNextVelocityY = velocityY - gravity * deltaSeconds
        val nextVelocityY = terminalVelocityY?.let { maxOf(rawNextVelocityY, it) } ?: rawNextVelocityY
        val nextY = position.y + nextVelocityY * deltaSeconds
        val nextPosition = Vec3(position.x, nextY, position.z)

        if (nextVelocityY <= 0.0) {
            val ground = terrain.groundAt(
                x = position.x,
                z = position.z,
                minGroundY = minOf(position.y, nextY) - settings.halfHeight - settings.groundContactEpsilon,
                maxGroundY = position.y - settings.halfHeight + settings.groundContactEpsilon
            )
            if (ground != null && nextY <= ground.position.y + settings.halfHeight) {
                return TerrainActorVerticalResult3D(
                    position = Vec3(position.x, ground.position.y + settings.halfHeight, position.z),
                    velocityY = 0.0,
                    isGrounded = true,
                    ground = ground
                )
            }

            val shallowRecoveryGround = terrain.groundAt(
                x = position.x,
                z = position.z,
                minGroundY = position.y - settings.halfHeight,
                maxGroundY = position.y - settings.halfHeight + settings.maxStepUp
            )
            if (shallowRecoveryGround != null && nextY <= shallowRecoveryGround.position.y + settings.halfHeight) {
                return TerrainActorVerticalResult3D(
                    position = Vec3(position.x, shallowRecoveryGround.position.y + settings.halfHeight, position.z),
                    velocityY = 0.0,
                    isGrounded = true,
                    ground = shallowRecoveryGround
                )
            }
        }

        return TerrainActorVerticalResult3D(
            position = nextPosition,
            velocityY = nextVelocityY,
            isGrounded = false,
            ground = null
        )
    }

    private fun groundNearActor(position: Vec3): TerrainSurfaceHit3D? {
        return terrain.groundAt(
            x = position.x,
            z = position.z,
            minGroundY = position.y - settings.halfHeight - settings.groundContactEpsilon,
            maxGroundY = position.y - settings.halfHeight + settings.groundContactEpsilon
        )
    }
}

data class TerrainTriangle3D(
    val a: Vec3,
    val b: Vec3,
    val c: Vec3
) {
    val normal: Vec3 = normalize(cross(subtract(b, a), subtract(c, a)))

    fun heightAt(
        x: Double,
        z: Double
    ): Double? {
        val v0x = b.x - a.x
        val v0z = b.z - a.z
        val v1x = c.x - a.x
        val v1z = c.z - a.z
        val v2x = x - a.x
        val v2z = z - a.z
        val denominator = v0x * v1z - v1x * v0z
        if (abs(denominator) < 0.000001) {
            return null
        }

        val beta = (v2x * v1z - v1x * v2z) / denominator
        val gamma = (v0x * v2z - v2x * v0z) / denominator
        val alpha = 1.0 - beta - gamma
        val epsilon = 0.0001
        if (alpha < -epsilon || beta < -epsilon || gamma < -epsilon) {
            return null
        }
        if (alpha > 1.0 + epsilon || beta > 1.0 + epsilon || gamma > 1.0 + epsilon) {
            return null
        }

        return a.y * alpha + b.y * beta + c.y * gamma
    }
}

private fun subtract(
    a: Vec3,
    b: Vec3
): Vec3 {
    return Vec3(a.x - b.x, a.y - b.y, a.z - b.z)
}

private fun cross(
    a: Vec3,
    b: Vec3
): Vec3 {
    return Vec3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x
    )
}

private fun normalize(value: Vec3): Vec3 {
    val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
    if (length == 0.0) {
        return Vec3(0.0, 1.0, 0.0)
    }
    return Vec3(value.x / length, value.y / length, value.z / length)
}
