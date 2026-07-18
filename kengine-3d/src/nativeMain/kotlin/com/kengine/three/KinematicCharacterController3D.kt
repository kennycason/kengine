package com.kengine.three

import com.kengine.math.Vec3

data class KinematicCharacterControllerSettings3D(
    val halfHeight: Double = 0.5,
    val collisionRadius: Double = 0.35,
    val maxStepUp: Double = 0.5,
    val maxStepDown: Double = 1.0,
    val groundContactEpsilon: Double = 0.05,
    val jumpVelocity: Double = 8.0,
    val gravity: Double = 24.0,
    val fallingGravity: Double = gravity,
    val terminalVelocityY: Double? = null,
    val staticCollisionSkinWidth: Double = 0.02,
    val staticCollisionIterations: Int = 4,
    val staticCollisionIgnoredFloorNormalY: Double = 0.55
)

data class KinematicCharacterState3D(
    var position: Vec3,
    var velocityY: Double = 0.0,
    var isGrounded: Boolean = false,
    var ground: TerrainSurfaceHit3D? = null,
    var staticContacts: List<StaticMeshContact3D> = emptyList()
)

data class KinematicCharacterStepResult3D(
    val previousPosition: Vec3,
    val positionBeforeVertical: Vec3,
    val position: Vec3,
    val velocityY: Double,
    val isGrounded: Boolean,
    val ground: TerrainSurfaceHit3D?,
    val staticContacts: List<StaticMeshContact3D>,
    val movedHorizontally: Boolean,
    val jumped: Boolean,
    val landed: Boolean
)

