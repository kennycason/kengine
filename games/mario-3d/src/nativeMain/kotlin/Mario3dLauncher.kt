import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.hooks.context.useContext
import com.kengine.input.digitalAxis
import com.kengine.input.snapAxis
import com.kengine.input.controller.CalibratedControllerAxes
import com.kengine.input.controller.ControllerInputEventSubscriber
import com.kengine.input.controller.ControllerAxisInputSettings
import com.kengine.input.controller.controls.Buttons
import com.kengine.input.keyboard.KeyboardInputEventSubscriber
import com.kengine.log.Logger
import com.kengine.math.Vec3
import com.kengine.sdl.RenderBackend
import com.kengine.three.AnimationClipMap3D
import com.kengine.three.AnimationClipReference3D
import com.kengine.three.AnimationClipSet3D
import com.kengine.three.AnimationPose3D
import com.kengine.three.AnimationStateController3D
import com.kengine.three.AnimatedModelInstance3D
import com.kengine.three.AnimatedModelAsset3D
import com.kengine.three.AnimatedModelSourceCache3D
import com.kengine.three.Camera3D
import com.kengine.three.Collision3D
import com.kengine.three.DebugRenderer3D
import com.kengine.three.DirectionalLight3D
import com.kengine.three.GpuContext
import com.kengine.three.GpuFrame
import com.kengine.three.GpuMesh
import com.kengine.three.GpuResourceScope3D
import com.kengine.three.GpuTextureCache3D
import com.kengine.three.KinematicCharacterController3D
import com.kengine.three.KinematicCharacterControllerSettings3D
import com.kengine.three.KinematicCharacterState3D
import com.kengine.three.LoadedModelAssetBundle3D
import com.kengine.three.ModelAsset3D
import com.kengine.three.ModelAssetBundle3D
import com.kengine.three.ModelAssetLoader3D
import com.kengine.three.ModelAssetPathResolver3D
import com.kengine.three.ModelLoadOptions3D
import com.kengine.three.ModelSourceCache3D
import com.kengine.three.Node3D
import com.kengine.three.Scene3D
import com.kengine.three.SceneMesh3D
import com.kengine.three.SceneModel3D
import com.kengine.three.SceneRenderer3D
import com.kengine.three.TerrainActorController3D
import com.kengine.three.TerrainActorControllerSettings3D
import com.kengine.three.TerrainMeshCollider3D
import com.kengine.three.SphereCollider3D
import com.kengine.three.ThirdPersonCameraController3D
import com.kengine.three.ThirdPersonCameraInput3D
import com.kengine.three.ThirdPersonCameraSettings3D
import com.kengine.three.Transform3D
import com.kengine.three.addAnimatedModelAssetNode
import com.kengine.three.addModelAssetNode
import com.kengine.three.createStaticCollider
import com.kengine.three.createTerrainCollider
import com.kengine.three.forwardForYaw
import com.kengine.three.horizontalDistance
import com.kengine.three.horizontalLength
import com.kengine.three.horizontalVelocityForMovement
import com.kengine.three.lerp
import com.kengine.three.moveAngleToward
import com.kengine.three.setPose
import com.kengine.three.shortestAngleDelta
import com.kengine.three.squaredHorizontalDistance
import com.kengine.three.ui.GpuUiAlign3D
import com.kengine.three.ui.GpuUiContext3D
import com.kengine.three.ui.GpuUiRenderer3D
import com.kengine.three.ui.GpuUiView3D
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import sdl3.SDL_GetTicks
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val WINDOW_WIDTH = 960
private const val WINDOW_HEIGHT = 540
private const val LEFT_STICK_X_AXIS = 0
private const val LEFT_STICK_Y_AXIS = 1
private const val RIGHT_STICK_X_AXIS = 2
private const val RIGHT_STICK_Y_AXIS = 3
private const val WORLD_TARGET_SIZE = 105.0
private const val PLAYER_HALF_HEIGHT = 0.82
private const val PLAYER_WALK_SPEED = 5.4
private const val PLAYER_RUN_SPEED = 8.2
private const val PLAYER_CROUCH_WALK_SPEED = 2.8
private const val PLAYER_JUMP_VELOCITY = 15.4
private const val PLAYER_DOUBLE_JUMP_VELOCITY = 17.6
private const val PLAYER_BACKFLIP_JUMP_VELOCITY = 20.5
private const val PLAYER_BACKFLIP_BACK_SPEED = 4.6
private const val PLAYER_LONG_JUMP_VELOCITY = 13.2
private const val PLAYER_LONG_JUMP_SPEED = 13.8
private const val PLAYER_GROUND_POUND_VELOCITY = -32.0
private const val PLAYER_GROUND_POUND_ANIMATION_SECONDS = 0.7
private const val PLAYER_JUMP_GRAVITY = 42.0
private const val PLAYER_FALL_GRAVITY = 48.0
private const val PLAYER_TERMINAL_FALL_SPEED = -28.0
private const val PLAYER_MAX_STEP_DOWN = 1.4
private const val PLAYER_COLLISION_RADIUS = 0.38
private const val PLAYER_STOMP_BOUNCE_VELOCITY = 10.8
private const val PLAYER_HIGH_STOMP_BOUNCE_VELOCITY = 16.2
private const val PLAYER_BUMP_BACK_DISTANCE = 0.58
private const val PLAYER_HURT_BOUNCE_VELOCITY = 8.6
private const val PLAYER_HURT_ANIMATION_SECONDS = 0.42
private const val PLAYER_HURT_COOLDOWN_SECONDS = 0.75
private const val MARIO_MAX_HEALTH = 99
private const val GOOMBA_DAMAGE = 6
private const val BOWSER_DAMAGE = 18
private const val ENEMY_MAX_STEP_UP = 0.48
private const val ENEMY_MAX_STEP_DOWN = 0.58
private const val GOOMBA_COLLISION_RADIUS = 0.48
private const val GOOMBA_COLLISION_CENTER_Y = 0.42
private const val GOOMBA_STOMP_MIN_HEIGHT = 0.28
private const val BOWSER_HEALTH = 10
private const val BOWSER_COLLISION_RADIUS = 1.48
private const val BOWSER_COLLISION_CENTER_Y = 1.5
private const val BOWSER_STOMP_MIN_HEIGHT = 1.35
private const val BOWSER_STOMP_BOUNCE_VELOCITY = 13.4
private const val BOWSER_HIGH_STOMP_BOUNCE_VELOCITY = 18.0
private const val BOWSER_BODY_BUMP_DISTANCE = 1.1
private const val BOWSER_AGGRO_RADIUS = 16.0
private const val BOWSER_LEASH_RADIUS = 8.5
private const val BOWSER_RESET_RADIUS = 15.0
private const val BOWSER_FALL_RECOVERY_DROP = 5.0
private const val BOWSER_HOME_STOP_RADIUS = 0.45
private const val BOWSER_WALK_SPEED = 1.35
private const val BOWSER_LUNGE_SPEED = 5.1
private const val BOWSER_GROUNDED_TURN_SPEED = 1.55
private const val BOWSER_AIR_TURN_SPEED = 0.65
private const val BOWSER_ATTACK_MAX_YAW_ERROR = 0.7
private const val BOWSER_JUMP_VELOCITY = 8.8
private const val BOWSER_GRAVITY = 28.0
private const val BOWSER_TERMINAL_FALL_SPEED = -18.0
private const val BOWSER_ATTACK_COOLDOWN_SECONDS = 2.35
private const val BOWSER_HIT_FLASH_SECONDS = 0.32
private const val REGULAR_COIN_COUNT = 100
private const val REGULAR_COIN_PICKUP_RADIUS = 0.82
private const val GOLDEN_COIN_PICKUP_RADIUS = 1.25
private const val COIN_PICKUP_VERTICAL_PADDING = 1.2
private const val REGULAR_COIN_SCATTER_RANGE = 42.0
private const val REGULAR_COIN_SCATTER_GRID_SIZE = 10
private const val REGULAR_COIN_MIN_SPACING = 2.35
private const val REGULAR_COIN_SCATTER_ATTEMPTS = 6000
private const val REGULAR_COIN_SCATTER_SEED = 64
private const val GOLDEN_COIN_COUNT = 5
private const val STAR_FLOAT_HEIGHT = 1.65
private const val STAR_PICKUP_RADIUS = 0.72
private const val MARIO_HUD_FONT_PATH = "assets/fonts/arcade_classic.ttf"
private const val MARIO_HUD_FONT_SIZE = 36f
private const val MARIO_HUD_SHADOW_OFFSET = 3.0
private const val MARIO_HUD_COIN_ICON_BOX_SIZE = 36.0
private const val MARIO_HUD_GOLD_COIN_ICON_SIZE = 36.0
private const val MARIO_HUD_REGULAR_COIN_ICON_SIZE = 18.0
private const val MARIO_MODEL_YAW_OFFSET = 3.141592653589793
private const val GOOMBA_MODEL_YAW_OFFSET = 3.141592653589793
private const val BOWSER_MODEL_YAW_OFFSET = 1.5707963267948966
private const val GROUND_CONTACT_EPSILON = 0.05
private const val MAX_STEP_UP = 0.72
private const val MOVEMENT_INPUT_EPSILON = 0.08
private const val LOOK_INPUT_EPSILON = 0.04
private const val CAMERA_STOP_EPSILON = 0.01
private const val CAMERA_YAW_SPEED = 3.0
private const val CAMERA_PITCH_SPEED = 1.75
private const val CAMERA_ZOOM_SPEED = 3.6
private const val CAMERA_LOOK_SMOOTHING = 18.0
private const val CAMERA_FOLLOW_SMOOTHING = 10.0
private const val CAMERA_MIN_PITCH = -0.08
private const val CAMERA_MAX_PITCH = 0.95
private const val CAMERA_MIN_DISTANCE = 2.8
private const val CAMERA_MAX_DISTANCE = 7.2
private const val REWARD_COIN_FLY_SECONDS = 0.85
private const val REWARD_COIN_HOLD_SECONDS = 1.45
private const val REWARD_COIN_RETURN_SECONDS = 1.0
private const val REWARD_COIN_ARC_HEIGHT = 4.2
private const val REWARD_COIN_CAMERA_Y_OFFSET = 0.85
private val CAMERA_DISTANCES = listOf(3.5, 4.9, 6.3)
private val CONTROLLER_AXIS_SETTINGS = ControllerAxisInputSettings(
    axisCount = 6,
    deadzone = 0.14,
    rawReleaseDeadzone = 0.08f,
    calibrationSeconds = 0.2
)

