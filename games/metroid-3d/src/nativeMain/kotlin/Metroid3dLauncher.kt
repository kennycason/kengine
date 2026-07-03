import com.kengine.createGameContext
import com.kengine.file.File
import com.kengine.graphics.Color
import com.kengine.hooks.context.useContext
import com.kengine.input.controller.ControllerInputEventSubscriber
import com.kengine.input.controller.controls.Buttons
import com.kengine.log.Logger
import com.kengine.math.Vec3
import com.kengine.sdl.RenderBackend
import com.kengine.three.Camera3D
import com.kengine.three.DirectionalLight3D
import com.kengine.three.GlbMeshLoadOptions
import com.kengine.three.GlbMeshLoader
import com.kengine.three.GpuContext
import com.kengine.three.GpuMesh
import com.kengine.three.LitGpuMesh
import com.kengine.three.LitMeshRenderer3D
import com.kengine.three.LitVertex3D
import com.kengine.three.Mat4
import com.kengine.three.MeshRenderer3D
import com.kengine.three.ObjMeshLoadOptions
import com.kengine.three.ObjMeshLoader
import com.kengine.three.Transform3D
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import sdl3.SDL_GetTicks
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val WINDOW_WIDTH = 960
private const val WINDOW_HEIGHT = 540
private const val LEFT_STICK_X_AXIS = 0
private const val LEFT_STICK_Y_AXIS = 1
private const val RIGHT_STICK_X_AXIS = 2
private const val RIGHT_STICK_Y_AXIS = 3
private const val CONTROLLER_AXIS_COUNT = 6
private const val CONTROLLER_DEADZONE = 0.14
private const val CONTROLLER_CALIBRATION_SECONDS = 0.75
private const val WORLD_TARGET_SIZE = 105.0
private const val PLAYER_HALF_HEIGHT = 0.58
private const val PLAYER_MOVE_SPEED = 6.2
private const val PLAYER_JUMP_VELOCITY = 8.4
private const val PLAYER_GRAVITY = 15.5
private const val SAMUS_MODEL_YAW_OFFSET = 3.141592653589793
private const val GROUND_CONTACT_EPSILON = 0.05
private const val MAX_STEP_UP = 0.72
private const val CAMERA_YAW_SPEED = 3.0
private const val CAMERA_PITCH_SPEED = 1.75
private const val CAMERA_LOOK_SMOOTHING = 18.0
private const val CAMERA_FOLLOW_SMOOTHING = 10.0
private const val CAMERA_MIN_PITCH = -0.08
private const val CAMERA_MAX_PITCH = 0.95
private val CAMERA_DISTANCES = doubleArrayOf(3.7, 4.9, 6.2)