class KinematicCharacterController3D(
    val terrainController: TerrainActorController3D,
    val settings: KinematicCharacterControllerSettings3D = KinematicCharacterControllerSettings3D(),
    private val staticCollider: StaticMeshCollider3D? = null
) {
    constructor(
        terrain: TerrainMeshCollider3D,
        settings: KinematicCharacterControllerSettings3D = KinematicCharacterControllerSettings3D(),
        staticCollider: StaticMeshCollider3D? = null
    ) : this(
        terrainController = TerrainActorController3D(
            terrain = terrain,
            settings = TerrainActorControllerSettings3D(
                halfHeight = settings.halfHeight,
                maxStepUp = settings.maxStepUp,
                maxStepDown = settings.maxStepDown,
                groundContactEpsilon = settings.groundContactEpsilon
            )
        ),
        settings = settings,
        staticCollider = staticCollider
    )

    fun actorYAt(
        x: Double,
        z: Double,
        minActorY: Double? = null,
        maxActorY: Double? = null
    ): Double? {
        return terrainController.actorYAt(x, z, minActorY, maxActorY)
    }

    fun step(
        state: KinematicCharacterState3D,
        horizontalVelocity: Vec3,
        deltaSeconds: Double,
        jumpRequested: Boolean = false,
        allowLeaveGround: Boolean = true
    ): KinematicCharacterStepResult3D {
        val previousPosition = state.position
        state.staticContacts = emptyList()
        val wasGrounded = refreshGrounded(state)
        val horizontalMove = terrainController.moveHorizontal(
            position = state.position,
            deltaX = horizontalVelocity.x * deltaSeconds,
            deltaZ = horizontalVelocity.z * deltaSeconds,
            isGrounded = state.isGrounded,
            allowLeaveGround = allowLeaveGround
        )
        state.position = horizontalMove.position
        state.isGrounded = horizontalMove.isGrounded
        state.ground = horizontalMove.ground
        val horizontalCollision = resolveStaticCollisions(
            position = state.position,
            movement = Vec3(
                horizontalMove.position.x - previousPosition.x,
                0.0,
                horizontalMove.position.z - previousPosition.z
            )
        )
        state.position = terrainSupportedHorizontalPosition(
            position = Vec3(
                horizontalCollision.position.x,
                horizontalMove.position.y,
                horizontalCollision.position.z
            ),
            wasGrounded = horizontalMove.isGrounded
        )
        state.staticContacts = horizontalCollision.contacts
        if (horizontalCollision.collided) {
            refreshGrounded(state)
        }

        val jumped = if (jumpRequested && state.isGrounded) {
            state.velocityY = settings.jumpVelocity
            state.isGrounded = false
            state.ground = null
            true
        } else {
            false
        }

        val positionBeforeVertical = state.position
        val verticalMove = terrainController.applyGravity(
            position = state.position,
            velocityY = state.velocityY,
            deltaSeconds = deltaSeconds,
            gravity = if (state.velocityY > 0.0) settings.gravity else settings.fallingGravity,
            terminalVelocityY = settings.terminalVelocityY
        )
        state.position = verticalMove.position
        state.velocityY = verticalMove.velocityY
        state.isGrounded = verticalMove.isGrounded
        state.ground = verticalMove.ground
        val verticalDeltaY = verticalMove.position.y - positionBeforeVertical.y
        if (verticalDeltaY > 0.0) {
            val verticalCollision = resolveStaticCollisions(
                position = state.position,
                movement = Vec3(0.0, verticalDeltaY, 0.0)
            )
            if (verticalCollision.collided) {
                val hitCeiling = verticalCollision.contacts.any { it.normal.y < -0.35 }
                state.position = verticalCollision.position
                state.staticContacts = state.staticContacts + verticalCollision.contacts
                if (hitCeiling && state.velocityY > 0.0) {
                    state.velocityY = 0.0
                }
                refreshGrounded(state)
            }
        }

        return KinematicCharacterStepResult3D(
            previousPosition = previousPosition,
            positionBeforeVertical = positionBeforeVertical,
            position = state.position,
            velocityY = state.velocityY,
            isGrounded = state.isGrounded,
            ground = state.ground,
            staticContacts = state.staticContacts,
            movedHorizontally = horizontalMove.moved || horizontalCollision.collided,
            jumped = jumped,
            landed = !wasGrounded && state.isGrounded
        )
    }

    fun moveHorizontal(
        state: KinematicCharacterState3D,
        deltaX: Double,
        deltaZ: Double,
        allowLeaveGround: Boolean = true
    ): TerrainActorMoveResult3D {
        refreshGrounded(state)
        val previousPosition = state.position
        val move = terrainController.moveHorizontal(
            position = state.position,
            deltaX = deltaX,
            deltaZ = deltaZ,
            isGrounded = state.isGrounded,
            allowLeaveGround = allowLeaveGround
        )
        state.position = move.position
        state.isGrounded = move.isGrounded
        state.ground = move.ground
        val collision = resolveStaticCollisions(
            position = state.position,
            movement = Vec3(
                move.position.x - previousPosition.x,
                0.0,
                move.position.z - previousPosition.z
            )
        )
        state.position = terrainSupportedHorizontalPosition(
            position = Vec3(collision.position.x, move.position.y, collision.position.z),
            wasGrounded = move.isGrounded
        )
        state.staticContacts = collision.contacts
        return if (collision.collided) {
            val ground = terrainController.groundNearActor(state.position)
            TerrainActorMoveResult3D(
                position = state.position,
                moved = true,
                isGrounded = ground != null,
                ground = ground
            )
        } else {
            move
        }
    }

    fun capsuleCollider(
        state: KinematicCharacterState3D,
        radius: Double
    ): CapsuleCollider3D {
        return capsuleCollider(state.position, radius)
    }

    fun capsuleCollider(
        position: Vec3,
        radius: Double
    ): CapsuleCollider3D {
        return CapsuleCollider3D(
            start = Vec3(position.x, position.y - settings.halfHeight + radius, position.z),
            end = Vec3(position.x, position.y + settings.halfHeight - radius, position.z),
            radius = radius
        )
    }

    private fun resolveStaticCollisions(
        position: Vec3,
        movement: Vec3
    ): StaticMeshCapsuleResolveResult3D {
        val collider = staticCollider ?: return StaticMeshCapsuleResolveResult3D(
            position = position,
            contacts = emptyList(),
            collided = false
        )
        return collider.resolveCapsule(
            position = position,
            halfHeight = settings.halfHeight,
            radius = settings.collisionRadius,
            ignoredFloorNormalY = settings.staticCollisionIgnoredFloorNormalY,
            skinWidth = settings.staticCollisionSkinWidth,
            iterations = settings.staticCollisionIterations,
            movement = movement
        )
    }

    private fun terrainSupportedHorizontalPosition(
        position: Vec3,
        wasGrounded: Boolean
    ): Vec3 {
        if (!wasGrounded) {
            return position
        }

        val actorY = terrainController.actorYAt(
            x = position.x,
            z = position.z,
            minActorY = position.y - settings.maxStepDown,
            maxActorY = position.y + settings.maxStepUp
        ) ?: return position

        return Vec3(position.x, actorY, position.z)
    }

    private fun refreshGrounded(state: KinematicCharacterState3D): Boolean {
        val ground = terrainController.groundNearActor(state.position)
            ?: terrainController.actorYAt(
                x = state.position.x,
                z = state.position.z,
                maxActorY = state.position.y + settings.groundContactEpsilon
            )?.let {
                TerrainSurfaceHit3D(
                    position = Vec3(state.position.x, it - settings.halfHeight, state.position.z),
                    normal = Vec3(0.0, 1.0, 0.0)
                )
            }
        val groundY = ground?.position?.y?.plus(settings.halfHeight)
        state.isGrounded = groundY != null &&
            state.position.y <= groundY + settings.groundContactEpsilon &&
            state.velocityY <= 0.1
        state.ground = if (state.isGrounded) {
            ground
        } else {
            null
        }
        return state.isGrounded
    }
}
