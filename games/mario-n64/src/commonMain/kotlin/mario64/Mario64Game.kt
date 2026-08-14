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

    private var playerX = -3000
    private var playerY = PLAYER_START_Y
    private var playerZ = -3000
    private var cameraYaw = INITIAL_YAW
    private var cameraPitch = INITIAL_PITCH

    override fun update(input: InputState) {
        val forwardAxis = input.axis(InputButton.DPAD_DOWN, InputButton.DPAD_UP)
        val strafeAxis = input.axis(InputButton.DPAD_LEFT, InputButton.DPAD_RIGHT)

        if (forwardAxis != 0 || strafeAxis != 0) {
            val fwdCos = cosAngle(cameraYaw)
            val fwdSin = sinAngle(cameraYaw)
            val speed = if (input.isPressed(InputButton.B)) RUN_SPEED else WALK_SPEED
            playerX += trigMul(-strafeAxis * speed, fwdCos) + trigMul(forwardAxis * speed, fwdSin)
            playerZ += trigMul(strafeAxis * speed, fwdSin) + trigMul(forwardAxis * speed, fwdCos)
        }

        cameraYaw = wrapAngle(
            cameraYaw + input.axis(InputButton.C_RIGHT, InputButton.C_LEFT) * YAW_SPEED
        )
        cameraPitch = clampInt(
            cameraPitch + input.axis(InputButton.C_DOWN, InputButton.C_UP) * PITCH_SPEED,
            MIN_PITCH,
            MAX_PITCH
        )

        if (input.isPressed(InputButton.A)) {
            playerY += VERTICAL_SPEED
        }
        if (input.isPressed(InputButton.Z)) {
            playerY -= VERTICAL_SPEED
        }

        val startJustPressed = (input.mask and InputState.bitFor(InputButton.START)) != 0 &&
            (previousInputMask and InputState.bitFor(InputButton.START)) == 0
        if (startJustPressed) {
            playerX = -3000
            playerY = PLAYER_START_Y
            playerZ = -3000
            cameraYaw = INITIAL_YAW
            cameraPitch = INITIAL_PITCH
        }

        frame += 1
        previousInputMask = input.mask
    }

    override fun draw(render: RenderContext) {
        render.clear(rgba(92, 148, 252))
        render.drawWorld3D(
            meshId = BATTLEFIELD_MESH_ID,
            cameraX = playerX,
            cameraY = playerY,
            cameraZ = playerZ,
            cameraYaw = cameraYaw,
            cameraPitch = cameraPitch,
            projectionDistance = PROJECTION_DISTANCE
        )
    }

    override fun cleanup() {}

    companion object {
        val BATTLEFIELD_MESH_ID = RenderAssetId.mesh("battlefield")

        private const val WALK_SPEED = 24
        private const val RUN_SPEED = 64
        private const val VERTICAL_SPEED = 20
        private const val YAW_SPEED = 18
        private const val PITCH_SPEED = 9
        private const val INITIAL_YAW = 128
        private const val INITIAL_PITCH = -60
        private const val MIN_PITCH = -250
        private const val MAX_PITCH = 250
        private const val PLAYER_START_Y = 2500
        private const val PROJECTION_DISTANCE = 300
    }
}