private object MarioModelAssets {
    val World = ModelAsset3D(
        relativePath = "models/Super Mario 64 Bob-Omb Battlefield.glb",
        options = ModelLoadOptions3D(
            targetSize = WORLD_TARGET_SIZE,
            defaultColor = Color.fromHex("ffffff")
        )
    )
    val Mario = AnimatedModelAsset3D.skinnedTexturedLit(
        relativePath = "models/Mario64Animated.glb",
        options = ModelLoadOptions3D(
            targetSize = 1.58,
            defaultColor = Color.fromHex("ffffff")
        )
    )
    val Goomba = AnimatedModelAsset3D.nodeAnimatedLit(
        relativePath = "models/Animated Goomba Super Mario Bros.glb",
        options = ModelLoadOptions3D(
            targetSize = 0.92,
            defaultColor = Color.fromHex("a86432")
        )
    )
    val Bowser = ModelAsset3D(
        relativePath = "models/Super Mario 64 Bowser.glb",
        options = ModelLoadOptions3D(
            targetSize = 4.4,
            defaultColor = Color.fromHex("ffffff")
        )
    )
    val Bundle = ModelAssetBundle3D(
        models = listOf(World, Bowser),
        animatedModels = listOf(Mario, Goomba)
    )
}

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
            val resources = GpuResourceScope3D()
            val textureCache = resources.track(GpuTextureCache3D(this))
            val sourceCache = ModelSourceCache3D()
            val animatedSourceCache = AnimatedModelSourceCache3D()
            val modelAssets = ModelAssetLoader3D(
                gpu = this,
                resources = resources,
                resolver = ModelAssetPathResolver3D(sourceAssetRoot = "games/mario-3d/assets"),
                textureCache = textureCache,
                sourceCache = sourceCache,
                animatedSourceCache = animatedSourceCache
            )
            val loadedAssets = modelAssets.loadBundle(MarioModelAssets.Bundle)
            val sceneRenderer = resources.track(SceneRenderer3D(this))
            val debugRenderer = resources.track(DebugRenderer3D(this)) { it.cleanup() }
            val hudFont = font.addFont("mario-hud", MARIO_HUD_FONT_PATH, fontSize = MARIO_HUD_FONT_SIZE)
            val hudRenderer = resources.track(GpuUiRenderer3D(this, hudFont))
            val light = DirectionalLight3D(
                direction = Vec3(-0.45, -0.85, -0.32),
                color = Color.fromHex("fff8e6"),
                ambientStrength = 0.42f,
                diffuseStrength = 0.8f
            )
            val scene = Scene3D(light)
            val content = createMarioSceneContent(
                gpu = this,
                resources = resources,
                loadedAssets = loadedAssets,
                scene = scene
            )
            val hud = MarioHudUi(content)
            resources.track(scene)

            val playerState = KinematicCharacterState3D(
                position = Vec3(0.0, content.playerController.actorYAt(0.0, 0.0) ?: PLAYER_HALF_HEIGHT, 0.0),
                isGrounded = true
            )
            val cameraController = ThirdPersonCameraController3D(
                target = playerState.position,
                yawRadians = 0.0,
                pitchRadians = 0.38,
                settings = ThirdPersonCameraSettings3D(
                    yawSpeed = CAMERA_YAW_SPEED,
                    pitchSpeed = CAMERA_PITCH_SPEED,
                    zoomSpeed = CAMERA_ZOOM_SPEED,
                    lookSmoothing = CAMERA_LOOK_SMOOTHING,
                    followSmoothing = CAMERA_FOLLOW_SMOOTHING,
                    minPitch = CAMERA_MIN_PITCH,
                    maxPitch = CAMERA_MAX_PITCH,
                    minDistance = CAMERA_MIN_DISTANCE,
                    maxDistance = CAMERA_MAX_DISTANCE,
                    distanceStops = CAMERA_DISTANCES,
                    inputStopEpsilon = CAMERA_STOP_EPSILON,
                    invertLookY = false
                )
            )
            var playerYaw = 0.0
            val controllerAxes = CalibratedControllerAxes(CONTROLLER_AXIS_SETTINGS)
            var wasJumpPressed = false
            var wasCrouchPressed = false
            var doubleJumpAvailable = false
            var forcedAirAnimationState: MarioAnimationState? = null
            var forcedAirAnimationUntil = 0.0
            var wasGrounded = true
            var landingAnimationUntil = 0.0
            var lastPlayerHitAt = -PLAYER_HURT_COOLDOWN_SECONDS
            var wasDebugPressed = false
            var debugEnabled = false
            var rewardCutscene: RewardCoinCutscene? = null
            var previousTicks = SDL_GetTicks()

            try {
                while (isRunning) {
                    sdlEvent.pollEvents()
                    action.update()

                    val ticks = SDL_GetTicks()
                    val deltaSeconds = ((ticks - previousTicks).toDouble() / 1000.0).coerceIn(0.0, 0.1)
                    previousTicks = ticks
                    val elapsedSeconds = ticks.toDouble() / 1000.0
                    val input = sampleMarioFrameInput(
                        keyboard = keyboard.keyboard,
                        controller = controller.controller,
                        controllerAxes = controllerAxes,
                        elapsedSeconds = elapsedSeconds
                    )

                    if (input.debugPressed && !wasDebugPressed) {
                        debugEnabled = !debugEnabled
                    }
                    wasDebugPressed = input.debugPressed

                    rewardCutscene = updateRewardCoinCutscene(content, rewardCutscene, elapsedSeconds)
                    val gameplayPaused = rewardCutscene != null
                    var player = playerState.position
                    var playerVelocityY = playerState.velocityY
                    var isGroundedAfterGravity = playerState.isGrounded
                    var isMoving = false

                    if (!gameplayPaused) {
                        cameraController.updateInput(
                            input = ThirdPersonCameraInput3D(
                                lookX = input.lookX,
                                lookY = input.lookY,
                                zoom = input.zoom,
                                cycleDistance = input.cycleCameraDistance
                            ),
                            deltaSeconds = deltaSeconds
                        )

                        val moveDirection = cameraController.movementDirection(input.moveRight, input.moveForward)
                        val moveLength = horizontalLength(moveDirection)
                        isMoving = moveLength > MOVEMENT_INPUT_EPSILON
                        val isGroundedBeforeStep = playerState.isGrounded
                        val jumpJustPressed = input.jumpPressed && !wasJumpPressed
                        val crouchJustPressed = input.crouchPressed && !wasCrouchPressed
                        val moveSpeed = when {
                            input.crouchPressed && isGroundedBeforeStep -> PLAYER_CROUCH_WALK_SPEED
                            input.runPressed -> PLAYER_RUN_SPEED
                            else -> PLAYER_WALK_SPEED
                        }
                        val baseHorizontalVelocity = if (isMoving) {
                            horizontalVelocityForMovement(moveDirection, moveLength, moveSpeed)
                        } else {
                            Vec3(0.0, 0.0, 0.0)
                        }
                        if (isMoving) {
                            playerYaw = atan2(-moveDirection.x, -moveDirection.z)
                        }
                        var horizontalVelocity = baseHorizontalVelocity
                        var controllerJumpRequested = jumpJustPressed
                        if (crouchJustPressed && !isGroundedBeforeStep) {
                            horizontalVelocity = Vec3(0.0, 0.0, 0.0)
                            playerState.velocityY = minOf(playerState.velocityY, PLAYER_GROUND_POUND_VELOCITY)
                            playerState.isGrounded = false
                            playerState.ground = null
                            controllerJumpRequested = false
                            doubleJumpAvailable = false
                            forcedAirAnimationState = MarioAnimationState.GROUND_POUND
                            forcedAirAnimationUntil = elapsedSeconds + PLAYER_GROUND_POUND_ANIMATION_SECONDS
                        } else if (jumpJustPressed && input.crouchPressed && isGroundedBeforeStep) {
                            if (isMoving && input.runPressed) {
                                horizontalVelocity = horizontalVelocityForMovement(
                                    moveDirection = moveDirection,
                                    moveLength = moveLength,
                                    speed = PLAYER_LONG_JUMP_SPEED
                                )
                                playerState.velocityY = PLAYER_LONG_JUMP_VELOCITY
                                forcedAirAnimationState = MarioAnimationState.LONG_JUMP
                            } else {
                                val backward = forwardForYaw(playerYaw + MARIO_MODEL_YAW_OFFSET, PLAYER_BACKFLIP_BACK_SPEED)
                                horizontalVelocity = Vec3(backward.x, 0.0, backward.z)
                                playerState.velocityY = PLAYER_BACKFLIP_JUMP_VELOCITY
                                forcedAirAnimationState = MarioAnimationState.BACKFLIP
                            }
                            playerState.isGrounded = false
                            playerState.ground = null
                            controllerJumpRequested = false
                            doubleJumpAvailable = true
                            forcedAirAnimationUntil = elapsedSeconds + 0.5
                        } else if (jumpJustPressed && !isGroundedBeforeStep && doubleJumpAvailable) {
                            playerState.velocityY = maxOf(playerState.velocityY, PLAYER_DOUBLE_JUMP_VELOCITY)
                            playerState.isGrounded = false
                            playerState.ground = null
                            controllerJumpRequested = false
                            doubleJumpAvailable = false
                            forcedAirAnimationState = MarioAnimationState.DOUBLE_JUMP
                            forcedAirAnimationUntil = elapsedSeconds + 0.45
                        }

                        val playerStep = content.playerController.step(
                            state = playerState,
                            horizontalVelocity = horizontalVelocity,
                            deltaSeconds = deltaSeconds,
                            jumpRequested = controllerJumpRequested,
                            allowLeaveGround = true
                        )
                        if (playerStep.jumped) {
                            doubleJumpAvailable = true
                        }
                        val playerYBeforeGravity = playerStep.positionBeforeVertical.y
                        player = playerState.position
                        playerVelocityY = playerState.velocityY
                        isGroundedAfterGravity = playerState.isGrounded

                        updateGoombas(content.goombas, content.enemyController, deltaSeconds)
                        updateBowser(content.bowser, content.enemyController, player, deltaSeconds)
                        val goombaCollision = resolvePlayerGoombaCollisions(
                            player = player,
                            playerYBeforeGravity = playerYBeforeGravity,
                            playerVelocityY = playerVelocityY,
                            isGrounded = isGroundedAfterGravity,
                            goombas = content.goombas,
                            playerController = content.playerController,
                            elapsedSeconds = elapsedSeconds,
                            lastPlayerHitAt = lastPlayerHitAt,
                            highBounceRequested = input.jumpPressed
                        )
                        goombaCollision.applyTo(playerState)
                        updateProgressFromCollision(content.progress, goombaCollision)
                        if (goombaCollision.playerDamage > 0) {
                            forcedAirAnimationState = MarioAnimationState.HURT
                            forcedAirAnimationUntil = elapsedSeconds + PLAYER_HURT_ANIMATION_SECONDS
                            doubleJumpAvailable = false
                        }
                        player = playerState.position
                        playerVelocityY = playerState.velocityY
                        isGroundedAfterGravity = playerState.isGrounded
                        lastPlayerHitAt = goombaCollision.lastPlayerHitAt
                        val bowserCollision = resolvePlayerBowserCollision(
                            player = player,
                            playerYBeforeGravity = playerYBeforeGravity,
                            playerVelocityY = playerVelocityY,
                            isGrounded = isGroundedAfterGravity,
                            bowser = content.bowser,
                            playerController = content.playerController,
                            elapsedSeconds = elapsedSeconds,
                            lastPlayerHitAt = lastPlayerHitAt,
                            highBounceRequested = input.jumpPressed
                        )
                        bowserCollision.applyTo(playerState)
                        updateProgressFromCollision(content.progress, bowserCollision)
                        if (bowserCollision.playerDamage > 0) {
                            forcedAirAnimationState = MarioAnimationState.HURT
                            forcedAirAnimationUntil = elapsedSeconds + PLAYER_HURT_ANIMATION_SECONDS
                            doubleJumpAvailable = false
                        }
                        player = playerState.position
                        playerVelocityY = playerState.velocityY
                        isGroundedAfterGravity = playerState.isGrounded
                        lastPlayerHitAt = bowserCollision.lastPlayerHitAt
                        if (!content.bowser.isActive && !content.summitStar.isCollected) {
                            content.summitStar.isActive = true
                        }
                        resolveSummitStarCollection(content.summitStar, content.playerController, player)
                        collectRegularCoins(content, player)
                        collectGoldenCoins(content, player)
                        rewardCutscene = updateGoldenCoinChallenges(
                            content = content,
                            player = player,
                            elapsedSeconds = elapsedSeconds,
                            activeRewardCutscene = rewardCutscene,
                            bowserDefeatedThisFrame = bowserCollision.bowserDefeated
                        )
                        rewardCutscene = updateRewardCoinCutscene(content, rewardCutscene, elapsedSeconds)

                        if (!wasGrounded && isGroundedAfterGravity) {
                            landingAnimationUntil = elapsedSeconds + 0.22
                        }
                        if (isGroundedAfterGravity) {
                            doubleJumpAvailable = false
                            forcedAirAnimationState = null
                        }
                        wasGrounded = isGroundedAfterGravity
                    }

                    wasJumpPressed = input.jumpPressed
                    wasCrouchPressed = input.crouchPressed

                    val activeForcedAirAnimation = forcedAirAnimationState
                        ?.takeIf { !isGroundedAfterGravity && elapsedSeconds < forcedAirAnimationUntil }
                    val nextMarioAnimationState = when {
                        activeForcedAirAnimation != null -> activeForcedAirAnimation
                        !isGroundedAfterGravity && playerVelocityY > 0.0 -> MarioAnimationState.JUMP
                        !isGroundedAfterGravity -> MarioAnimationState.FALL
                        elapsedSeconds < landingAnimationUntil -> MarioAnimationState.LAND
                        input.crouchPressed && isMoving -> MarioAnimationState.CROUCH_WALK
                        input.crouchPressed -> MarioAnimationState.CROUCH
                        isMoving && input.runPressed -> MarioAnimationState.RUN
                        isMoving -> MarioAnimationState.WALK
                        else -> MarioAnimationState.IDLE
                    }
                    val marioPose = content.marioAnimation.pose(nextMarioAnimationState, deltaSeconds)

                    val cameraTarget = rewardCutscene?.cameraTarget(elapsedSeconds, playerState.position)
                        ?: playerState.position
                    cameraController.follow(cameraTarget, deltaSeconds)
                    val camera = cameraController.camera()

                    content.syncSceneNodes(
                        player = player,
                        playerYaw = playerYaw,
                        marioPose = marioPose,
                        elapsedSeconds = elapsedSeconds
                    )
                    scene.prepareForDraw()
                    hud.prepare(hudRenderer)

                    render(0.47f, 0.67f, 0.94f, 1f, enableDepth = true) { frame ->
                        sceneRenderer.draw(scene, frame, camera)
                        if (debugEnabled) {
                            drawMarioDebugOverlay(
                                debugRenderer = debugRenderer,
                                frame = frame,
                                camera = camera,
                                content = content,
                                playerState = playerState,
                                player = player,
                                playerYaw = playerYaw
                            )
                        }
                        hud.render(hudRenderer, frame)
                    }

                    mouse.mouse.clearFrameState()
                    SDL_Delay(16u)
                }
            } finally {
                resources.cleanup()
            }
        }
    }
}

