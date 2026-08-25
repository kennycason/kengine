package mario64

import com.kengine.PortableGame
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderAssetId
import com.kengine.render.RenderContext
import com.kengine.storage.PortableStorage

class Mario64Game : PortableGame {
    override val storageNamespace: String = "mario64"

    private var frame = 0
    private var previousInputMask = 0

    private val collision = Mario64CollisionAssets.battlefield
    private var playerX = PLAYER_START_X
    private var playerY = 0
    private var playerZ = PLAYER_START_Z
    private var playerVerticalVelocity = 0
    private var playerGrounded = false
    private var playerSupportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
    private var cameraYaw = INITIAL_YAW
    private var cameraPitch = INITIAL_PITCH

    init {
        resetPlayer()
    }

    val bodyX: Int
        get() = playerX

    val bodyY: Int
        get() = playerY

    val bodyZ: Int
        get() = playerZ

    val verticalVelocity: Int
        get() = playerVerticalVelocity

    val isGrounded: Boolean
        get() = playerGrounded

    val supportTriangle: Int
        get() = playerSupportTriangle

    override fun update(input: InputState) {
        val forwardAxis = input.axis(InputButton.DPAD_DOWN, InputButton.DPAD_UP)
        val strafeAxis = input.axis(InputButton.DPAD_LEFT, InputButton.DPAD_RIGHT)

        var moveX = 0
        var moveZ = 0
        if (forwardAxis != 0 || strafeAxis != 0) {
            val fwdCos = cosAngle(cameraYaw)
            val fwdSin = sinAngle(cameraYaw)
            val speed = if (input.isPressed(InputButton.B)) RUN_SPEED else WALK_SPEED
            moveX = trigMul(-strafeAxis * speed, fwdCos) + trigMul(forwardAxis * speed, fwdSin)
            moveZ = trigMul(strafeAxis * speed, fwdSin) + trigMul(forwardAxis * speed, fwdCos)
        }

        cameraYaw = wrapAngle(
            cameraYaw + input.axis(InputButton.C_RIGHT, InputButton.C_LEFT) * YAW_SPEED
        )
        cameraPitch = clampInt(
            cameraPitch + input.axis(InputButton.C_DOWN, InputButton.C_UP) * PITCH_SPEED,
            MIN_PITCH,
            MAX_PITCH
        )

        val startJustPressed = (input.mask and InputState.bitFor(InputButton.START)) != 0 &&
            (previousInputMask and InputState.bitFor(InputButton.START)) == 0
        if (startJustPressed) {
            resetPlayer()
            cameraYaw = INITIAL_YAW
            cameraPitch = INITIAL_PITCH
        } else {
            val jumpJustPressed = (input.mask and InputState.bitFor(InputButton.A)) != 0 &&
                (previousInputMask and InputState.bitFor(InputButton.A)) == 0
            stepPlayer(moveX, moveZ, jumpJustPressed)
        }

        frame += 1
        previousInputMask = input.mask
    }

    override fun draw(render: RenderContext) {
        render.clear(rgba(92, 148, 252))
        render.drawWorld3D(
            meshId = BATTLEFIELD_MESH_ID,
            cameraX = playerX,
            cameraY = playerY + CAMERA_EYE_HEIGHT,
            cameraZ = playerZ,
            cameraYaw = cameraYaw,
            cameraPitch = cameraPitch,
            projectionDistance = PROJECTION_DISTANCE
        )
    }

    /**
     * The N64 backend currently miscompiles construction of the equivalent
     * [KinematicGroundBody3D] wrapper. Keep this state primitive and game-local
     * until that ABI issue is fixed; the generic class remains the reference
     * implementation exercised by the JVM tests.
     */
    private fun resetPlayer() {
        playerX = PLAYER_START_X
        playerZ = PLAYER_START_Z
        val spawnGround = collision.groundHeightAt(playerX, playerZ)
        if (spawnGround == StaticTriangleGrid3D.NO_GROUND) {
            playerY = collision.maxY + MAXIMUM_STEP_UP
            playerGrounded = false
            playerSupportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
        } else {
            playerY = spawnGround
            playerGrounded = true
            playerSupportTriangle = collision.lastSupportTriangle
        }
        playerVerticalVelocity = 0
    }

    private fun stepPlayer(moveX: Int, moveZ: Int, jumpPressed: Boolean) {
        if (jumpPressed && playerGrounded) {
            playerGrounded = false
            playerSupportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
            playerVerticalVelocity = JUMP_VELOCITY
        }

        val nextX = playerX + moveX
        val nextZ = playerZ + moveZ
        if (playerGrounded) {
            val ground = collision.groundHeightAt(nextX, nextZ, playerY + MAXIMUM_STEP_UP)
            if (ground != StaticTriangleGrid3D.NO_GROUND && ground >= playerY - GROUND_SNAP_DISTANCE) {
                playerX = nextX
                playerY = ground
                playerZ = nextZ
                playerVerticalVelocity = 0
                playerSupportTriangle = collision.lastSupportTriangle
            } else {
                playerX = nextX
                playerZ = nextZ
                playerGrounded = false
                playerSupportTriangle = StaticTriangleGrid3D.NO_TRIANGLE
            }
        } else {
            playerX = nextX
            playerZ = nextZ
        }

        if (!playerGrounded) {
            playerVerticalVelocity = maxOf(
                TERMINAL_FALL_VELOCITY,
                playerVerticalVelocity - GRAVITY
            )
            val previousY = playerY
            val nextY = playerY + playerVerticalVelocity
            if (playerVerticalVelocity <= 0) {
                val ground = collision.groundHeightAt(playerX, playerZ, previousY)
                if (ground != StaticTriangleGrid3D.NO_GROUND && nextY <= ground) {
                    playerY = ground
                    playerVerticalVelocity = 0
                    playerGrounded = true
                    playerSupportTriangle = collision.lastSupportTriangle
                    return
                }
            }
            playerY = nextY

            if (playerY < collision.minY - FALL_RESET_DISTANCE) {
                resetPlayer()
            }
        }
    }

    override fun cleanup() {}

    companion object {
        val BATTLEFIELD_MESH_ID = RenderAssetId.mesh("battlefield")
        private const val WALK_SPEED = 24
        private const val RUN_SPEED = 64
        private const val YAW_SPEED = 18
        private const val PITCH_SPEED = 9
        private const val INITIAL_YAW = 128
        private const val INITIAL_PITCH = -60
        private const val MIN_PITCH = -250
        private const val MAX_PITCH = 250
        private const val PLAYER_START_X = -3000
        private const val PLAYER_START_Z = -3000
        private const val MAXIMUM_STEP_UP = 64
        private const val GROUND_SNAP_DISTANCE = 96
        private const val GRAVITY = 4
        private const val JUMP_VELOCITY = 48
        private const val TERMINAL_FALL_VELOCITY = -80
        private const val FALL_RESET_DISTANCE = 2048
        const val CAMERA_EYE_HEIGHT = 180
        private const val PROJECTION_DISTANCE = 300
    }
}