@OptIn(ExperimentalForeignApi::class)
fun main() {
    createGameContext(
        title = "Kengine - Metroid 3D",
        width = WINDOW_WIDTH,
        height = WINDOW_HEIGHT,
        logLevel = Logger.Level.INFO,
        renderBackend = RenderBackend.SDL_GPU_3D
    ) {
        useContext(GpuContext.create(sdl), cleanup = true) {
            val worldAssetPath = resolveMetroidAsset("models/metroid3d/Super Mario 64 Bob-Omb Battlefield.glb")
            val worldOptions = GlbMeshLoadOptions(
                targetSize = WORLD_TARGET_SIZE,
                defaultColor = Color.fromHex("7fbf72")
            )
            val worldVertices = GlbMeshLoader.loadLitVertices(worldAssetPath, worldOptions)
            val world = LitGpuMesh.create(this, worldVertices)
            val terrain = TerrainCollision.fromVertices(worldVertices)
            val samus = ObjMeshLoader.loadLit(
                gpu = this,
                assetPath = resolveMetroidAsset("models/metroid3d/Samus Super Smash Bros N64/samus.obj"),
                options = ObjMeshLoadOptions(
                    targetSize = 1.15,
                    defaultColor = Color.fromHex("f0a43c")
                )
            )
            val projectileMesh = GpuMesh.sphere(
                gpu = this,
                radius = 0.07,
                color = Color.fromHex("ffde66"),
                rings = 8,
                segments = 12
            )
            val meshRenderer = MeshRenderer3D(this)
            val litRenderer = LitMeshRenderer3D(this)
            val light = DirectionalLight3D(
                direction = Vec3(-0.45, -0.85, -0.32),
                color = Color.fromHex("fff8e6"),
                ambientStrength = 0.42f,
                diffuseStrength = 0.8f
            )

            var player = Vec3(0.0, terrain.playerYAt(0.0, 0.0) ?: PLAYER_HALF_HEIGHT, 0.0)
            var cameraFocus = player
            var playerVelocityY = 0.0
            var playerYaw = 0.0
            var cameraYaw = 0.0
            var cameraPitch = 0.38
            var cameraLookX = 0.0
            var cameraLookY = 0.0
            val projectiles = mutableListOf<Projectile>()
            var controllerNeutral: FloatArray? = null
            var calibratedControllerId: UInt? = null
            var controllerCalibrationUntil = 0.0
            var wasJumpPressed = false
            var wasCameraDistancePressed = false
            var cameraDistanceIndex = 0
            var nextFireAt = 0.0
            var previousTicks = SDL_GetTicks()

            try {
                while (isRunning) {
                    sdlEvent.pollEvents()
                    action.update()

                    val ticks = SDL_GetTicks()
                    val deltaSeconds = ((ticks - previousTicks).toDouble() / 1000.0).coerceIn(0.0, 0.1)
                    previousTicks = ticks
                    val elapsedSeconds = ticks.toDouble() / 1000.0
                    val keyboardState = keyboard.keyboard
                    val controllerState = controller.controller
                    val controllerId = controllerState.getFirstControllerId()
                    val isControllerCalibrating = if (controllerId == null) {
                        controllerNeutral = null
                        calibratedControllerId = null
                        false
                    } else {
                        if (controllerId != calibratedControllerId) {
                            calibratedControllerId = controllerId
                            controllerCalibrationUntil = elapsedSeconds + CONTROLLER_CALIBRATION_SECONDS
                        }
                        val isCalibrating = elapsedSeconds <= controllerCalibrationUntil
                        if (isCalibrating || controllerNeutral == null) {
                            controllerNeutral = captureControllerNeutral(controllerState, controllerId)
                        }
                        isCalibrating
                    }

                    val leftStickX = normalizedControllerAxis(
                        controllerAxisValue(
                            controllerState,
                            controllerId,
                            controllerNeutral,
                            LEFT_STICK_X_AXIS,
                            isControllerCalibrating
                        )
                    )
                    val leftStickY = -normalizedControllerAxis(
                        controllerAxisValue(
                            controllerState,
                            controllerId,
                            controllerNeutral,
                            LEFT_STICK_Y_AXIS,
                            isControllerCalibrating
                        )
                    )
                    val rightStickX = normalizedControllerAxis(
                        controllerAxisValue(
                            controllerState,
                            controllerId,
                            controllerNeutral,
                            RIGHT_STICK_X_AXIS,
                            isControllerCalibrating
                        )
                    )
                    val rightStickY = -normalizedControllerAxis(
                        controllerAxisValue(
                            controllerState,
                            controllerId,
                            controllerNeutral,
                            RIGHT_STICK_Y_AXIS,
                            isControllerCalibrating
                        )
                    )

                    val moveRightInput = (
                        axis(keyboardState.isDPressed(), keyboardState.isAPressed()) +
                            leftStickX
                        ).coerceIn(-1.0, 1.0)
                    val moveForwardInput = (
                        axis(keyboardState.isWPressed(), keyboardState.isSPressed()) +
                            leftStickY
                        ).coerceIn(-1.0, 1.0)
                    val lookX = (
                        axis(keyboardState.isRightPressed(), keyboardState.isLeftPressed()) +
                            -rightStickX
                        ).coerceIn(-1.0, 1.0)
                    val lookY = (
                        axis(keyboardState.isUpPressed(), keyboardState.isDownPressed()) +
                            rightStickY
                        ).coerceIn(-1.0, 1.0)

                    val lookSmoothing = smoothingFactor(CAMERA_LOOK_SMOOTHING, deltaSeconds)
                    cameraLookX = lerp(cameraLookX, lookX, lookSmoothing)
                    cameraLookY = lerp(cameraLookY, lookY, lookSmoothing)
                    cameraYaw += cameraLookX * CAMERA_YAW_SPEED * deltaSeconds
                    cameraPitch = (cameraPitch + cameraLookY * CAMERA_PITCH_SPEED * deltaSeconds)
                        .coerceIn(CAMERA_MIN_PITCH, CAMERA_MAX_PITCH)

                    val cameraDistancePressed = keyboardState.isTPressed() ||
                        controllerState.isButtonPressed(Buttons.TRIANGLE)
                    if (cameraDistancePressed && !wasCameraDistancePressed) {
                        cameraDistanceIndex = (cameraDistanceIndex + 1) % CAMERA_DISTANCES.size
                    }
                    wasCameraDistancePressed = cameraDistancePressed

                    val moveDirection = movementDirection(moveRightInput, moveForwardInput, cameraYaw)
                    val moveLength = horizontalLength(moveDirection)
                    val moveSpeed = PLAYER_MOVE_SPEED * deltaSeconds
                    if (moveLength > 0.02) {
                        val moveScale = moveSpeed / moveLength.coerceAtLeast(1.0)
                        val currentGroundY = terrain.playerYAt(
                            x = player.x,
                            z = player.z,
                            maxPlayerY = player.y + GROUND_CONTACT_EPSILON
                        ) ?: PLAYER_HALF_HEIGHT
                        val isGroundedForMove = player.y <= currentGroundY + GROUND_CONTACT_EPSILON &&
                            playerVelocityY <= 0.1
                        player = movePlayerOnTerrain(
                            player = player,
                            deltaX = moveDirection.x * moveScale,
                            deltaZ = moveDirection.z * moveScale,
                            terrain = terrain,
                            isGrounded = isGroundedForMove
                        )
                        playerYaw = atan2(-moveDirection.x, -moveDirection.z)
                    }

                    val jumpPressed = keyboardState.isSpacePressed() ||
                        keyboardState.isXPressed() ||
                        controllerState.isButtonPressed(Buttons.B)
                    val groundYBeforeJump = terrain.playerYAt(
                        x = player.x,
                        z = player.z,
                        maxPlayerY = player.y + GROUND_CONTACT_EPSILON
                    ) ?: PLAYER_HALF_HEIGHT
                    val isGrounded = player.y <= groundYBeforeJump + GROUND_CONTACT_EPSILON && playerVelocityY <= 0.1
                    if (jumpPressed && !wasJumpPressed && isGrounded) {
                        playerVelocityY = PLAYER_JUMP_VELOCITY
                    }
                    wasJumpPressed = jumpPressed
                    val previousPlayerY = player.y
                    playerVelocityY -= PLAYER_GRAVITY * deltaSeconds
                    player = Vec3(player.x, player.y + playerVelocityY * deltaSeconds, player.z)
                    val groundYAfterGravity = terrain.playerYAt(
                        x = player.x,
                        z = player.z,
                        maxPlayerY = max(previousPlayerY, player.y) + GROUND_CONTACT_EPSILON
                    ) ?: PLAYER_HALF_HEIGHT
                    if (playerVelocityY <= 0.0 && player.y < groundYAfterGravity) {
                        player = Vec3(player.x, groundYAfterGravity, player.z)
                        playerVelocityY = 0.0
                    }

                    val firePressed = keyboardState.isFPressed() ||
                        controllerState.isButtonPressed(Buttons.Y) ||
                        controllerState.isButtonPressed(Buttons.SQUARE)
                    if (firePressed && elapsedSeconds >= nextFireAt) {
                        val forward = forwardForYaw(playerYaw, 1.0)
                        val origin = Vec3(
                            player.x + forward.x * 0.58,
                            player.y + 0.48,
                            player.z + forward.z * 0.58
                        )
                        projectiles += Projectile(
                            position = origin,
                            velocity = Vec3(forward.x * 8.5, 0.0, forward.z * 8.5),
                            lifetime = 1.2
                        )
                        nextFireAt = elapsedSeconds + 0.2
                    }
                    for (index in projectiles.indices) {
                        val projectile = projectiles[index]
                        projectiles[index] = projectile.copy(
                            position = add(projectile.position, scale(projectile.velocity, deltaSeconds)),
                            lifetime = projectile.lifetime - deltaSeconds
                        )
                    }
                    projectiles.removeAll { it.lifetime <= 0.0 }

                    cameraFocus = lerp(cameraFocus, player, smoothingFactor(CAMERA_FOLLOW_SMOOTHING, deltaSeconds))
                    val camera = createThirdPersonCamera(
                        player = cameraFocus,
                        cameraYaw = cameraYaw,
                        cameraPitch = cameraPitch,
                        distance = CAMERA_DISTANCES[cameraDistanceIndex]
                    )

                    render(0.035f, 0.05f, 0.08f, 1f, enableDepth = true) { frame ->
                        litRenderer.draw(
                            frame = frame,
                            mesh = world,
                            transform = Transform3D(),
                            camera = camera,
                            light = light
                        )
                        litRenderer.draw(
                            frame = frame,
                            mesh = samus,
                            transform = Transform3D(
                                position = player,
                                rotation = Vec3(0.0, playerYaw + SAMUS_MODEL_YAW_OFFSET, 0.0)
                            ),
                            camera = camera,
                            light = light
                        )
                        projectiles.forEach { projectile ->
                            meshRenderer.draw(
                                frame = frame,
                                mesh = projectileMesh,
                                transform = Transform3D(position = projectile.position),
                                camera = camera
                            )
                        }
                    }

                    mouse.mouse.clearFrameState()
                    SDL_Delay(16u)
                }
            } finally {
                litRenderer.cleanup()
                meshRenderer.cleanup()
                projectileMesh.cleanup()
                samus.cleanup()
                world.cleanup()
            }
        }
    }
}