private data class MarioSceneContent(
    val playerController: KinematicCharacterController3D,
    val enemyController: TerrainActorController3D,
    val marioAnimation: AnimationStateController3D<MarioAnimationState>,
    val goombas: List<RoamingEnemy>,
    val bowser: BossEnemy,
    val summitStar: SummitStar,
    val progress: MarioGameProgress,
    val regularCoins: MutableList<RegularCoin>,
    val goldenCoins: MutableList<GoldenCoin>,
    val bowserNode: Node3D<SceneModel3D>,
    val starNode: Node3D<SceneMesh3D>,
    val regularCoinNodes: List<Node3D<SceneMesh3D>>,
    val goldenCoinNodes: List<Node3D<SceneMesh3D>>,
    val goombaNodes: List<Node3D<AnimatedModelInstance3D>>,
    val marioNode: Node3D<AnimatedModelInstance3D>
)

private data class MarioFrameInput(
    val moveRight: Double,
    val moveForward: Double,
    val lookX: Double,
    val lookY: Double,
    val zoom: Double,
    val cycleCameraDistance: Boolean,
    val runPressed: Boolean,
    val jumpPressed: Boolean,
    val crouchPressed: Boolean,
    val debugPressed: Boolean
)

private fun sampleMarioFrameInput(
    keyboard: KeyboardInputEventSubscriber,
    controller: ControllerInputEventSubscriber,
    controllerAxes: CalibratedControllerAxes,
    elapsedSeconds: Double
): MarioFrameInput {
    val controllerAxisSample = controllerAxes.sample(controller, elapsedSeconds)
    val leftStickX = controllerAxisSample.axis(LEFT_STICK_X_AXIS)
    val leftStickY = controllerAxisSample.axis(LEFT_STICK_Y_AXIS, invert = true)
    val rightStickX = controllerAxisSample.axis(RIGHT_STICK_X_AXIS)
    val rightStickY = controllerAxisSample.axis(RIGHT_STICK_Y_AXIS, invert = true)

    return MarioFrameInput(
        moveRight = snapAxis(
            digitalAxis(keyboard.isDPressed(), keyboard.isAPressed()) + leftStickX,
            MOVEMENT_INPUT_EPSILON
        ),
        moveForward = snapAxis(
            digitalAxis(keyboard.isWPressed(), keyboard.isSPressed()) + leftStickY,
            MOVEMENT_INPUT_EPSILON
        ),
        lookX = snapAxis(
            digitalAxis(keyboard.isRightPressed(), keyboard.isLeftPressed()) - rightStickX,
            LOOK_INPUT_EPSILON
        ),
        lookY = snapAxis(
            digitalAxis(keyboard.isUpPressed(), keyboard.isDownPressed()) + rightStickY,
            LOOK_INPUT_EPSILON
        ),
        zoom = digitalAxis(
            controller.isButtonPressed(Buttons.DPAD_DOWN),
            controller.isButtonPressed(Buttons.DPAD_UP)
        ),
        cycleCameraDistance = keyboard.isTPressed() || controller.isButtonPressed(Buttons.TRIANGLE),
        runPressed = keyboard.isLShiftPressed() ||
            keyboard.isRShiftPressed() ||
            keyboard.isBPressed() ||
            controller.isButtonPressed(Buttons.SQUARE) ||
            controller.isButtonPressed(Buttons.Y),
        jumpPressed = keyboard.isSpacePressed() ||
            keyboard.isXPressed() ||
            controller.isButtonPressed(Buttons.B),
        crouchPressed = keyboard.isLCtrlPressed() ||
            keyboard.isRCtrlPressed() ||
            controller.isButtonPressed(Buttons.L2),
        debugPressed = keyboard.isF1Pressed() || controller.isButtonPressed(Buttons.START)
    )
}

