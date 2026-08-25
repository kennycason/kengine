package mario64

/**
 * Allocation-free fixed-step body for proving terrain locomotion on a static
 * triangle grid. Wall/capsule response is intentionally the next layer; this
 * class currently owns floor following, gravity, jumping, landing, and reset.
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

    init {
        reset()
    }

    fun reset() {
        x = spawnX
        z = spawnZ
        val spawnGround = collision.groundHeightAt(x, z)
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
    }

    /** Advances one fixed simulation step using a desired X/Z displacement. */
    fun step(moveX: Int, moveZ: Int, jumpPressed: Boolean) {
        if (jumpPressed && grounded) {
            grounded = false
            supportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
            verticalVelocity = JUMP_VELOCITY
        }

        val nextX = x + moveX
        val nextZ = z + moveZ
        if (grounded) {
            val ground = collision.groundHeightAt(nextX, nextZ, y + MAXIMUM_STEP_UP)
            if (ground != StaticTriangleGrid3D.NO_GROUND && ground >= y - GROUND_SNAP_DISTANCE) {
                x = nextX
                y = ground
                z = nextZ
                verticalVelocity = 0
                supportTriangle = collision.lastSupportTriangle
            } else {
                x = nextX
                z = nextZ
                grounded = false
                supportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
            }
        } else {
            x = nextX
            z = nextZ
        }

        if (!grounded) {
            verticalVelocity = maxOf(TERMINAL_FALL_VELOCITY, verticalVelocity - GRAVITY)
            val previousY = y
            val nextY = y + verticalVelocity
            if (verticalVelocity <= 0) {
                val ground = collision.groundHeightAt(x, z, previousY)
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

    companion object {
        private const val MAXIMUM_STEP_UP = 64
        private const val GROUND_SNAP_DISTANCE = 96
        private const val GRAVITY = 4
        private const val JUMP_VELOCITY = 48
        private const val TERMINAL_FALL_VELOCITY = -80
        private const val FALL_RESET_DISTANCE = 2048
    }
}
