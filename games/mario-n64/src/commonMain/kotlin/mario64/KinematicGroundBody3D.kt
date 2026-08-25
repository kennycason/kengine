package mario64

/**
 * Allocation-free fixed-step body for proving terrain locomotion on a static
 * triangle grid. It owns floor following, a horizontal body radius with wall
 * sliding, gravity, jumping, landing, and reset.
 */
class KinematicGroundBody3D(
    private val collision: StaticTriangleGrid3D,
    private val spawnX: Int,
    private val spawnZ: Int
) {
    var x: Int = spawnX
        private set

    var y: Int = 0
        private set

    var z: Int = spawnZ
        private set

    var verticalVelocity: Int = 0
        private set

    var grounded: Boolean = false
        private set

    var supportTriangle: Int = StaticTriangleGrid3D.NO_TRIANGLE
        private set

    var wallTriangle: Int = StaticTriangleGrid3D.NO_TRIANGLE
        private set

    var wallCollisionCount: Int = 0
        private set

    init {
        reset()
    }

    fun reset() {
        x = spawnX
        z = spawnZ
        val spawnGround = collision.groundHeightNear(x, z)
        if (spawnGround == StaticTriangleGrid3D.NO_GROUND) {
            y = collision.maxY + MAXIMUM_STEP_UP
            grounded = false
            supportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
        } else {
            y = spawnGround
            grounded = true
            supportTriangle = collision.lastSupportTriangle
        }
        verticalVelocity = 0
        wallTriangle = StaticTriangleGrid3D.NO_TRIANGLE
        wallCollisionCount = 0
    }

    /** Advances one fixed simulation step using a desired X/Z displacement. */
    fun step(moveX: Int, moveZ: Int, jumpPressed: Boolean) {
        if (jumpPressed && grounded) {
            grounded = false
            supportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
            verticalVelocity = JUMP_VELOCITY
        }

        wallTriangle = StaticTriangleGrid3D.NO_TRIANGLE
        wallCollisionCount = 0
        moveHorizontal(moveX, moveZ)

        if (!grounded) {
            verticalVelocity = maxOf(TERMINAL_FALL_VELOCITY, verticalVelocity - GRAVITY)
            val previousY = y
            val nextY = y + verticalVelocity
            if (verticalVelocity <= 0) {
                val ground = collision.groundHeightNear(x, z, previousY)
                if (ground != StaticTriangleGrid3D.NO_GROUND && nextY <= ground) {
                    y = ground
                    verticalVelocity = 0
                    grounded = true
                    supportTriangle = collision.lastSupportTriangle
                    return
                }
            }
            y = nextY

            if (y < collision.minY - FALL_RESET_DISTANCE) {
                reset()
            }
        }
    }

    private fun moveHorizontal(moveX: Int, moveZ: Int) {
        val greatestMovement = maxOf(absInt(moveX), absInt(moveZ))
        val substepCount = minOf(
            MAXIMUM_HORIZONTAL_SUBSTEPS,
            maxOf(1, (greatestMovement + MAXIMUM_HORIZONTAL_SUBSTEP - 1) / MAXIMUM_HORIZONTAL_SUBSTEP)
        )
        var appliedX = 0
        var appliedZ = 0
        var substep = 1
        while (substep <= substepCount) {
            val targetAppliedX = moveX * substep / substepCount
            val targetAppliedZ = moveZ * substep / substepCount
            val nextX = x + targetAppliedX - appliedX
            val nextZ = z + targetAppliedZ - appliedZ
            collision.resolveWalls(
                x = nextX,
                sampleY = y + WALL_SAMPLE_HEIGHT,
                z = nextZ,
                radius = BODY_RADIUS
            )
            val resolvedX = collision.lastResolvedX
            val resolvedZ = collision.lastResolvedZ
            wallCollisionCount += collision.lastWallCollisionCount
            if (collision.lastWallTriangle != StaticTriangleGrid3D.NO_TRIANGLE) {
                wallTriangle = collision.lastWallTriangle
            }

            if (grounded) {
                val ground = collision.groundHeightNear(
                    resolvedX,
                    resolvedZ,
                    y + MAXIMUM_STEP_UP
                )
                if (ground != StaticTriangleGrid3D.NO_GROUND && ground >= y - GROUND_SNAP_DISTANCE) {
                    x = resolvedX
                    y = ground
                    z = resolvedZ
                    verticalVelocity = 0
                    supportTriangle = collision.lastSupportTriangle
                } else {
                    x = resolvedX
                    z = resolvedZ
                    grounded = false
                    supportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
                }
            } else {
                x = resolvedX
                z = resolvedZ
            }

            appliedX = targetAppliedX
            appliedZ = targetAppliedZ
            substep += 1
        }
    }

    private fun absInt(value: Int): Int = if (value < 0) -value else value

    companion object {
        private const val MAXIMUM_STEP_UP = 64
        private const val GROUND_SNAP_DISTANCE = 96
        private const val GRAVITY = 4
        private const val JUMP_VELOCITY = 48
        private const val TERMINAL_FALL_VELOCITY = -80
        private const val FALL_RESET_DISTANCE = 2048
        private const val BODY_RADIUS = 50
        private const val WALL_SAMPLE_HEIGHT = 60
        private const val MAXIMUM_HORIZONTAL_SUBSTEP = 12
        private const val MAXIMUM_HORIZONTAL_SUBSTEPS = 4
    }
}