private class MarioHudUi(
    private val content: MarioSceneContent
) {
    private val ui = GpuUiContext3D()

    init {
        addTextWithShadow(
            id = "mario-health",
            x = 20.0,
            y = 14.0,
            width = 112.0,
            height = 42.0,
            color = Color.fromHex("fff6e2"),
            align = GpuUiAlign3D.LEFT,
            text = { lifeText() }
        )
        addCoinCounter(
            id = "gold-coins",
            y = 14.0,
            coinColor = Color.fromHex("f4c63d"),
            coinSize = MARIO_HUD_GOLD_COIN_ICON_SIZE,
            countText = { goldenCoinCountText() }
        )
        addCoinCounter(
            id = "regular-coins",
            y = 60.0,
            coinColor = Color.fromHex("ffd447"),
            coinSize = MARIO_HUD_REGULAR_COIN_ICON_SIZE,
            countText = { regularCoinCountText() }
        )
        ui.performLayout()
    }

    fun prepare(renderer: GpuUiRenderer3D) {
        currentTexts().forEach(renderer::preloadText)
    }

    fun render(
        renderer: GpuUiRenderer3D,
        frame: GpuFrame
    ) {
        renderer.render(ui, frame)
    }

    private fun currentTexts(): List<String> {
        return listOf(
            lifeText(),
            goldenCoinCountText(),
            regularCoinCountText()
        )
    }

    private fun addCoinCounter(
        id: String,
        y: Double,
        coinColor: Color,
        coinSize: Double,
        countText: () -> String
    ) {
        val coinInset = (MARIO_HUD_COIN_ICON_BOX_SIZE - coinSize) * 0.5
        addCoinWithShadow(
            id = "$id-icon",
            x = WINDOW_WIDTH - 122.0 + coinInset,
            y = y + coinInset,
            size = coinSize,
            color = coinColor,
        )
        addTextWithShadow(
            id = "$id-count",
            x = WINDOW_WIDTH - 78.0,
            y = y,
            width = 64.0,
            height = 42.0,
            color = Color.fromHex("fff6e2"),
            align = GpuUiAlign3D.LEFT,
            text = countText
        )
    }

    private fun addCoinWithShadow(
        id: String,
        x: Double,
        y: Double,
        size: Double,
        color: Color
    ) {
        ui.addView(
            hudCoinView(
                id = "$id-shadow",
                x = x + MARIO_HUD_SHADOW_OFFSET,
                y = y + MARIO_HUD_SHADOW_OFFSET,
                size = size,
                color = Color.fromHex("000000c8")
            )
        )
        ui.addView(
            hudCoinView(
                id = id,
                x = x,
                y = y,
                size = size,
                color = color
            )
        )
    }

    private fun hudCoinView(
        id: String,
        x: Double,
        y: Double,
        size: Double,
        color: Color
    ): GpuUiView3D {
        return GpuUiView3D(
            id = id,
            desiredX = x,
            desiredY = y,
            desiredWidth = size,
            desiredHeight = size
        ).apply {
            coin(
                id = "$id-coin",
                width = size,
                height = size,
                color = color
            )
        }
    }

    private fun addTextWithShadow(
        id: String,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        color: Color,
        align: GpuUiAlign3D,
        text: () -> String
    ) {
        ui.addView(
            hudTextView(
                id = "$id-shadow",
                x = x + MARIO_HUD_SHADOW_OFFSET,
                y = y + MARIO_HUD_SHADOW_OFFSET,
                width = width,
                height = height,
                color = Color.fromHex("000000c8"),
                align = align,
                text = text
            )
        )
        ui.addView(
            hudTextView(
                id = id,
                x = x,
                y = y,
                width = width,
                height = height,
                color = color,
                align = align,
                text = text
            )
        )
    }

    private fun hudTextView(
        id: String,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        color: Color,
        align: GpuUiAlign3D,
        text: () -> String
    ): GpuUiView3D {
        return GpuUiView3D(
            id = id,
            desiredX = x,
            desiredY = y,
            desiredWidth = width,
            desiredHeight = height
        ).apply {
            label(
                id = "$id-label",
                text = text,
                width = width,
                height = height,
                color = color,
                align = align
            )
        }
    }

    private fun lifeText(): String {
        return content.progress.marioHealth.coerceIn(0, MARIO_MAX_HEALTH).toString()
    }

    private fun goldenCoinCountText(): String {
        return content.goldenCoins.count { it.isCollected }.toString()
    }

    private fun regularCoinCountText(): String {
        return content.progress.regularCoinsCollected.coerceIn(0, REGULAR_COIN_COUNT).toString()
    }
}

private fun createMarioSceneContent(
    gpu: GpuContext,
    resources: GpuResourceScope3D,
    loadedAssets: LoadedModelAssetBundle3D,
    scene: Scene3D
): MarioSceneContent {
    val terrain = loadedAssets.createTerrainCollider(MarioModelAssets.World)
    val staticWorld = loadedAssets.createStaticCollider(MarioModelAssets.World)
    val playerController = KinematicCharacterController3D(
        terrain = terrain,
        settings = KinematicCharacterControllerSettings3D(
            halfHeight = PLAYER_HALF_HEIGHT,
            collisionRadius = PLAYER_COLLISION_RADIUS,
            maxStepUp = MAX_STEP_UP,
            maxStepDown = PLAYER_MAX_STEP_DOWN,
            groundContactEpsilon = GROUND_CONTACT_EPSILON,
            jumpVelocity = PLAYER_JUMP_VELOCITY,
            gravity = PLAYER_JUMP_GRAVITY,
            fallingGravity = PLAYER_FALL_GRAVITY,
            terminalVelocityY = PLAYER_TERMINAL_FALL_SPEED
        ),
        staticCollider = staticWorld
    )
    val enemyController = TerrainActorController3D(
        terrain = terrain,
        settings = TerrainActorControllerSettings3D(
            halfHeight = 0.0,
            maxStepUp = ENEMY_MAX_STEP_UP,
            maxStepDown = ENEMY_MAX_STEP_DOWN,
            groundContactEpsilon = GROUND_CONTACT_EPSILON
        )
    )
    val marioAnimation = AnimationStateController3D(
        clipMap = marioAnimationClips(loadedAssets.animatedModel(MarioModelAssets.Mario).clips),
        initialState = MarioAnimationState.IDLE
    )
    val starMesh = resources.track(
        GpuMesh.sphere(
            gpu = gpu,
            radius = 0.42,
            color = Color.fromHex("ffd447"),
            rings = 10,
            segments = 16
        )
    ) { it.cleanup() }
    val regularCoinMesh = resources.track(
        GpuMesh.sphere(
            gpu = gpu,
            radius = 0.16,
            color = Color.fromHex("ffd447"),
            rings = 8,
            segments = 12
        )
    ) { it.cleanup() }
    val goldenCoinMesh = resources.track(
        GpuMesh.sphere(
            gpu = gpu,
            radius = 0.35,
            color = Color.fromHex("f4c63d"),
            rings = 12,
            segments = 18
        )
    ) { it.cleanup() }
    val goombas = createGoombas(enemyController)
    val bowser = createBowser(findHighestGroundRegionCenter(terrain))
    val summitStar = createSummitStar(bowser.home)
    val progress = MarioGameProgress()
    val regularCoins = createRegularCoins(terrain)
    val goldenCoins = createGoldenCoins(terrain, bowser.home)

    scene.addModelAssetNode(loadedAssets, MarioModelAssets.World)
    val bowserNode = scene.addModelAssetNode(loadedAssets, MarioModelAssets.Bowser, isVisible = false)
    val starNode = scene.addMeshNode(starMesh, isVisible = false)
    val regularCoinNodes = regularCoins.map {
        scene.addMeshNode(regularCoinMesh, isVisible = false)
    }
    val goldenCoinNodes = goldenCoins.map {
        scene.addMeshNode(goldenCoinMesh, isVisible = false)
    }
    val goombaNodes = goombas.map {
        scene.addAnimatedModelAssetNode(loadedAssets, MarioModelAssets.Goomba, isVisible = false)
    }
    val marioNode = scene.addAnimatedModelAssetNode(loadedAssets, MarioModelAssets.Mario)

    return MarioSceneContent(
        playerController = playerController,
        enemyController = enemyController,
        marioAnimation = marioAnimation,
        goombas = goombas,
        bowser = bowser,
        summitStar = summitStar,
        progress = progress,
        regularCoins = regularCoins,
        goldenCoins = goldenCoins,
        bowserNode = bowserNode,
        starNode = starNode,
        regularCoinNodes = regularCoinNodes,
        goldenCoinNodes = goldenCoinNodes,
        goombaNodes = goombaNodes,
        marioNode = marioNode
    )
}

