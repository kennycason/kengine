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
import com.kengine.three.GlbAnimationClipInfo
import com.kengine.three.GlbMeshLoadOptions
import com.kengine.three.GlbMeshLoader
import com.kengine.three.GpuContext
import com.kengine.three.LitMeshRenderer3D
import com.kengine.three.LitVertex3D
import com.kengine.three.Mat4
import com.kengine.three.TexturedLitMeshRenderer3D
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
private const val CONTROLLER_RAW_RELEASE_DEADZONE = 0.08f
private const val CONTROLLER_CALIBRATION_SECONDS = 0.2
private const val WORLD_TARGET_SIZE = 105.0
private const val PLAYER_HALF_HEIGHT = 0.82
private const val PLAYER_WALK_SPEED = 5.4
private const val PLAYER_RUN_SPEED = 8.2
private const val PLAYER_JUMP_VELOCITY = 9.6
private const val PLAYER_GRAVITY = 17.5
private const val MARIO_MODEL_YAW_OFFSET = 3.141592653589793
private const val GOOMBA_MODEL_YAW_OFFSET = 3.141592653589793
private const val BOWSER_MODEL_YAW_OFFSET = 3.141592653589793
private const val GROUND_CONTACT_EPSILON = 0.05
private const val MAX_STEP_UP = 0.72
private const val MOVEMENT_INPUT_EPSILON = 0.08
private const val LOOK_INPUT_EPSILON = 0.04
private const val CAMERA_STOP_EPSILON = 0.01
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
        title = "Kengine - Mario 3D",
        width = WINDOW_WIDTH,
        height = WINDOW_HEIGHT,
        logLevel = Logger.Level.INFO,
        renderBackend = RenderBackend.SDL_GPU_3D
    ) {
        useContext(GpuContext.create(sdl), cleanup = true) {
            val worldVertices = GlbMeshLoader.loadLitVertices(
                assetPath = resolveMarioAsset("models/Super Mario 64 Bob-Omb Battlefield.glb"),
                options = GlbMeshLoadOptions(
                    targetSize = WORLD_TARGET_SIZE,
                    defaultColor = Color.fromHex("7fbf72")
                )
            )
            val world = GlbMeshLoader.loadTexturedLit(
                gpu = this,
                assetPath = resolveMarioAsset("models/Super Mario 64 Bob-Omb Battlefield.glb"),
                options = GlbMeshLoadOptions(
                    targetSize = WORLD_TARGET_SIZE,
                    defaultColor = Color.fromHex("ffffff")
                )
            )
            val terrain = TerrainCollision.fromVertices(worldVertices)
            val mario = GlbMeshLoader.loadSkinnedTexturedLit(
                gpu = this,
                assetPath = resolveMarioAsset("models/Mario64Animated.glb"),
                options = GlbMeshLoadOptions(
                    targetSize = 1.58,
                    defaultColor = Color.fromHex("ffffff")
                )
            )
            val marioClips = MarioAnimationClips.from(mario.clips)
            val goomba = GlbMeshLoader.loadAnimatedLit(
                gpu = this,
                assetPath = resolveMarioAsset("models/Animated Goomba Super Mario Bros.glb"),
                options = GlbMeshLoadOptions(
                    targetSize = 0.92,
                    defaultColor = Color.fromHex("a86432")
                )
            )
            val bowser = GlbMeshLoader.loadTexturedLit(
                gpu = this,
                assetPath = resolveMarioAsset("models/Super Mario 64 Bowser.glb"),
                options = GlbMeshLoadOptions(
                    targetSize = 4.4,
                    defaultColor = Color.fromHex("ffffff")
                )
            )
            val litRenderer = LitMeshRenderer3D(this)
            val texturedRenderer = TexturedLitMeshRenderer3D(this)
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
            var marioAnimationState = MarioAnimationState.IDLE
            var marioAnimationTime = 0.0
            val goombas = createGoombas(terrain)
            val bowserAnchor = findHighestGroundRegionCenter(terrain)
            var controllerNeutral: FloatArray? = null
            var calibratedControllerId: UInt? = null
            var controllerCalibrationUntil = 0.0
            var wasJumpPressed = false
            var wasGrounded = true
            var landingAnimationUntil = 0.0
            var wasCameraDistancePressed = false
            var cameraDistanceIndex = 0
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
                            controllerNeutral = captureControllerNeutral(controllerState, controllerId)
                            controllerCalibrationUntil = elapsedSeconds + CONTROLLER_CALIBRATION_SECONDS
                        }
                        val isCalibrating = elapsedSeconds <= controllerCalibrationUntil
                        if (controllerNeutral == null) {
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

                    val moveRightInput = snapInput(
                        axis(keyboardState.isDPressed(), keyboardState.isAPressed()) +
                            leftStickX
                    )
                    val moveForwardInput = snapInput(
                        axis(keyboardState.isWPressed(), keyboardState.isSPressed()) +
                            leftStickY
                    )
                    val lookX = snapInput(
                        axis(keyboardState.isRightPressed(), keyboardState.isLeftPressed()) +
                            -rightStickX,
                        LOOK_INPUT_EPSILON
                    )
                    val lookY = snapInput(
                        axis(keyboardState.isUpPressed(), keyboardState.isDownPressed()) +
                            rightStickY,
                        LOOK_INPUT_EPSILON
                    )

                    val lookSmoothing = smoothingFactor(CAMERA_LOOK_SMOOTHING, deltaSeconds)
                    cameraLookX = smoothInput(cameraLookX, lookX, lookSmoothing)
                    cameraLookY = smoothInput(cameraLookY, lookY, lookSmoothing)
                    cameraYaw += cameraLookX * CAMERA_YAW_SPEED * deltaSeconds
                    cameraPitch = (cameraPitch + cameraLookY * CAMERA_PITCH_SPEED * deltaSeconds)
                        .coerceIn(CAMERA_MIN_PITCH, CAMERA_MAX_PITCH)

                    val cameraDistancePressed = keyboardState.isTPressed() ||
                        controllerState.isButtonPressed(Buttons.TRIANGLE)
                    if (cameraDistancePressed && !wasCameraDistancePressed) {
                        cameraDistanceIndex = (cameraDistanceIndex + 1) % CAMERA_DISTANCES.size
                    }
                    wasCameraDistancePressed = cameraDistancePressed

                    val runPressed = keyboardState.isLShiftPressed() ||
                        keyboardState.isRShiftPressed() ||
                        keyboardState.isBPressed() ||
                        controllerState.isButtonPressed(Buttons.SQUARE) ||
                        controllerState.isButtonPressed(Buttons.Y)
                    val moveDirection = movementDirection(moveRightInput, moveForwardInput, cameraYaw)
                    val moveLength = horizontalLength(moveDirection)
                    val isMoving = moveLength > MOVEMENT_INPUT_EPSILON
                    val moveSpeed = (if (runPressed) PLAYER_RUN_SPEED else PLAYER_WALK_SPEED) * deltaSeconds
                    if (isMoving) {
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
                    val isGroundedAfterGravity = player.y <= groundYAfterGravity + GROUND_CONTACT_EPSILON &&
                        playerVelocityY <= 0.1
                    if (!wasGrounded && isGroundedAfterGravity) {
                        landingAnimationUntil = elapsedSeconds + 0.22
                    }
                    wasGrounded = isGroundedAfterGravity

                    val nextMarioAnimationState = when {
                        !isGroundedAfterGravity && playerVelocityY > 0.0 -> MarioAnimationState.JUMP
                        !isGroundedAfterGravity -> MarioAnimationState.FALL
                        elapsedSeconds < landingAnimationUntil -> MarioAnimationState.LAND
                        isMoving && runPressed -> MarioAnimationState.RUN
                        isMoving -> MarioAnimationState.WALK
                        else -> MarioAnimationState.IDLE
                    }
                    if (nextMarioAnimationState != marioAnimationState) {
                        marioAnimationState = nextMarioAnimationState
                        marioAnimationTime = 0.0
                    } else {
                        marioAnimationTime += deltaSeconds * marioAnimationState.playbackSpeed
                    }
                    mario.updatePose(
                        clipIndex = marioClips.indexFor(marioAnimationState),
                        timeSeconds = marioAnimationTime
                    )

                    updateGoombas(goombas, terrain, deltaSeconds)
                    cameraFocus = lerp(cameraFocus, player, smoothingFactor(CAMERA_FOLLOW_SMOOTHING, deltaSeconds))
                    val camera = createThirdPersonCamera(
                        player = cameraFocus,
                        cameraYaw = cameraYaw,
                        cameraPitch = cameraPitch,
                        distance = CAMERA_DISTANCES[cameraDistanceIndex]
                    )

                    render(0.47f, 0.67f, 0.94f, 1f, enableDepth = true) { frame ->
                        world.draw(
                            renderer = texturedRenderer,
                            frame = frame,
                            transform = Transform3D(),
                            camera = camera,
                            light = light
                        )
                        goombas.forEach { enemy ->
                            goomba.draw(
                                renderer = litRenderer,
                                frame = frame,
                                transform = Transform3D(
                                    position = enemy.position,
                                    rotation = Vec3(0.0, enemy.yaw + GOOMBA_MODEL_YAW_OFFSET, 0.0)
                                ),
                                camera = camera,
                                light = light,
                                timeSeconds = elapsedSeconds * 1.4 + enemy.angle
                            )
                        }
                        bowser.draw(
                            renderer = texturedRenderer,
                            frame = frame,
                            transform = Transform3D(
                                position = Vec3(
                                    bowserAnchor.x,
                                    bowserAnchor.y + 0.18 + sin(elapsedSeconds * 1.1) * 0.1,
                                    bowserAnchor.z
                                ),
                                rotation = Vec3(0.0, elapsedSeconds * -0.22 + BOWSER_MODEL_YAW_OFFSET, 0.0)
                            ),
                            camera = camera,
                            light = light
                        )
                        mario.draw(
                            renderer = texturedRenderer,
                            frame = frame,
                            transform = Transform3D(
                                position = Vec3(player.x, player.y - PLAYER_HALF_HEIGHT, player.z),
                                rotation = Vec3(0.0, playerYaw + MARIO_MODEL_YAW_OFFSET, 0.0)
                            ),
                            camera = camera,
                            light = light
                        )
                    }

                    mouse.mouse.clearFrameState()
                    SDL_Delay(16u)
                }
            } finally {
                texturedRenderer.cleanup()
                litRenderer.cleanup()
                bowser.cleanup()
                goomba.cleanup()
                mario.cleanup()
                world.cleanup()
            }
        }
    }
}

