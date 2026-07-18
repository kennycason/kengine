package com.kengine.three

import com.kengine.math.Vec3

data class KinematicCharacterControllerSettings3D(
    val halfHeight: Double = 0.5,
    val maxStepUp: Double = 0.5,
    val maxStepDown: Double = 1.0,
    val groundContactEpsilon: Double = 0.05,
    val jumpVelocity: Double = 8.0,
    val gravity: Double = 24.0,
    val fallingGravity: Double = gravity,
    val terminalVelocityY: Double? = null
)

data class KinematicCharacterState3D(
    var position: Vec3,
    var velocityY: Double = 0.0,
    var isGrounded: Boolean = false,
    var ground: TerrainSurfaceHit3D? = null
)

data class KinematicCharacterStepResult3D(
    val previousPosition: Vec3,
    val positionBeforeVertical: Vec3,
    val position: Vec3,
    val velocityY: Double,
    val isGrounded: Boolean,
    val ground: TerrainSurfaceHit3D?,
    val movedHorizontally: Boolean,
    val jumped: Boolean,
    val landed: Boolean
)

class KinematicCharacterController3D(
    val terrainController: TerrainActorController3D,
    val settings: KinematicCharacterControllerSettings3D = KinematicCharacterControllerSettings3D()
) {
    constructor(
        terrain: TerrainMeshCollider3D,
        settings: KinematicCharacterControllerSettings3D = KinematicCharacterControllerSettings3D()
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
        settings = settings
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

        return KinematicCharacterStepResult3D(
            previousPosition = previousPosition,
            positionBeforeVertical = positionBeforeVertical,
            position = state.position,
            velocityY = state.velocityY,
            isGrounded = state.isGrounded,
            ground = state.ground,
            movedHorizontally = horizontalMove.moved,
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
        return move
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

    private fun refreshGrounded(state: KinematicCharacterState3D): Boolean {
        val groundY = terrainController.actorYAt(
            x = state.position.x,
            z = state.position.z,
            maxActorY = state.position.y + settings.groundContactEpsilon
        )
        val ground = groundY?.let {
            TerrainSurfaceHit3D(
                position = Vec3(state.position.x, it - settings.halfHeight, state.position.z),
                normal = Vec3(0.0, 1.0, 0.0)
            )
        }
        state.isGrounded = groundY != null &&
            state.position.y <= groundY + settings.groundContactEpsilon &&
            state.velocityY <= 0.1
        state.ground = if (state.isGrounded) ground else null
        return state.isGrounded
    }
}