private fun drawMarioDebugOverlay(
    debugRenderer: DebugRenderer3D,
    frame: GpuFrame,
    camera: Camera3D,
    content: MarioSceneContent,
    playerState: KinematicCharacterState3D,
    player: Vec3,
    playerYaw: Double
) {
    debugRenderer.wireCapsule(
        frame = frame,
        camera = camera,
        capsule = content.playerController.capsuleCollider(playerState, PLAYER_COLLISION_RADIUS),
        color = Color.fromHex("5dffcb")
    )
    val playerForward = forwardForYaw(playerYaw, 1.6)
    debugRenderer.line(
        frame = frame,
        camera = camera,
        start = player,
        end = Vec3(
            player.x + playerForward.x,
            player.y + 0.15,
            player.z + playerForward.z
        ),
        color = Color.fromHex("ffffff")
    )
    playerState.ground?.let { ground ->
        debugRenderer.wireSphere(
            frame = frame,
            camera = camera,
            center = ground.position,
            radius = 0.16,
            color = Color.fromHex("ffd447")
        )
    }
    playerState.staticContacts.forEach { contact ->
        debugRenderer.contactPoint(
            frame = frame,
            camera = camera,
            point = contact.point,
            normal = contact.normal,
            color = Color.fromHex("ff3b6d")
        )
    }
    content.goombas.filter { it.isActive }.forEach { enemy ->
        debugRenderer.wireSphere(
            frame = frame,
            camera = camera,
            sphere = goombaCollider(enemy),
            color = Color.fromHex("ff7a5c")
        )
    }
    if (content.bowser.isActive) {
        debugRenderer.wireSphere(
            frame = frame,
            camera = camera,
            sphere = bowserCollider(content.bowser),
            color = Color.fromHex("ff4058")
        )
        debugRenderer.wireSphere(
            frame = frame,
            camera = camera,
            center = content.bowser.home,
            radius = BOWSER_LEASH_RADIUS,
            color = Color.fromHex("35c9d0")
        )
    }
    if (content.summitStar.isActive && !content.summitStar.isCollected) {
        debugRenderer.wireSphere(
            frame = frame,
            camera = camera,
            sphere = summitStarCollider(content.summitStar),
            color = Color.fromHex("f0c84b")
        )
    }
}

private fun MarioSceneContent.syncSceneNodes(
    player: Vec3,
    playerYaw: Double,
    marioPose: AnimationPose3D,
    elapsedSeconds: Double
) {
    bowserNode.setVisible(bowser.isActive)
    if (bowser.isActive) {
        val hitPulse = if (elapsedSeconds < bowser.hitFlashUntil) {
            1.0 + abs(sin(elapsedSeconds * 52.0)) * 0.12
        } else {
            1.0
        }
        bowserNode.setPositionYaw(
            position = bowser.position,
            yawRadians = bowser.yaw,
            yOffset = 0.18 + sin(elapsedSeconds * 1.1) * 0.1,
            yawOffsetRadians = BOWSER_MODEL_YAW_OFFSET,
            scale = Vec3(hitPulse, hitPulse, hitPulse)
        )
    }

    starNode.setVisible(summitStar.isActive && !summitStar.isCollected)
    if (starNode.isVisible) {
        starNode.setTransform(
            Transform3D(
                position = Vec3(
                    summitStar.position.x,
                    summitStar.position.y + sin(elapsedSeconds * 3.1) * 0.16,
                    summitStar.position.z
                ),
                rotation = Vec3(0.0, elapsedSeconds * 2.8, elapsedSeconds * 1.5)
            )
        )
    }

    regularCoinNodes.forEachIndexed { index, node ->
        val coin = regularCoins[index]
        node.setVisible(!coin.isCollected)
        if (node.isVisible) {
            node.setTransform(
                Transform3D(
                    position = Vec3(
                        coin.position.x,
                        coin.position.y + sin(elapsedSeconds * 3.2 + index * 0.21) * 0.08,
                        coin.position.z
                    ),
                    rotation = Vec3(0.0, elapsedSeconds * 4.2 + index * 0.12, 0.0),
                    scale = Vec3(1.0, 1.28, 0.16)
                )
            )
        }
    }

    goldenCoinNodes.forEachIndexed { index, node ->
        val coin = goldenCoins[index]
        node.setVisible(coin.isAvailable && !coin.isCollected)
        if (node.isVisible) {
            val size = if (coin.isBig) 1.45 else 1.0
            node.setTransform(
                Transform3D(
                    position = Vec3(
                        coin.renderPosition.x,
                        coin.renderPosition.y + sin(elapsedSeconds * 2.4 + index) * 0.14,
                        coin.renderPosition.z
                    ),
                    rotation = Vec3(0.0, elapsedSeconds * 3.1 + index, 0.0),
                    scale = Vec3(size, size * 1.25, size * 0.14)
                )
            )
        }
    }

    goombaNodes.forEachIndexed { index, node ->
        val enemy = goombas[index]
        node.setVisible(enemy.isActive)
        if (enemy.isActive) {
            node.setPositionYaw(
                position = enemy.position,
                yawRadians = enemy.yaw,
                yawOffsetRadians = GOOMBA_MODEL_YAW_OFFSET
            ).setPose(AnimationPose3D(timeSeconds = elapsedSeconds * 1.4 + enemy.angle))
        }
    }

    marioNode
        .setPositionYaw(
            position = player,
            yawRadians = playerYaw,
            yOffset = -PLAYER_HALF_HEIGHT,
            yawOffsetRadians = MARIO_MODEL_YAW_OFFSET
        )
        .setPose(marioPose)
}

private data class RoamingEnemy(
    val center: Vec3,
    val radius: Double,
    var speed: Double,
    var angle: Double,
    var position: Vec3,
    var yaw: Double,
    var isActive: Boolean = true
)

private data class BossEnemy(
    val home: Vec3,
    var position: Vec3,
    var yaw: Double,
    var velocityY: Double = 0.0,
    var isGrounded: Boolean = true,
    var attackCooldownSeconds: Double = 1.0,
    var lungeX: Double = 0.0,
    var lungeZ: Double = 0.0,
    var health: Int = BOWSER_HEALTH,
    var hitFlashUntil: Double = 0.0,
    var isActive: Boolean = true
)

private data class SummitStar(
    val position: Vec3,
    var isActive: Boolean = false,
    var isCollected: Boolean = false
)

private data class MarioGameProgress(
    var marioHealth: Int = MARIO_MAX_HEALTH,
    var regularCoinsCollected: Int = 0,
    var goombasDefeated: Int = 0
)

private data class RegularCoin(
    val position: Vec3,
    var isCollected: Boolean = false
)

private data class GoldenCoin(
    val challenge: GoldenCoinChallenge,
    val label: String,
    val position: Vec3,
    val isBig: Boolean = false,
    var isAvailable: Boolean = true,
    var isCollected: Boolean = false,
    var renderPosition: Vec3 = position
)

private enum class GoldenCoinChallenge {
    FIVE_GOOMBAS,
    FIFTY_COINS,
    BOWSER,
    HILLTOP,
    RIDGE
}

private data class RewardCoinCutscene(
    val challenge: GoldenCoinChallenge,
    val startPosition: Vec3,
    val targetPosition: Vec3,
    val startedAt: Double
) {
    private val flyEndsAt = startedAt + REWARD_COIN_FLY_SECONDS
    private val holdEndsAt = flyEndsAt + REWARD_COIN_HOLD_SECONDS
    private val returnEndsAt = holdEndsAt + REWARD_COIN_RETURN_SECONDS

    fun isActive(elapsedSeconds: Double): Boolean = elapsedSeconds < returnEndsAt

    fun coinPosition(elapsedSeconds: Double): Vec3 {
        val t = ((elapsedSeconds - startedAt) / REWARD_COIN_FLY_SECONDS).coerceIn(0.0, 1.0)
        val eased = smoothStep(t)
        val arcY = sin(eased * 3.141592653589793) * REWARD_COIN_ARC_HEIGHT
        val base = lerp(startPosition, targetPosition, eased)
        return Vec3(base.x, base.y + arcY, base.z)
    }

    fun cameraTarget(elapsedSeconds: Double, player: Vec3): Vec3 {
        val coinFocus = coinPosition(elapsedSeconds).let {
            Vec3(it.x, it.y + REWARD_COIN_CAMERA_Y_OFFSET, it.z)
        }
        if (elapsedSeconds < holdEndsAt) {
            return coinFocus
        }

        val t = ((elapsedSeconds - holdEndsAt) / REWARD_COIN_RETURN_SECONDS).coerceIn(0.0, 1.0)
        return lerp(coinFocus, player, smoothStep(t))
    }
}

private data class PlayerEnemyCollisionResult(
    val player: Vec3,
    val playerVelocityY: Double,
    val isGrounded: Boolean,
    val lastPlayerHitAt: Double,
    val defeatedGoombas: Int = 0,
    val bowserDefeated: Boolean = false,
    val playerDamage: Int = 0
) {
    fun applyTo(state: KinematicCharacterState3D) {
        state.position = player
        state.velocityY = playerVelocityY
        state.isGrounded = isGrounded
    }
}

private enum class MarioAnimationState {
    IDLE,
    WALK,
    RUN,
    CROUCH,
    CROUCH_WALK,
    JUMP,
    DOUBLE_JUMP,
    BACKFLIP,
    LONG_JUMP,
    GROUND_POUND,
    HURT,
    FALL,
    LAND
}