private data class Projectile(
    val position: Vec3,
    val velocity: Vec3,
    val lifetime: Double
)

private data class FollowCamera(
    val eye: Vec3,
    val target: Vec3
) : Camera3D {
    override fun viewProjection(aspect: Float): Mat4 {
        return Mat4.perspective(
            fovDegrees = 58f,
            aspect = aspect,
            near = 0.1f,
            far = 240f
        ) * Mat4.lookAt(eye, target)
    }
}

private class TerrainCollision private constructor(
    private val triangles: List<TerrainTriangle>
) {
    fun playerYAt(
        x: Double,
        z: Double,
        maxPlayerY: Double? = null
    ): Double? {
        return heightAt(
            x = x,
            z = z,
            maxHeight = maxPlayerY?.minus(PLAYER_HALF_HEIGHT)
        )?.plus(PLAYER_HALF_HEIGHT)
    }

    private fun heightAt(
        x: Double,
        z: Double,
        maxHeight: Double?
    ): Double? {
        var bestHeight: Double? = null
        triangles.forEach { triangle ->
            val height = triangle.heightAt(x, z) ?: return@forEach
            if (maxHeight != null && height > maxHeight) {
                return@forEach
            }
            if (bestHeight == null || height > bestHeight!!) {
                bestHeight = height
            }
        }
        return bestHeight
    }

    companion object {
        fun fromVertices(vertices: List<LitVertex3D>): TerrainCollision {
            val triangles = mutableListOf<TerrainTriangle>()
            for (index in 0 until vertices.size - 2 step 3) {
                triangles += TerrainTriangle(
                    a = vertices[index].position,
                    b = vertices[index + 1].position,
                    c = vertices[index + 2].position
                )
            }
            return TerrainCollision(triangles)
        }
    }
}