private data class RoamingEnemy(
    val center: Vec3,
    val radius: Double,
    val speed: Double,
    var angle: Double,
    var position: Vec3,
    var yaw: Double
)

private enum class MarioAnimationState(
    val playbackSpeed: Double
) {
    IDLE(1.0),
    WALK(1.05),
    RUN(1.18),
    JUMP(1.0),
    FALL(1.0),
    LAND(1.0)
}

private data class MarioAnimationClips(
    val idle: Int,
    val walk: Int,
    val run: Int,
    val jump: Int,
    val fall: Int,
    val land: Int
) {
    fun indexFor(state: MarioAnimationState): Int {
        return when (state) {
            MarioAnimationState.IDLE -> idle
            MarioAnimationState.WALK -> walk
            MarioAnimationState.RUN -> run
            MarioAnimationState.JUMP -> jump
            MarioAnimationState.FALL -> fall
            MarioAnimationState.LAND -> land
        }
    }

    companion object {
        fun from(clips: List<GlbAnimationClipInfo>): MarioAnimationClips {
            fun clip(name: String): Int {
                val index = clips.indexOfFirst { it.name == name }
                require(index >= 0) {
                    "Mario animated GLB is missing required clip '$name'. Available: ${clips.joinToString { it.name }}"
                }
                return index
            }

            return MarioAnimationClips(
                idle = clip("Armature|AreaWait64"),
                walk = clip("Armature|Walk"),
                run = clip("Armature|Run"),
                jump = clip("Armature|Jump"),
                fall = clip("Armature|Fall"),
                land = clip("Armature|Land")
            )
        }
    }
}

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
        return groundYAt(
            x = x,
            z = z,
            maxGroundY = maxPlayerY?.minus(PLAYER_HALF_HEIGHT)
        )?.plus(PLAYER_HALF_HEIGHT)
    }

    fun groundYAt(
        x: Double,
        z: Double,
        maxGroundY: Double? = null
    ): Double? {
        var bestHeight: Double? = null
        triangles.forEach { triangle ->
            val height = triangle.heightAt(x, z) ?: return@forEach
            if (maxGroundY != null && height > maxGroundY) {
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

private fun createGoombas(terrain: TerrainCollision): MutableList<RoamingEnemy> {
    return mutableListOf(
        createGoomba(terrain, centerX = -8.0, centerZ = -9.0, radius = 3.4, speed = 0.85, angle = 0.0),
        createGoomba(terrain, centerX = 7.5, centerZ = -12.0, radius = 4.2, speed = 0.66, angle = 1.7),
        createGoomba(terrain, centerX = -17.0, centerZ = 7.0, radius = 4.8, speed = 0.72, angle = 3.0),
        createGoomba(terrain, centerX = 17.0, centerZ = 10.0, radius = 4.0, speed = 0.93, angle = 4.4),
        createGoomba(terrain, centerX = 0.0, centerZ = 18.0, radius = 5.1, speed = 0.58, angle = 2.2)
    )
}

private fun createGoomba(
    terrain: TerrainCollision,
    centerX: Double,
    centerZ: Double,
    radius: Double,
    speed: Double,
    angle: Double
): RoamingEnemy {
    val position = surfacePosition(
        terrain = terrain,
        x = centerX + cos(angle) * radius,
        z = centerZ + sin(angle) * radius
    )
    return RoamingEnemy(
        center = Vec3(centerX, 0.0, centerZ),
        radius = radius,
        speed = speed,
        angle = angle,
        position = position,
        yaw = angle + 1.5707963267948966
    )
}

private fun updateGoombas(
    enemies: List<RoamingEnemy>,
    terrain: TerrainCollision,
    deltaSeconds: Double
) {
    enemies.forEach { enemy ->
        val previous = enemy.position
        enemy.angle += enemy.speed * deltaSeconds
        val next = surfacePosition(
            terrain = terrain,
            x = enemy.center.x + cos(enemy.angle) * enemy.radius,
            z = enemy.center.z + sin(enemy.angle) * enemy.radius
        )
        enemy.position = next

        val deltaX = next.x - previous.x
        val deltaZ = next.z - previous.z
        if (deltaX * deltaX + deltaZ * deltaZ > 0.000001) {
            enemy.yaw = atan2(-deltaX, -deltaZ)
        }
    }
}

private fun surfacePosition(
    terrain: TerrainCollision,
    x: Double,
    z: Double,
    fallbackY: Double = 0.0
): Vec3 {
    return Vec3(x, terrain.groundYAt(x, z) ?: fallbackY, z)
}

private fun findHighestGroundRegionCenter(terrain: TerrainCollision): Vec3 {
    val samples = mutableListOf<Vec3>()
    var highestY = Double.NEGATIVE_INFINITY
    var x = -42.0
    while (x <= 42.0) {
        var z = -42.0
        while (z <= 42.0) {
            terrain.groundYAt(x, z)?.let { y ->
                val sample = Vec3(x, y, z)
                samples += sample
                if (y > highestY) {
                    highestY = y
                }
            }
            z += 1.25
        }
        x += 1.25
    }

    if (samples.isEmpty()) {
        return Vec3(0.0, 0.0, 0.0)
    }

    val summitSamples = samples.filter { it.y >= highestY - 1.2 }.ifEmpty {
        samples.sortedByDescending { it.y }.take(6)
    }
    val centerX = summitSamples.map { it.x }.average()
    val centerZ = summitSamples.map { it.z }.average()
    return surfacePosition(
        terrain = terrain,
        x = centerX,
        z = centerZ,
        fallbackY = highestY
    )
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

private fun snapInput(
    value: Double,
    epsilon: Double = MOVEMENT_INPUT_EPSILON
): Double {
    val clamped = value.coerceIn(-1.0, 1.0)
    return if (abs(clamped) < epsilon) 0.0 else clamped
}

private fun smoothInput(
    current: Double,
    target: Double,
    amount: Double
): Double {
    val next = lerp(current, target, amount)
    return if (target == 0.0 && abs(next) < CAMERA_STOP_EPSILON) 0.0 else next
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
    if (abs(rawValue) < CONTROLLER_RAW_RELEASE_DEADZONE) {
        return 0f
    }

    val neutralValue = neutral?.getOrNull(axisIndex) ?: 0f
    val adjustedValue = (rawValue - neutralValue).coerceIn(-1f, 1f)
    return if (abs(adjustedValue) < CONTROLLER_RAW_RELEASE_DEADZONE) 0f else adjustedValue
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

private fun resolveMarioAsset(relativePath: String): String {
    val builtAssetPath = File.resolveAssetPath("assets/$relativePath")
    if (File.isExist(builtAssetPath)) {
        return builtAssetPath
    }

    val sourceAssetPath = File.resolveAssetPath("games/mario-3d/assets/$relativePath")
    if (File.isExist(sourceAssetPath)) {
        return sourceAssetPath
    }

    return builtAssetPath
}