private fun marioAnimationClips(clips: AnimationClipSet3D): AnimationClipMap3D<MarioAnimationState> {
    return AnimationClipMap3D.fromClipNames(
        clips = clips,
        references = listOf(
            marioClipReference(clips, MarioAnimationState.IDLE, listOf("Armature|AreaWait64"), "Armature|AreaWait64"),
            marioClipReference(clips, MarioAnimationState.WALK, listOf("Armature|Walk"), "Armature|Walk", 1.42),
            marioClipReference(clips, MarioAnimationState.RUN, listOf("Armature|Run"), "Armature|Run", 2.05),
            marioClipReference(
                clips = clips,
                state = MarioAnimationState.CROUCH,
                candidates = listOf(
                    "Armature|Crouch",
                    "Armature|WaitToCrouch",
                    "Armature|Crouching"
                ),
                fallback = "Armature|AreaWait64"
            ),
            marioClipReference(
                clips = clips,
                state = MarioAnimationState.CROUCH_WALK,
                candidates = listOf(
                    "Armature|CrouchWalk",
                    "Armature|WalkCrouching"
                ),
                fallback = "Armature|Walk",
                playbackSpeed = 0.82
            ),
            marioClipReference(clips, MarioAnimationState.JUMP, listOf("Armature|Jump"), "Armature|Jump"),
            marioClipReference(
                clips = clips,
                state = MarioAnimationState.DOUBLE_JUMP,
                candidates = listOf(
                    "Armature|DoubleJump",
                    "Armature|JumpDouble"
                ),
                fallback = "Armature|Jump",
                playbackSpeed = 1.08
            ),
            marioClipReference(
                clips = clips,
                state = MarioAnimationState.BACKFLIP,
                candidates = listOf(
                    "Armature|BackFlip",
                    "Armature|Backflip",
                    "Armature|JumpBack"
                ),
                fallback = "Armature|Jump",
                playbackSpeed = 1.12
            ),
            marioClipReference(
                clips = clips,
                state = MarioAnimationState.LONG_JUMP,
                candidates = listOf(
                    "Armature|LongJump",
                    "Armature|JumpLong"
                ),
                fallback = "Armature|Jump",
                playbackSpeed = 0.95
            ),
            marioClipReference(
                clips = clips,
                state = MarioAnimationState.GROUND_POUND,
                candidates = listOf(
                    "Armature|GroundPound",
                    "Armature|HipDrop",
                    "Armature|Fall"
                ),
                fallback = "Armature|Fall",
                playbackSpeed = 1.18
            ),
            marioClipReference(
                clips = clips,
                state = MarioAnimationState.HURT,
                candidates = listOf(
                    "Armature|Damage",
                    "Armature|Damaged",
                    "Armature|Hurt"
                ),
                fallback = "Armature|Fall",
                playbackSpeed = 1.05
            ),
            marioClipReference(clips, MarioAnimationState.FALL, listOf("Armature|Fall"), "Armature|Fall"),
            marioClipReference(clips, MarioAnimationState.LAND, listOf("Armature|Land"), "Armature|Land")
        )
    )
}

private fun marioClipReference(
    clips: AnimationClipSet3D,
    state: MarioAnimationState,
    candidates: List<String>,
    fallback: String,
    playbackSpeed: Double = 1.0
): AnimationClipReference3D<MarioAnimationState> {
    val clipName = (candidates + fallback).firstOrNull { clips.indexOf(it) != null }
        ?: clips.names.first()
    return AnimationClipReference3D(state, clipName, playbackSpeed = playbackSpeed)
}

private fun resolvePlayerGoombaCollisions(
    player: Vec3,
    playerYBeforeGravity: Double,
    playerVelocityY: Double,
    isGrounded: Boolean,
    goombas: List<RoamingEnemy>,
    playerController: KinematicCharacterController3D,
    elapsedSeconds: Double,
    lastPlayerHitAt: Double,
    highBounceRequested: Boolean
): PlayerEnemyCollisionResult {
    var resolvedPlayer = player
    var resolvedVelocityY = playerVelocityY
    var resolvedGrounded = isGrounded
    var resolvedLastHitAt = lastPlayerHitAt
    var defeatedGoombas = 0
    var playerDamage = 0

    goombas.filter { it.isActive }.forEach { enemy ->
        val contact = Collision3D.overlap(
            playerController.capsuleCollider(resolvedPlayer, PLAYER_COLLISION_RADIUS),
            goombaCollider(enemy)
        ) ?: return@forEach
        val feetWereAboveStompLine = playerYBeforeGravity - PLAYER_HALF_HEIGHT >=
            enemy.position.y + GOOMBA_STOMP_MIN_HEIGHT
        val feetAreAboveGround = resolvedPlayer.y - PLAYER_HALF_HEIGHT >= enemy.position.y - GROUND_CONTACT_EPSILON
        val isStomp = resolvedVelocityY <= 0.0 && feetWereAboveStompLine && feetAreAboveGround
        if (isStomp) {
            enemy.isActive = false
            defeatedGoombas += 1
            resolvedVelocityY = if (highBounceRequested) {
                PLAYER_HIGH_STOMP_BOUNCE_VELOCITY
            } else {
                PLAYER_STOMP_BOUNCE_VELOCITY
            }
            resolvedGrounded = false
            return@forEach
        }

        if (elapsedSeconds - resolvedLastHitAt < PLAYER_HURT_COOLDOWN_SECONDS) {
            return@forEach
        }

        val away = horizontalAwayFromEnemy(resolvedPlayer, enemy.position)
        val bumpMove = playerController.terrainController.moveHorizontal(
            position = resolvedPlayer,
            deltaX = away.x * (PLAYER_BUMP_BACK_DISTANCE + contact.depth * 0.25),
            deltaZ = away.z * (PLAYER_BUMP_BACK_DISTANCE + contact.depth * 0.25),
            isGrounded = resolvedGrounded,
            allowLeaveGround = false
        )
        resolvedPlayer = bumpMove.position
        resolvedGrounded = false
        resolvedVelocityY = PLAYER_HURT_BOUNCE_VELOCITY
        resolvedLastHitAt = elapsedSeconds
        playerDamage += GOOMBA_DAMAGE
    }

    return PlayerEnemyCollisionResult(
        player = resolvedPlayer,
        playerVelocityY = resolvedVelocityY,
        isGrounded = resolvedGrounded,
        lastPlayerHitAt = resolvedLastHitAt,
        defeatedGoombas = defeatedGoombas,
        playerDamage = playerDamage
    )
}

private fun createBowser(anchor: Vec3): BossEnemy {
    return BossEnemy(
        home = anchor,
        position = anchor,
        yaw = 0.0
    )
}

private fun updateBowser(
    bowser: BossEnemy,
    controller: TerrainActorController3D,
    player: Vec3,
    deltaSeconds: Double
) {
    if (!bowser.isActive) {
        return
    }

    bowser.attackCooldownSeconds = maxOf(0.0, bowser.attackCooldownSeconds - deltaSeconds)
    val homeDistance = horizontalDistance(bowser.position, bowser.home)
    val nearbyGroundY = controller.actorYAt(
        x = bowser.position.x,
        z = bowser.position.z,
        minActorY = bowser.home.y - BOWSER_FALL_RECOVERY_DROP,
        maxActorY = bowser.position.y + GROUND_CONTACT_EPSILON
    )
    if (nearbyGroundY == null ||
        bowser.position.y < bowser.home.y - BOWSER_FALL_RECOVERY_DROP ||
        homeDistance > BOWSER_RESET_RADIUS
    ) {
        resetBowserToHome(bowser, controller)
        return
    }

    if (bowser.position.y <= nearbyGroundY + GROUND_CONTACT_EPSILON && bowser.velocityY <= 0.1) {
        bowser.position = Vec3(bowser.position.x, nearbyGroundY, bowser.position.z)
        bowser.isGrounded = true
    }

    val playerDistance = horizontalDistance(bowser.position, player)
    val shouldChasePlayer = playerDistance <= BOWSER_AGGRO_RADIUS && homeDistance <= BOWSER_LEASH_RADIUS
    val target = if (shouldChasePlayer) player else bowser.home
    val targetDeltaX = target.x - bowser.position.x
    val targetDeltaZ = target.z - bowser.position.z
    val targetDistance = sqrt(targetDeltaX * targetDeltaX + targetDeltaZ * targetDeltaZ)
    val directionX = if (targetDistance > 0.000001) targetDeltaX / targetDistance else 0.0
    val directionZ = if (targetDistance > 0.000001) targetDeltaZ / targetDistance else -1.0
    val targetYaw = atan2(-directionX, -directionZ)
    if (shouldChasePlayer || targetDistance > BOWSER_HOME_STOP_RADIUS) {
        bowser.yaw = moveAngleToward(
            current = bowser.yaw,
            target = targetYaw,
            maxStep = (if (bowser.isGrounded) BOWSER_GROUNDED_TURN_SPEED else BOWSER_AIR_TURN_SPEED) * deltaSeconds
        )
    }

    val yawError = abs(shortestAngleDelta(bowser.yaw, targetYaw))
    if (bowser.isGrounded &&
        shouldChasePlayer &&
        playerDistance < BOWSER_AGGRO_RADIUS * 0.72 &&
        yawError <= BOWSER_ATTACK_MAX_YAW_ERROR &&
        bowser.attackCooldownSeconds <= 0.0
    ) {
        bowser.velocityY = BOWSER_JUMP_VELOCITY
        bowser.isGrounded = false
        val forward = forwardForYaw(bowser.yaw, 1.0)
        bowser.lungeX = forward.x
        bowser.lungeZ = forward.z
        bowser.attackCooldownSeconds = BOWSER_ATTACK_COOLDOWN_SECONDS
    }

    val horizontalSpeed = if (bowser.isGrounded) BOWSER_WALK_SPEED else BOWSER_LUNGE_SPEED
    val facing = forwardForYaw(bowser.yaw, 1.0)
    val turnAlignment = if (bowser.isGrounded) {
        ((cos(yawError) + 1.0) * 0.5).coerceIn(0.25, 1.0)
    } else {
        1.0
    }
    val moveDirectionX = if (bowser.isGrounded || !shouldChasePlayer) facing.x else bowser.lungeX
    val moveDirectionZ = if (bowser.isGrounded || !shouldChasePlayer) facing.z else bowser.lungeZ
    val shouldMove = if (shouldChasePlayer) {
        playerDistance > BOWSER_COLLISION_RADIUS * 1.4 || !bowser.isGrounded
    } else {
        targetDistance > BOWSER_HOME_STOP_RADIUS
    }
    if (shouldMove) {
        moveBowserSafely(
            bowser = bowser,
            controller = controller,
            deltaX = moveDirectionX * horizontalSpeed * turnAlignment * deltaSeconds,
            deltaZ = moveDirectionZ * horizontalSpeed * turnAlignment * deltaSeconds
        )
    }

    val verticalMove = controller.applyGravity(
        position = bowser.position,
        velocityY = bowser.velocityY,
        deltaSeconds = deltaSeconds,
        gravity = BOWSER_GRAVITY,
        terminalVelocityY = BOWSER_TERMINAL_FALL_SPEED
    )
    bowser.position = verticalMove.position
    bowser.velocityY = verticalMove.velocityY
    bowser.isGrounded = verticalMove.isGrounded
}