private data class TerrainTriangle(
    val a: Vec3,
    val b: Vec3,
    val c: Vec3
) {
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

private fun movePlayerOnTerrain(
    player: Vec3,
    deltaX: Double,
    deltaZ: Double,
    terrain: TerrainCollision,
    isGrounded: Boolean
): Vec3 {
    fun candidate(
        xDelta: Double,
        zDelta: Double
    ): Vec3? {
        val nextX = player.x + xDelta
        val nextZ = player.z + zDelta
        val nextGroundY = terrain.playerYAt(
            x = nextX,
            z = nextZ,
            maxPlayerY = player.y + if (isGrounded) MAX_STEP_UP else GROUND_CONTACT_EPSILON
        ) ?: return null
        if (isGrounded && nextGroundY - player.y > MAX_STEP_UP) {
            return null
        }
        return Vec3(
            x = nextX,
            y = if (isGrounded) nextGroundY else player.y,
            z = nextZ
        )
    }

    return candidate(deltaX, deltaZ)
        ?: candidate(deltaX, 0.0)
        ?: candidate(0.0, deltaZ)
        ?: player
}

private fun createThirdPersonCamera(
    player: Vec3,
    cameraYaw: Double,
    cameraPitch: Double,
    distance: Double
): FollowCamera {
    val forward = forwardForYaw(cameraYaw, 1.0)
    val horizontalDistance = cos(cameraPitch) * distance
    val eye = Vec3(
        x = player.x - forward.x * horizontalDistance,
        y = player.y + 0.95 + sin(cameraPitch) * distance,
        z = player.z - forward.z * horizontalDistance
    )
    val target = Vec3(player.x, player.y + 0.62, player.z)
    return FollowCamera(eye = eye, target = target)
}

private fun movementDirection(
    inputRight: Double,
    inputForward: Double,
    cameraYaw: Double
): Vec3 {
    val forward = forwardForYaw(cameraYaw, 1.0)
    val right = rightForYaw(cameraYaw, 1.0)
    return Vec3(
        x = right.x * inputRight + forward.x * inputForward,
        y = 0.0,
        z = right.z * inputRight + forward.z * inputForward
    )
}

private fun forwardForYaw(
    yaw: Double,
    length: Double
): Vec3 {
    return Vec3(-sin(yaw) * length, 0.0, -cos(yaw) * length)
}

private fun rightForYaw(
    yaw: Double,
    length: Double
): Vec3 {
    return Vec3(cos(yaw) * length, 0.0, -sin(yaw) * length)
}

private fun horizontalLength(value: Vec3): Double {
    return sqrt(value.x * value.x + value.z * value.z)
}

private fun smoothingFactor(
    speed: Double,
    deltaSeconds: Double
): Double {
    return (speed * deltaSeconds).coerceIn(0.0, 1.0)
}

private fun lerp(
    from: Double,
    to: Double,
    amount: Double
): Double {
    return from + (to - from) * amount
}

private fun lerp(
    from: Vec3,
    to: Vec3,
    amount: Double
): Vec3 {
    return Vec3(
        x = from.x + (to.x - from.x) * amount,
        y = from.y + (to.y - from.y) * amount,
        z = from.z + (to.z - from.z) * amount
    )
}

private fun add(
    a: Vec3,
    b: Vec3
): Vec3 {
    return Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
}

private fun scale(
    value: Vec3,
    scalar: Double
): Vec3 {
    return Vec3(value.x * scalar, value.y * scalar, value.z * scalar)
}

private fun axis(positivePressed: Boolean, negativePressed: Boolean): Double {
    return when {
        positivePressed && !negativePressed -> 1.0
        negativePressed && !positivePressed -> -1.0
        else -> 0.0
    }
}

private fun controllerAxisValue(
    controller: ControllerInputEventSubscriber,
    controllerId: UInt?,
    neutral: FloatArray?,
    axisIndex: Int,
    isCalibrating: Boolean
): Float {
    if (isCalibrating) {
        return 0f
    }

    val rawValue = controllerId?.let { controller.getAxisValue(it, axisIndex) } ?: 0f
    val neutralValue = neutral?.getOrNull(axisIndex) ?: 0f
    return (rawValue - neutralValue).coerceIn(-1f, 1f)
}

private fun captureControllerNeutral(
    controller: ControllerInputEventSubscriber,
    controllerId: UInt
): FloatArray {
    return FloatArray(CONTROLLER_AXIS_COUNT) { axisIndex ->
        controller.getAxisValue(controllerId, axisIndex)
    }
}

private fun normalizedControllerAxis(value: Float): Double {
    val raw = value.toDouble().coerceIn(-1.0, 1.0)
    val magnitude = abs(raw)
    if (magnitude < CONTROLLER_DEADZONE) {
        return 0.0
    }

    val normalized = ((magnitude - CONTROLLER_DEADZONE) / (1.0 - CONTROLLER_DEADZONE)).coerceIn(0.0, 1.0)
    return if (raw < 0.0) -normalized else normalized
}

private fun resolveMetroidAsset(relativePath: String): String {
    val builtAssetPath = File.resolveAssetPath("assets/$relativePath")
    if (File.isExist(builtAssetPath)) {
        return builtAssetPath
    }

    val sourceAssetPath = File.resolveAssetPath("games/metroid-3d/assets/$relativePath")
    if (File.isExist(sourceAssetPath)) {
        return sourceAssetPath
    }

    return builtAssetPath
}