private fun moveBowserSafely(
    bowser: BossEnemy,
    controller: TerrainActorController3D,
    deltaX: Double,
    deltaZ: Double
) {
    val nextX = bowser.position.x + deltaX
    val nextZ = bowser.position.z + deltaZ
    val nextPosition = Vec3(nextX, bowser.position.y, nextZ)
    if (horizontalDistance(nextPosition, bowser.home) > BOWSER_RESET_RADIUS) {
        return
    }

    val groundAhead = controller.actorYAt(
        x = nextX,
        z = nextZ,
        minActorY = bowser.home.y - BOWSER_FALL_RECOVERY_DROP,
        maxActorY = bowser.position.y + BOWSER_JUMP_VELOCITY
    ) ?: return

    if (!bowser.isGrounded && groundAhead < bowser.home.y - BOWSER_FALL_RECOVERY_DROP) {
        return
    }

    val move = controller.moveHorizontal(
        position = bowser.position,
        deltaX = deltaX,
        deltaZ = deltaZ,
        isGrounded = bowser.isGrounded,
        allowLeaveGround = false
    )
    bowser.position = move.position
    bowser.isGrounded = if (bowser.isGrounded) move.isGrounded else false
}

private fun resetBowserToHome(
    bowser: BossEnemy,
    controller: TerrainActorController3D
) {
    val homeY = controller.actorYAt(bowser.home.x, bowser.home.z) ?: bowser.home.y
    bowser.position = Vec3(bowser.home.x, homeY, bowser.home.z)
    bowser.velocityY = 0.0
    bowser.isGrounded = true
    bowser.attackCooldownSeconds = 1.0
    bowser.lungeX = 0.0
    bowser.lungeZ = 0.0
}

private fun createSummitStar(anchor: Vec3): SummitStar {
    return SummitStar(
        position = Vec3(anchor.x, anchor.y + STAR_FLOAT_HEIGHT, anchor.z)
    )
}

private fun createRegularCoins(terrain: TerrainMeshCollider3D): MutableList<RegularCoin> {
    val random = Random(REGULAR_COIN_SCATTER_SEED)
    val coins = mutableListOf<RegularCoin>()
    val cellSize = (REGULAR_COIN_SCATTER_RANGE * 2.0) / REGULAR_COIN_SCATTER_GRID_SIZE.toDouble()
    val cells = (0 until REGULAR_COIN_SCATTER_GRID_SIZE).flatMap { cellX ->
        (0 until REGULAR_COIN_SCATTER_GRID_SIZE).map { cellZ ->
            cellX to cellZ
        }
    }.shuffled(random)

    cells.forEach { (cellX, cellZ) ->
        if (coins.size >= REGULAR_COIN_COUNT) {
            return@forEach
        }

        val minX = -REGULAR_COIN_SCATTER_RANGE + cellX * cellSize
        val minZ = -REGULAR_COIN_SCATTER_RANGE + cellZ * cellSize
        var placedInCell = false
        repeat(4) {
            if (!placedInCell && tryAddRegularCoin(
                    terrain = terrain,
                    coins = coins,
                    x = minX + random.nextDouble(0.12, 0.88) * cellSize,
                    z = minZ + random.nextDouble(0.12, 0.88) * cellSize,
                    minSpacing = REGULAR_COIN_MIN_SPACING
                )
            ) {
                placedInCell = true
            }
        }
    }

    val spacingPasses = listOf(
        REGULAR_COIN_MIN_SPACING,
        REGULAR_COIN_MIN_SPACING * 0.75,
        REGULAR_COIN_MIN_SPACING * 0.5,
        0.0
    )
    spacingPasses.forEach { minSpacing ->
        var attempts = 0
        while (coins.size < REGULAR_COIN_COUNT && attempts < REGULAR_COIN_SCATTER_ATTEMPTS) {
            attempts += 1
            tryAddRegularCoin(
                terrain = terrain,
                coins = coins,
                x = random.nextDouble(-REGULAR_COIN_SCATTER_RANGE, REGULAR_COIN_SCATTER_RANGE),
                z = random.nextDouble(-REGULAR_COIN_SCATTER_RANGE, REGULAR_COIN_SCATTER_RANGE),
                minSpacing = minSpacing
            )
        }
    }

    check(coins.size == REGULAR_COIN_COUNT) {
        "Mario regular coin layout must place $REGULAR_COIN_COUNT coins."
    }
    return coins
}

private fun tryAddRegularCoin(
    terrain: TerrainMeshCollider3D,
    coins: MutableList<RegularCoin>,
    x: Double,
    z: Double,
    minSpacing: Double
): Boolean {
    val groundY = terrain.groundYAt(x, z) ?: return false
    val position = Vec3(x, groundY + 0.62, z)
    val minSpacingSquared = minSpacing * minSpacing
    if (minSpacingSquared > 0.0 &&
        coins.any { squaredHorizontalDistance(it.position, position) < minSpacingSquared }
    ) {
        return false
    }
    coins += RegularCoin(position)
    return true
}

private fun createGoldenCoins(
    terrain: TerrainMeshCollider3D,
    bowserHome: Vec3
): MutableList<GoldenCoin> {
    return mutableListOf(
        GoldenCoin(
            challenge = GoldenCoinChallenge.FIVE_GOOMBAS,
            label = "GOOMBA FIVE",
            position = collectiblePosition(terrain, -8.0, -9.0, yOffset = 1.35),
            isBig = true,
            isAvailable = false
        ),
        GoldenCoin(
            challenge = GoldenCoinChallenge.FIFTY_COINS,
            label = "FIFTY COINS",
            position = collectiblePosition(terrain, 0.0, 0.0, yOffset = 1.35),
            isBig = true,
            isAvailable = false
        ),
        GoldenCoin(
            challenge = GoldenCoinChallenge.BOWSER,
            label = "BOWSER",
            position = Vec3(bowserHome.x, bowserHome.y + 1.55, bowserHome.z),
            isBig = true,
            isAvailable = false
        ),
        GoldenCoin(
            challenge = GoldenCoinChallenge.HILLTOP,
            label = "HILLTOP",
            position = collectiblePosition(terrain, -21.0, 17.0, yOffset = 1.1)
        ),
        GoldenCoin(
            challenge = GoldenCoinChallenge.RIDGE,
            label = "RIDGE",
            position = collectiblePosition(terrain, 24.0, -18.0, yOffset = 1.1)
        )
    )
}

private fun collectiblePosition(
    terrain: TerrainMeshCollider3D,
    x: Double,
    z: Double,
    yOffset: Double
): Vec3 {
    val ground = groundPosition(terrain, x, z)
    return Vec3(ground.x, ground.y + yOffset, ground.z)
}

private fun resolveSummitStarCollection(
    star: SummitStar,
    playerController: KinematicCharacterController3D,
    player: Vec3
) {
    if (!star.isActive || star.isCollected) {
        return
    }

    if (Collision3D.overlap(
            playerController.capsuleCollider(player, PLAYER_COLLISION_RADIUS),
            summitStarCollider(star)
        ) != null
    ) {
        star.isCollected = true
        star.isActive = false
    }
}

private fun updateProgressFromCollision(
    progress: MarioGameProgress,
    result: PlayerEnemyCollisionResult
) {
    if (result.playerDamage > 0) {
        progress.marioHealth = (progress.marioHealth - result.playerDamage).coerceAtLeast(0)
    }
    progress.goombasDefeated += result.defeatedGoombas
}

private fun collectRegularCoins(
    content: MarioSceneContent,
    player: Vec3
) {
    var collected = 0
    content.regularCoins.forEach { coin ->
        if (!coin.isCollected && playerTouchesCollectible(player, coin.position, REGULAR_COIN_PICKUP_RADIUS)) {
            coin.isCollected = true
            collected += 1
        }
    }
    content.progress.regularCoinsCollected += collected
}

private fun collectGoldenCoins(
    content: MarioSceneContent,
    player: Vec3
) {
    content.goldenCoins.forEach { coin ->
        if (coin.isAvailable &&
            !coin.isCollected &&
            playerTouchesCollectible(player, coin.renderPosition, GOLDEN_COIN_PICKUP_RADIUS)
        ) {
            coin.isCollected = true
            coin.isAvailable = false
        }
    }
}

private fun updateGoldenCoinChallenges(
    content: MarioSceneContent,
    player: Vec3,
    elapsedSeconds: Double,
    activeRewardCutscene: RewardCoinCutscene?,
    bowserDefeatedThisFrame: Boolean
): RewardCoinCutscene? {
    if (activeRewardCutscene != null) {
        return activeRewardCutscene
    }

    val challenge = when {
        content.progress.goombasDefeated >= 5 &&
            content.canRevealGoldenCoin(GoldenCoinChallenge.FIVE_GOOMBAS) -> GoldenCoinChallenge.FIVE_GOOMBAS
        content.progress.regularCoinsCollected >= 50 &&
            content.canRevealGoldenCoin(GoldenCoinChallenge.FIFTY_COINS) -> GoldenCoinChallenge.FIFTY_COINS
        (bowserDefeatedThisFrame || !content.bowser.isActive) &&
            content.canRevealGoldenCoin(GoldenCoinChallenge.BOWSER) -> GoldenCoinChallenge.BOWSER
        else -> null
    }

    return challenge?.let { revealGoldenCoin(content, it, player, elapsedSeconds) }
}

private fun MarioSceneContent.canRevealGoldenCoin(challenge: GoldenCoinChallenge): Boolean {
    val coin = goldenCoins.firstOrNull { it.challenge == challenge } ?: return false
    return !coin.isAvailable && !coin.isCollected
}

private fun revealGoldenCoin(
    content: MarioSceneContent,
    challenge: GoldenCoinChallenge,
    player: Vec3,
    elapsedSeconds: Double
): RewardCoinCutscene? {
    val coin = content.goldenCoins.firstOrNull { it.challenge == challenge } ?: return null
    if (coin.isCollected || coin.isAvailable) {
        return null
    }

    val startPosition = Vec3(player.x, player.y + 1.3, player.z)
    coin.isAvailable = true
    coin.renderPosition = startPosition
    return RewardCoinCutscene(
        challenge = challenge,
        startPosition = startPosition,
        targetPosition = coin.position,
        startedAt = elapsedSeconds
    )
}

private fun updateRewardCoinCutscene(
    content: MarioSceneContent,
    cutscene: RewardCoinCutscene?,
    elapsedSeconds: Double
): RewardCoinCutscene? {
    if (cutscene == null) {
        return null
    }

    val coin = content.goldenCoins.firstOrNull { it.challenge == cutscene.challenge } ?: return null
    if (!cutscene.isActive(elapsedSeconds)) {
        coin.renderPosition = coin.position
        return null
    }

    coin.renderPosition = cutscene.coinPosition(elapsedSeconds)
    return cutscene
}

private fun smoothStep(value: Double): Double {
    val t = value.coerceIn(0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)
}

private fun playerTouchesCollectible(
    player: Vec3,
    collectible: Vec3,
    radius: Double
): Boolean {
    return horizontalDistance(player, collectible) <= radius &&
        abs(player.y - collectible.y) <= PLAYER_HALF_HEIGHT + COIN_PICKUP_VERTICAL_PADDING
}

private fun resolvePlayerBowserCollision(
    player: Vec3,
    playerYBeforeGravity: Double,
    playerVelocityY: Double,
    isGrounded: Boolean,
    bowser: BossEnemy,
    playerController: KinematicCharacterController3D,
    elapsedSeconds: Double,
    lastPlayerHitAt: Double,
    highBounceRequested: Boolean
): PlayerEnemyCollisionResult {
    if (!bowser.isActive) {
        return PlayerEnemyCollisionResult(player, playerVelocityY, isGrounded, lastPlayerHitAt)
    }

    val contact = Collision3D.overlap(
        playerController.capsuleCollider(player, PLAYER_COLLISION_RADIUS),
        bowserCollider(bowser)
    )
        ?: return PlayerEnemyCollisionResult(player, playerVelocityY, isGrounded, lastPlayerHitAt)
    val feetWereAboveStompLine = playerYBeforeGravity - PLAYER_HALF_HEIGHT >=
        bowser.position.y + BOWSER_STOMP_MIN_HEIGHT
    val feetAreAboveGround = player.y - PLAYER_HALF_HEIGHT >= bowser.position.y - GROUND_CONTACT_EPSILON
    val isStomp = playerVelocityY <= 0.0 && feetWereAboveStompLine && feetAreAboveGround
    if (isStomp) {
        bowser.health -= 1
        bowser.hitFlashUntil = elapsedSeconds + BOWSER_HIT_FLASH_SECONDS
        val bowserDefeated = bowser.health <= 0
        if (bowser.health <= 0) {
            bowser.isActive = false
        }
        return PlayerEnemyCollisionResult(
            player = player,
            playerVelocityY = if (highBounceRequested) {
                BOWSER_HIGH_STOMP_BOUNCE_VELOCITY
            } else {
                BOWSER_STOMP_BOUNCE_VELOCITY
            },
            isGrounded = false,
            lastPlayerHitAt = lastPlayerHitAt,
            bowserDefeated = bowserDefeated
        )
    }

    if (elapsedSeconds - lastPlayerHitAt < PLAYER_HURT_COOLDOWN_SECONDS) {
        return PlayerEnemyCollisionResult(player, playerVelocityY, isGrounded, lastPlayerHitAt)
    }

    val away = horizontalAwayFromEnemy(player, bowser.position)
    val bumpMove = playerController.terrainController.moveHorizontal(
        position = player,
        deltaX = away.x * (BOWSER_BODY_BUMP_DISTANCE + contact.depth * 0.25),
        deltaZ = away.z * (BOWSER_BODY_BUMP_DISTANCE + contact.depth * 0.25),
        isGrounded = isGrounded,
        allowLeaveGround = false
    )
    return PlayerEnemyCollisionResult(
        player = bumpMove.position,
        playerVelocityY = PLAYER_HURT_BOUNCE_VELOCITY,
        isGrounded = false,
        lastPlayerHitAt = elapsedSeconds,
        playerDamage = BOWSER_DAMAGE
    )
}

private fun goombaCollider(enemy: RoamingEnemy): SphereCollider3D {
    return SphereCollider3D(
        center = Vec3(
            enemy.position.x,
            enemy.position.y + GOOMBA_COLLISION_CENTER_Y,
            enemy.position.z
        ),
        radius = GOOMBA_COLLISION_RADIUS
    )
}

private fun bowserCollider(bowser: BossEnemy): SphereCollider3D {
    return SphereCollider3D(
        center = Vec3(
            bowser.position.x,
            bowser.position.y + BOWSER_COLLISION_CENTER_Y,
            bowser.position.z
        ),
        radius = BOWSER_COLLISION_RADIUS
    )
}

private fun summitStarCollider(star: SummitStar): SphereCollider3D {
    return SphereCollider3D(
        center = star.position,
        radius = STAR_PICKUP_RADIUS
    )
}

private fun horizontalAwayFromEnemy(
    player: Vec3,
    enemy: Vec3
): Vec3 {
    val deltaX = player.x - enemy.x
    val deltaZ = player.z - enemy.z
    val length = sqrt(deltaX * deltaX + deltaZ * deltaZ)
    if (length < 0.000001) {
        return Vec3(0.0, 0.0, 1.0)
    }
    return Vec3(deltaX / length, 0.0, deltaZ / length)
}

private fun createGoombas(controller: TerrainActorController3D): MutableList<RoamingEnemy> {
    return mutableListOf(
        createGoomba(controller, centerX = -8.0, centerZ = -9.0, radius = 3.4, speed = 0.85, angle = 0.0),
        createGoomba(controller, centerX = 7.5, centerZ = -12.0, radius = 4.2, speed = 0.66, angle = 1.7),
        createGoomba(controller, centerX = -17.0, centerZ = 7.0, radius = 4.8, speed = 0.72, angle = 3.0),
        createGoomba(controller, centerX = 17.0, centerZ = 10.0, radius = 4.0, speed = 0.93, angle = 4.4),
        createGoomba(controller, centerX = 0.0, centerZ = 18.0, radius = 5.1, speed = 0.58, angle = 2.2)
    )
}

private fun createGoomba(
    controller: TerrainActorController3D,
    centerX: Double,
    centerZ: Double,
    radius: Double,
    speed: Double,
    angle: Double
): RoamingEnemy {
    val x = centerX + cos(angle) * radius
    val z = centerZ + sin(angle) * radius
    val position = Vec3(x, controller.actorYAt(x, z) ?: 0.0, z)
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
    controller: TerrainActorController3D,
    deltaSeconds: Double
) {
    enemies.forEach { enemy ->
        if (!enemy.isActive) {
            return@forEach
        }

        val previous = enemy.position
        val nextAngle = enemy.angle + enemy.speed * deltaSeconds
        val next = moveGoombaTowardAngle(enemy, controller, nextAngle)
        if (next == null) {
            enemy.speed = -enemy.speed
            val reversedAngle = enemy.angle + enemy.speed * deltaSeconds
            moveGoombaTowardAngle(enemy, controller, reversedAngle)?.let { reversedNext ->
                enemy.angle = reversedAngle
                enemy.position = reversedNext
            }
        } else {
            enemy.angle = nextAngle
            enemy.position = next
        }

        val deltaX = enemy.position.x - previous.x
        val deltaZ = enemy.position.z - previous.z
        if (deltaX * deltaX + deltaZ * deltaZ > 0.000001) {
            enemy.yaw = atan2(-deltaX, -deltaZ)
        }
    }
}

private fun moveGoombaTowardAngle(
    enemy: RoamingEnemy,
    controller: TerrainActorController3D,
    angle: Double
): Vec3? {
    val nextX = enemy.center.x + cos(angle) * enemy.radius
    val nextZ = enemy.center.z + sin(angle) * enemy.radius
    val move = controller.moveHorizontal(
        position = enemy.position,
        deltaX = nextX - enemy.position.x,
        deltaZ = nextZ - enemy.position.z,
        isGrounded = true,
        allowLeaveGround = false
    )
    return if (move.moved && move.isGrounded) move.position else null
}

private fun groundPosition(
    terrain: TerrainMeshCollider3D,
    x: Double,
    z: Double,
    fallbackY: Double = 0.0
): Vec3 {
    return Vec3(x, terrain.groundYAt(x, z) ?: fallbackY, z)
}

private fun findHighestGroundRegionCenter(terrain: TerrainMeshCollider3D): Vec3 {
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
    return summitSamples.minByOrNull {
        squaredHorizontalDistance(it, Vec3(centerX, it.y, centerZ))
    } ?: groundPosition(
        terrain = terrain,
        x = centerX,
        z = centerZ,
        fallbackY = highestY
    )
}
