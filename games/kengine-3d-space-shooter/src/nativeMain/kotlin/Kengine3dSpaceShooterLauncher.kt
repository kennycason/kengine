import com.kengine.createGameContext
import com.kengine.file.File
import com.kengine.graphics.Color
import com.kengine.hooks.context.useContext
import com.kengine.input.controller.ControllerInputEventSubscriber
import com.kengine.input.controller.controls.Buttons
import com.kengine.log.Logger
import com.kengine.math.Vec3
import com.kengine.sdl.RenderBackend
import com.kengine.three.CubeFaceColors
import com.kengine.three.DirectionalLight3D
import com.kengine.three.GpuContext
import com.kengine.three.GpuMesh
import com.kengine.three.GpuTexture
import com.kengine.three.LitMeshRenderer3D
import com.kengine.three.MeshRenderer3D
import com.kengine.three.ObjMeshLoadOptions
import com.kengine.three.ObjMeshLoader
import com.kengine.three.PerspectiveCamera
import com.kengine.three.PrimitiveRenderer3D
import com.kengine.three.TexturedGpuMesh
import com.kengine.three.TexturedMeshRenderer3D
import com.kengine.three.Transform3D
import com.kengine.three.Vertex3D
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import sdl3.SDL_GetTicks
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TERRAIN_Y = -1.55
private const val BASE_WORLD_SPEED = 2.55
private const val WINDOW_WIDTH = 960
private const val WINDOW_HEIGHT = 540
private const val PLAYER_MIN_X = -7.2
private const val PLAYER_MAX_X = 6.8
private const val PLAYER_MIN_Y = TERRAIN_Y + 0.78
private const val PLAYER_MAX_Y = TERRAIN_Y + 4.6
private const val PLAYER_START_Y = TERRAIN_Y + 2.1
private const val AIM_MAX_YAW = 0.56
private const val AIM_MAX_PITCH = 0.42
private const val BOSS_SPAWN_DISTANCE = 55.0
private const val MAX_HEALTH = 180
private const val LEFT_STICK_X_AXIS = 0
private const val LEFT_STICK_Y_AXIS = 1
private const val RIGHT_STICK_X_AXIS = 2
private const val RIGHT_STICK_Y_AXIS = 3
private const val L2_TRIGGER_AXIS = 4
private const val R2_TRIGGER_AXIS = 5
private const val CONTROLLER_AXIS_COUNT = 6
private const val TRIGGER_PRESSED = 0.45
private const val CONTROLLER_DEADZONE = 0.22
private const val CONTROLLER_CALIBRATION_SECONDS = 0.75

private enum class WeaponType {
    Spread,
    Homing,
    Charge
}

private data class Turret(
    var x: Double,
    var y: Double,
    var z: Double,
    var cooldown: Double,
    var hp: Int = 3
)

private data class Projectile(
    var x: Double,
    var y: Double,
    var z: Double,
    var vx: Double,
    var vy: Double,
    var vz: Double,
    val fromPlayer: Boolean,
    var ttl: Double,
    val damage: Int = 1,
    val scale: Double = 1.0,
    val variant: Int = 0,
    val homingStrength: Double = 0.0,
    val weapon: WeaponType? = null,
    val piercing: Boolean = false
)

private data class Boss(
    var active: Boolean = false,
    var defeated: Boolean = false,
    var wave: Int = 0,
    var nextSpawnDistance: Double = BOSS_SPAWN_DISTANCE,
    var x: Double = 0.0,
    var y: Double = 0.85,
    var z: Double = -28.0,
    var hp: Int = 120,
    var maxHp: Int = 120,
    var cooldown: Double = 0.9,
    var hitFlashSeconds: Double = 0.0,
    var scaleX: Double = 1.75,
    var scaleY: Double = 1.2,
    var scaleZ: Double = 1.75,
    var motionPhase: Double = 0.0
)

private data class PowerUp(
    var x: Double,
    var y: Double,
    var z: Double,
    val type: PowerUpType
)

private data class TerrainStrip(
    var z: Double,
    val xOffset: Double
)

private data class ShipPose(
    val position: Vec3,
    val rotation: Vec3,
    val forward: Vec3,
    val muzzle: Vec3
)

private enum class PowerUpType {
    Life,
    Beam
}

@OptIn(ExperimentalForeignApi::class)
fun main() {
    createGameContext(
        title = "Kengine - 3D Space Shooter",
        width = WINDOW_WIDTH,
        height = WINDOW_HEIGHT,
        logLevel = Logger.Level.INFO,
        renderBackend = RenderBackend.SDL_GPU_3D
    ) {
        useContext(GpuContext.create(sdl), cleanup = true) {
            val camera = PerspectiveCamera(
                position = Vec3(0.0, 0.28, 0.0),
                fovDegrees = 63f,
                near = 0.1f,
                far = 80f
            )
            val meshRenderer = MeshRenderer3D(this)
            val litRenderer = LitMeshRenderer3D(this)
            val texturedRenderer = TexturedMeshRenderer3D(this)
            val hud = PrimitiveRenderer3D(this)

            val shipModel = ObjMeshLoader.loadLit(
                gpu = this,
                assetPath = resolveShooterAsset("models/kenney-space-kit/craft_racer.obj"),
                options = ObjMeshLoadOptions(
                    targetSize = 1.25,
                    defaultColor = Color.fromHex("d8e3f0")
                )
            )
            val shipLight = DirectionalLight3D(
                direction = Vec3(-0.35, -0.72, -0.48),
                color = Color.fromHex("ffffff"),
                ambientStrength = 0.42f,
                diffuseStrength = 0.86f
            )
            val turretMeshes = listOf(
                createTurretMesh(this, 0f),
                createTurretMesh(this, 70f),
                createTurretMesh(this, 140f),
                createTurretMesh(this, 215f)
            )
            val shotMesh = GpuMesh.sphere(this, radius = 0.22, color = Color.fromHex("6dfffb"), rings = 10, segments = 16)
            val chargeShotMesh = GpuMesh.sphere(this, radius = 0.24, color = Color.fromHex("ff8a24"), rings = 12, segments = 18)
            val enemyShotMeshes = listOf(
                GpuMesh.sphere(this, radius = 0.2, color = Color.fromHex("ff2f68"), rings = 8, segments = 14),
                GpuMesh.sphere(this, radius = 0.18, color = Color.fromHex("f0c84b"), rings = 8, segments = 14),
                GpuMesh.sphere(this, radius = 0.24, color = Color.fromHex("9d5cff"), rings = 8, segments = 14),
                GpuMesh.sphere(this, radius = 0.16, color = Color.fromHex("5dffcb"), rings = 8, segments = 14)
            )
            val powerSphereMesh = GpuMesh.sphere(this, radius = 0.28, color = Color.fromHex("7bff3b"), rings = 10, segments = 16)
            val aimBeadMesh = GpuMesh.sphere(this, radius = 0.2, color = Color.fromHex("ff2638"), rings = 8, segments = 14)
            val bossMeshes = listOf(
                createBossMesh(this, 0f),
                createBossMesh(this, 75f),
                createBossMesh(this, 150f),
                createBossMesh(this, 235f)
            )
            val bossFlashMesh = createBossMesh(this, 0f, flash = true)
            val terrainMeshes = listOf(
                createTerrainMesh(this, 0f),
                createTerrainMesh(this, 55f),
                createTerrainMesh(this, 120f),
                createTerrainMesh(this, 205f)
            )
            val powerCubeMesh = TexturedGpuMesh.cube(this)
            val checkerboard = GpuTexture.checkerboard(this)

            val turrets = mutableListOf(
                Turret(-2.45, TERRAIN_Y + 0.58, -8.0, 0.6),
                Turret(2.1, TERRAIN_Y + 0.72, -12.5, 1.2),
                Turret(0.05, TERRAIN_Y + 0.64, -17.0, 1.8)
            )
            val terrain = mutableListOf(
                TerrainStrip(-4.8, -0.45),
                TerrainStrip(-8.9, 0.62),
                TerrainStrip(-13.0, -0.78),
                TerrainStrip(-17.1, 0.35),
                TerrainStrip(-21.2, -0.22)
            )
            val projectiles = mutableListOf<Projectile>()
            val powerUps = mutableListOf<PowerUp>()
            val boss = Boss()

            var playerX = 0.0
            var playerY = PLAYER_START_Y
            var health = MAX_HEALTH
            var beamLevel = 1
            var lifePickupLevel = 1
            var score = 0
            var fireCooldown = 0.0
            var aimYaw = 0.0
            var aimPitch = 0.0
            var chargeSeconds = 0.0
            var previousSpacePressed = false
            var previousCPressed = false
            val unlockedWeapons = mutableListOf(WeaponType.Spread)
            var weaponIndex = 0
            var levelDistance = 0.0
            var previousTicks = SDL_GetTicks()
            var controllerNeutral: FloatArray? = null
            var calibratedControllerId: UInt? = null
            var controllerCalibrationUntil = 0.0

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
                    val rightStickX = -normalizedControllerAxis(
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
                    val l2Pressed = isTriggerPressed(
                        controllerState,
                        controllerId,
                        controllerNeutral,
                        L2_TRIGGER_AXIS,
                        isControllerCalibrating
                    )
                    val r2Pressed = isTriggerPressed(
                        controllerState,
                        controllerId,
                        controllerNeutral,
                        R2_TRIGGER_AXIS,
                        isControllerCalibrating
                    )
                    val speedMultiplier = speedMultiplier(
                        isAirBrakePressed = keyboardState.isCapsLockPressed() ||
                            controllerState.isButtonPressed(Buttons.L1),
                        isBoostPressed = keyboardState.isSemicolonPressed() ||
                            controllerState.isButtonPressed(Buttons.R1)
                    )
                    val worldSpeed = BASE_WORLD_SPEED * speedMultiplier
                    levelDistance += worldSpeed * deltaSeconds
                    val cPressed = keyboardState.isCPressed() || l2Pressed
                    if (cPressed && !previousCPressed) {
                        weaponIndex = (weaponIndex + 1) % unlockedWeapons.size
                        chargeSeconds = 0.0
                    }
                    previousCPressed = cPressed

                    val moveSpeed = 3.65 * deltaSeconds
                    val moveInputX = (axis(keyboardState.isDPressed(), keyboardState.isAPressed()) + leftStickX)
                        .coerceIn(-1.0, 1.0)
                    val moveInputY = (axis(keyboardState.isWPressed(), keyboardState.isSPressed()) + leftStickY)
                        .coerceIn(-1.0, 1.0)
                    playerX += moveInputX * moveSpeed
                    playerY += moveInputY * moveSpeed * 0.8
                    playerX = playerX.coerceIn(PLAYER_MIN_X, PLAYER_MAX_X)
                    playerY = playerY.coerceIn(PLAYER_MIN_Y, PLAYER_MAX_Y)

                    val yawInput = (axis(keyboardState.isLPressed(), keyboardState.isJPressed()) + rightStickX)
                        .coerceIn(-1.0, 1.0)
                    val pitchInput = (axis(keyboardState.isIPressed(), keyboardState.isKPressed()) + rightStickY)
                        .coerceIn(-1.0, 1.0)
                    val aimSpeed = 1.45 * deltaSeconds
                    aimYaw = updateAimAxis(
                        current = aimYaw,
                        input = yawInput,
                        speed = aimSpeed,
                        max = AIM_MAX_YAW,
                        deltaSeconds = deltaSeconds
                    )
                    aimPitch = updateAimAxis(
                        current = aimPitch,
                        input = pitchInput,
                        speed = aimSpeed,
                        max = AIM_MAX_PITCH,
                        deltaSeconds = deltaSeconds
                    )

                    fireCooldown = (fireCooldown - deltaSeconds).coerceAtLeast(0.0)
                    val firePressed = keyboardState.isSpacePressed() || r2Pressed
                    val activeWeapon = unlockedWeapons[weaponIndex]
                    val shipPose = shipPose(playerX, playerY, aimYaw, aimPitch, elapsedSeconds)

                    if (activeWeapon == WeaponType.Charge) {
                        if (firePressed) {
                            chargeSeconds = (chargeSeconds + deltaSeconds).coerceAtMost(1.55)
                        }
                        if (!firePressed && previousSpacePressed && fireCooldown <= 0.0) {
                            firePlayerWeapon(
                                projectiles = projectiles,
                                weapon = activeWeapon,
                                muzzle = shipPose.muzzle,
                                baseForward = shipPose.forward,
                                shipRenderYaw = shipPose.rotation.y,
                                shipRenderPitch = shipPose.rotation.x,
                                beamLevel = beamLevel,
                                chargeSeconds = chargeSeconds
                            )
                            fireCooldown = 0.28
                            chargeSeconds = 0.0
                        }
                    } else {
                        chargeSeconds = 0.0
                        if (firePressed && fireCooldown <= 0.0) {
                            firePlayerWeapon(
                                projectiles = projectiles,
                                weapon = activeWeapon,
                                muzzle = shipPose.muzzle,
                                baseForward = shipPose.forward,
                                shipRenderYaw = shipPose.rotation.y,
                                shipRenderPitch = shipPose.rotation.x,
                                beamLevel = beamLevel,
                                chargeSeconds = chargeSeconds
                            )
                            fireCooldown = weaponFireCooldown(activeWeapon, beamLevel)
                        }
                    }
                    previousSpacePressed = firePressed

                    terrain.forEach { strip ->
                        strip.z += worldSpeed * deltaSeconds
                        if (strip.z > -2.0) {
                            strip.z -= 19.5
                        }
                    }

                    updateBoss(
                        boss = boss,
                        projectiles = projectiles,
                        target = shipPose.position,
                        levelDistance = levelDistance,
                        worldSpeed = worldSpeed,
                        deltaSeconds = deltaSeconds,
                        elapsedSeconds = elapsedSeconds
                    )

                    updateTurrets(
                        turrets = turrets,
                        projectiles = projectiles,
                        target = shipPose.position,
                        worldSpeed = worldSpeed,
                        wave = boss.wave,
                        deltaSeconds = deltaSeconds,
                        elapsedSeconds = elapsedSeconds
                    )
                    updateProjectiles(
                        projectiles = projectiles,
                        turrets = turrets,
                        boss = boss,
                        powerUps = powerUps,
                        player = shipPose.position,
                        deltaSeconds = deltaSeconds,
                        score = score,
                        onScoreChanged = { score = it },
                        onDamage = {
                            health = (health - it).coerceAtLeast(0)
                            if (health == 0) {
                                health = MAX_HEALTH
                                beamLevel = 1
                                playerX = 0.0
                                playerY = PLAYER_START_Y
                                true
                            } else {
                                false
                            }
                        }
                    )
                    updatePowerUps(
                        powerUps = powerUps,
                        player = shipPose.position,
                        worldSpeed = worldSpeed,
                        deltaSeconds = deltaSeconds,
                        onLife = {
                            health = (health + 30 + lifePickupLevel * 14).coerceAtMost(MAX_HEALTH)
                            lifePickupLevel = (lifePickupLevel + 1).coerceAtMost(9)
                        },
                        onBeam = {
                            beamLevel = (beamLevel + 1).coerceAtMost(12)
                            unlockNextWeapon(unlockedWeapons)
                            weaponIndex = weaponIndex.coerceAtMost(unlockedWeapons.lastIndex)
                        }
                    )

                    val worldHue = ((levelDistance * 3.2 + boss.wave * 58.0) % 360.0).toFloat()
                    val skyColor = evolvingSkyColor(worldHue, elapsedSeconds)
                    val red = skyColor.r.toFloat() / 255f
                    val green = skyColor.g.toFloat() / 255f
                    val blue = skyColor.b.toFloat() / 255f

                    render(red, green, blue, 1f, enableDepth = true) { frame ->
                        val time = elapsedSeconds.toFloat()

                        terrain.forEachIndexed { index, strip ->
                            meshRenderer.draw(
                                frame = frame,
                                mesh = terrainMeshes[(boss.wave + index) % terrainMeshes.size],
                                transform = Transform3D(
                                    position = Vec3(strip.xOffset, TERRAIN_Y, strip.z),
                                    rotation = Vec3(0.0, sin(elapsedSeconds * 0.35 + index) * 0.05, 0.0),
                                    scale = Vec3(1.0, 1.0, 1.0)
                                ),
                                camera = camera
                            )
                        }

                        turrets.forEachIndexed { index, turret ->
                            val turretMesh = turretMeshes[boss.wave % turretMeshes.size]
                            meshRenderer.draw(
                                frame = frame,
                                mesh = turretMesh,
                                transform = Transform3D(
                                    position = Vec3(turret.x, turret.y, turret.z),
                                    rotation = Vec3(0.0, time * 0.45 + index, 0.0),
                                    scale = Vec3(0.45, 0.42, 0.45)
                                ),
                                camera = camera
                            )
                            meshRenderer.draw(
                                frame = frame,
                                mesh = turretMesh,
                                transform = Transform3D(
                                    position = Vec3(turret.x, turret.y + 0.34, turret.z - 0.12),
                                    rotation = Vec3(0.0, time * 0.85 + index, 0.0),
                                    scale = Vec3(0.16, 0.18, 0.62)
                                ),
                                camera = camera
                            )
                        }

                        if (boss.active && !boss.defeated) {
                            val activeBossMesh = if (boss.hitFlashSeconds > 0.0) {
                                bossFlashMesh
                            } else {
                                bossMeshes[boss.wave % bossMeshes.size]
                            }
                            meshRenderer.draw(
                                frame = frame,
                                mesh = activeBossMesh,
                                transform = Transform3D(
                                    position = Vec3(boss.x, boss.y, boss.z),
                                    rotation = Vec3(
                                        sin(elapsedSeconds * 0.45) * 0.12,
                                        time * 0.28,
                                        sin(elapsedSeconds * 0.7) * 0.1
                                    ),
                                    scale = Vec3(boss.scaleX, boss.scaleY, boss.scaleZ)
                                ),
                                camera = camera
                            )
                        }

                        powerUps.forEach { powerUp ->
                            if (powerUp.type == PowerUpType.Beam) {
                                texturedRenderer.draw(
                                    frame = frame,
                                    mesh = powerCubeMesh,
                                    texture = checkerboard,
                                    transform = Transform3D(
                                        position = Vec3(powerUp.x, powerUp.y, powerUp.z),
                                        rotation = Vec3(time.toDouble(), (time * 1.8f).toDouble(), 0.3),
                                        scale = Vec3(0.38, 0.38, 0.38)
                                    ),
                                    camera = camera
                                )
                            } else {
                                meshRenderer.draw(
                                    frame = frame,
                                    mesh = powerSphereMesh,
                                    transform = Transform3D(
                                        position = Vec3(powerUp.x, powerUp.y, powerUp.z),
                                        rotation = Vec3(0.0, time.toDouble(), 0.0),
                                        scale = Vec3(1.0, 1.0, 1.0)
                                    ),
                                    camera = camera
                                )
                            }
                        }

                        projectiles.forEach { projectile ->
                            val projectileScale = if (projectile.fromPlayer) {
                                0.68 * projectile.scale
                            } else {
                                0.88 * projectile.scale
                            }
                            val projectileMesh = when {
                                !projectile.fromPlayer -> enemyShotMeshes[projectile.variant % enemyShotMeshes.size]
                                projectile.weapon == WeaponType.Charge -> chargeShotMesh
                                else -> shotMesh
                            }
                            meshRenderer.draw(
                                frame = frame,
                                mesh = projectileMesh,
                                transform = Transform3D(
                                    position = Vec3(projectile.x, projectile.y, projectile.z),
                                    rotation = Vec3(time.toDouble(), 0.0, 0.0),
                                    scale = Vec3(projectileScale, projectileScale, projectileScale)
                                ),
                                camera = camera
                            )
                        }

                        if (activeWeapon == WeaponType.Charge && chargeSeconds > 0.0) {
                            val chargeScale = 0.72 + chargeSeconds.coerceIn(0.0, 1.55) * 0.68
                            meshRenderer.draw(
                                frame = frame,
                                mesh = chargeShotMesh,
                                transform = Transform3D(
                                    position = add(
                                        shipPose.muzzle,
                                        scale(shipPose.forward, 0.38 + chargeSeconds * 0.08)
                                    ),
                                    rotation = Vec3(time.toDouble() * 1.4, time.toDouble() * 0.9, 0.0),
                                    scale = Vec3(chargeScale, chargeScale, chargeScale)
                                ),
                                camera = camera
                            )
                        }

                        meshRenderer.draw(
                            frame = frame,
                            mesh = aimBeadMesh,
                            transform = Transform3D(
                                position = add(
                                    shipPose.muzzle,
                                    scale(shipPose.forward, aimBeadDistance(activeWeapon, chargeSeconds))
                                ),
                                rotation = Vec3(time.toDouble(), time.toDouble() * 0.6, 0.0),
                                scale = Vec3(0.52, 0.52, 0.52)
                            ),
                            camera = camera
                        )

                        litRenderer.draw(
                            frame = frame,
                            mesh = shipModel,
                            transform = Transform3D(
                                position = shipPose.position,
                                rotation = shipPose.rotation
                            ),
                            camera = camera,
                            light = shipLight.copy(
                                color = Color.fromHSV((worldHue + 80f) % 360f, 0.34f, 1.0f)
                            )
                        )

                        drawHud(hud, frame, health, beamLevel, score, activeWeapon, chargeSeconds, boss)
                    }

                    mouse.mouse.clearFrameState()
                    SDL_Delay(16u)
                }
            } finally {
                checkerboard.cleanup()
                powerCubeMesh.cleanup()
                terrainMeshes.forEach { it.cleanup() }
                powerSphereMesh.cleanup()
                aimBeadMesh.cleanup()
                bossFlashMesh.cleanup()
                bossMeshes.forEach { it.cleanup() }
                enemyShotMeshes.forEach { it.cleanup() }
                chargeShotMesh.cleanup()
                shotMesh.cleanup()
                turretMeshes.forEach { it.cleanup() }
                shipModel.cleanup()
                hud.cleanup()
                texturedRenderer.cleanup()
                litRenderer.cleanup()
                meshRenderer.cleanup()
            }
        }
    }
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

private fun isTriggerPressed(
    controller: ControllerInputEventSubscriber,
    controllerId: UInt?,
    neutral: FloatArray?,
    axisIndex: Int,
    isCalibrating: Boolean
): Boolean {
    if (controllerId == null || isCalibrating) {
        return false
    }

    val rawValue = controller.getAxisValue(controllerId, axisIndex).toDouble().coerceIn(-1.0, 1.0)
    val neutralValue = neutral?.getOrNull(axisIndex)?.toDouble()?.coerceIn(-1.0, 1.0) ?: 0.0
    return when {
        neutralValue <= -TRIGGER_PRESSED -> rawValue > neutralValue + TRIGGER_PRESSED
        neutralValue >= TRIGGER_PRESSED -> rawValue < neutralValue - TRIGGER_PRESSED
        else -> rawValue > TRIGGER_PRESSED
    }
}

private fun updateAimAxis(
    current: Double,
    input: Double,
    speed: Double,
    max: Double,
    deltaSeconds: Double
): Double {
    if (input != 0.0) {
        return (current + input * speed).coerceIn(-max, max)
    }

    val recenterBlend = (deltaSeconds * 3.2).coerceIn(0.0, 1.0)
    return current + (0.0 - current) * recenterBlend
}

private fun speedMultiplier(isAirBrakePressed: Boolean, isBoostPressed: Boolean): Double {
    var multiplier = 1.0
    if (isAirBrakePressed) {
        multiplier *= 0.38
    }
    if (isBoostPressed) {
        multiplier *= 1.72
    }
    return multiplier
}

private fun shipVisualPosition(playerX: Double, playerY: Double): Vec3 {
    return Vec3(
        x = playerX * 0.58,
        y = -0.62 + (playerY - PLAYER_START_Y) * 0.54,
        z = -1.58
    )
}

private fun shipPitch(playerY: Double, aimPitch: Double): Double {
    val climb = ((playerY - PLAYER_START_Y) / (PLAYER_MAX_Y - PLAYER_START_Y)).coerceIn(-1.0, 1.0)
    return 0.42 - climb * 0.42 + aimPitch * 0.75
}

private fun shipVisualYaw(playerX: Double, aimYaw: Double): Double {
    return aimYaw * 0.9 + playerX * -0.06
}

private fun shipPose(
    playerX: Double,
    playerY: Double,
    aimYaw: Double,
    aimPitch: Double,
    elapsedSeconds: Double
): ShipPose {
    val position = shipVisualPosition(playerX, playerY)
    val rotation = Vec3(
        x = shipPitch(playerY, aimPitch) + sin(elapsedSeconds * 4.0) * 0.04,
        y = shipVisualYaw(playerX, aimYaw),
        z = aimYaw * -0.32 + playerX * -0.18
    )
    val forward = shipForward(rotation.y, rotation.x)

    return ShipPose(
        position = position,
        rotation = rotation,
        forward = forward,
        muzzle = add(position, scale(forward, 0.82))
    )
}

private fun shipForward(renderYaw: Double, renderPitch: Double): Vec3 {
    val horizontal = cos(renderPitch)
    return Vec3(
        x = -sin(renderYaw) * horizontal,
        y = sin(renderPitch),
        z = -cos(renderYaw) * horizontal
    )
}

private fun evolvingSkyColor(worldHue: Float, elapsedSeconds: Double): Color {
    val pulse = ((sin(elapsedSeconds * 0.42) + 1.0) * 0.5).toFloat()
    return Color.fromHSV(
        h = worldHue,
        saturation = 0.78f,
        value = 0.095f + pulse * 0.055f
    )
}

private fun aimBeadDistance(weapon: WeaponType, chargeSeconds: Double): Double {
    return when (weapon) {
        WeaponType.Charge -> 1.45 + chargeSeconds.coerceIn(0.0, 1.55) * 0.38
        WeaponType.Homing -> 1.95
        WeaponType.Spread -> 1.72
    }
}

private fun weaponFireCooldown(weapon: WeaponType, beamLevel: Int): Double {
    return when (weapon) {
        WeaponType.Spread -> (0.29 - beamLevel * 0.018).coerceAtLeast(0.07)
        WeaponType.Homing -> (0.38 - beamLevel * 0.012).coerceAtLeast(0.18)
        WeaponType.Charge -> 0.28
    }
}

private fun unlockNextWeapon(unlockedWeapons: MutableList<WeaponType>) {
    val nextWeapon = WeaponType.entries.firstOrNull { it !in unlockedWeapons }
    if (nextWeapon != null) {
        unlockedWeapons += nextWeapon
    }
}

private fun add(a: Vec3, b: Vec3): Vec3 {
    return Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
}

private fun scale(value: Vec3, amount: Double): Vec3 {
    return Vec3(value.x * amount, value.y * amount, value.z * amount)
}

private fun direction(from: Vec3, to: Vec3, speed: Double): Vec3 {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val dz = to.z - from.z
    val length = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001)
    return Vec3(dx / length * speed, dy / length * speed, dz / length * speed)
}

private fun pseudoRandom(seed: Int, salt: Int): Double {
    return abs(sin(seed * 12.9898 + salt * 78.233) * 43758.5453) % 1.0
}

private fun firePlayerWeapon(
    projectiles: MutableList<Projectile>,
    weapon: WeaponType,
    muzzle: Vec3,
    baseForward: Vec3,
    shipRenderYaw: Double,
    shipRenderPitch: Double,
    beamLevel: Int,
    chargeSeconds: Double
) {
    when (weapon) {
        WeaponType.Spread -> {
            val fan = when {
                beamLevel <= 1 -> listOf(0.0)
                beamLevel == 2 -> listOf(-0.12, 0.12)
                beamLevel == 3 -> listOf(-0.2, 0.0, 0.2)
                beamLevel == 4 -> listOf(-0.27, -0.09, 0.09, 0.27)
                else -> listOf(-0.34, -0.17, 0.0, 0.17, 0.34)
            }
            val pitchRows = when {
                beamLevel >= 8 -> listOf(-0.13, 0.0, 0.13)
                beamLevel >= 6 -> listOf(-0.09, 0.09)
                else -> listOf(0.0)
            }
            val damage = 1 + beamLevel / 4

            pitchRows.forEach { pitchOffset ->
                fan.forEach { yawOffset ->
                    val direction = shipForward(
                        renderYaw = shipRenderYaw - yawOffset,
                        renderPitch = shipRenderPitch + pitchOffset
                    )
                    projectiles += Projectile(
                        x = muzzle.x,
                        y = muzzle.y,
                        z = muzzle.z,
                        vx = direction.x * 8.9,
                        vy = direction.y * 8.9,
                        vz = direction.z * 8.9,
                        fromPlayer = true,
                        ttl = 2.7,
                        damage = damage,
                        scale = 0.92 + beamLevel.coerceAtMost(8) * 0.035
                    )
                }
            }
        }

        WeaponType.Homing -> {
            projectiles += Projectile(
                x = muzzle.x,
                y = muzzle.y,
                z = muzzle.z,
                vx = baseForward.x * 7.7,
                vy = baseForward.y * 7.7,
                vz = baseForward.z * 7.7,
                fromPlayer = true,
                ttl = 3.5,
                damage = 1 + beamLevel / 3,
                scale = 0.9 + beamLevel.coerceAtMost(8) * 0.04,
                homingStrength = 5.6 + beamLevel * 0.32
            )
        }

        WeaponType.Charge -> {
            val charge = chargeSeconds.coerceIn(0.18, 1.55)
            val projectileScale = 1.55 + charge * 1.55
            projectiles += Projectile(
                x = muzzle.x,
                y = muzzle.y,
                z = muzzle.z,
                vx = baseForward.x * 6.85,
                vy = baseForward.y * 6.85,
                vz = baseForward.z * 6.85,
                fromPlayer = true,
                ttl = 4.2,
                damage = 1,
                scale = projectileScale,
                weapon = WeaponType.Charge,
                piercing = true
            )
        }
    }
}

private fun updateBoss(
    boss: Boss,
    projectiles: MutableList<Projectile>,
    target: Vec3,
    levelDistance: Double,
    worldSpeed: Double,
    deltaSeconds: Double,
    elapsedSeconds: Double
) {
    if (!boss.active && levelDistance >= boss.nextSpawnDistance) {
        boss.wave += 1
        boss.active = true
        boss.defeated = false
        boss.x = 0.0
        boss.y = 0.9 + boss.wave * 0.12
        boss.z = -30.0
        boss.maxHp = 48 + boss.wave * 18
        boss.hp = boss.maxHp
        boss.cooldown = 1.2
        boss.hitFlashSeconds = 0.0
        boss.scaleX = 1.3 + pseudoRandom(boss.wave, 1) * 1.55
        boss.scaleY = 0.92 + pseudoRandom(boss.wave, 2) * 0.88
        boss.scaleZ = 1.25 + pseudoRandom(boss.wave, 3) * 1.7
        boss.motionPhase = pseudoRandom(boss.wave, 4) * 6.28
    }

    if (!boss.active || boss.defeated) {
        return
    }

    boss.hitFlashSeconds = (boss.hitFlashSeconds - deltaSeconds).coerceAtLeast(0.0)
    boss.z = (boss.z + worldSpeed * (0.18 + boss.wave * 0.012) * deltaSeconds).coerceAtMost(-12.0)
    boss.x = sin(elapsedSeconds * (0.34 + boss.wave * 0.025) + boss.motionPhase) * (1.7 + boss.scaleX * 0.52)
    boss.y = 0.92 + boss.wave * 0.08 + sin(elapsedSeconds * 0.62 + boss.motionPhase) * (0.36 + boss.scaleY * 0.12)
    boss.cooldown -= deltaSeconds

    if (boss.cooldown <= 0.0) {
        fireEnemyPattern(
            projectiles = projectiles,
            source = Vec3(boss.x, boss.y - 0.15, boss.z + 0.65),
            target = target,
            pattern = boss.wave % 4,
            wave = boss.wave,
            salt = 7
        )
        boss.cooldown = (1.12 - boss.wave * 0.035).coerceAtLeast(0.52)
    }
}

private fun fireEnemyPattern(
    projectiles: MutableList<Projectile>,
    source: Vec3,
    target: Vec3,
    pattern: Int,
    wave: Int,
    salt: Int
) {
    val base = direction(source, target, 1.0)

    fun addShot(
        vxOffset: Double,
        vyOffset: Double,
        speed: Double,
        damage: Int,
        scale: Double,
        variant: Int,
        ttl: Double = 4.0
    ) {
        projectiles += Projectile(
            x = source.x,
            y = source.y,
            z = source.z,
            vx = base.x * speed + vxOffset,
            vy = base.y * speed + vyOffset,
            vz = base.z * speed,
            fromPlayer = false,
            ttl = ttl,
            damage = damage,
            scale = scale,
            variant = variant
        )
    }

    when (pattern) {
        0 -> addShot(0.0, 0.0, 3.0 + wave * 0.05, 1, 0.75, wave + salt)
        1 -> addShot(0.0, 0.0, 4.75 + wave * 0.06, 1, 0.52, wave + salt + 1, ttl = 2.8)
        2 -> addShot(0.0, 0.0, 2.25 + wave * 0.03, 2, 1.32, wave + salt + 2, ttl = 4.8)
        else -> {
            val spread = if (wave >= 3) listOf(-1.2, -0.6, 0.0, 0.6, 1.2) else listOf(-0.72, 0.0, 0.72)
            spread.forEachIndexed { index, offset ->
                addShot(
                    vxOffset = offset,
                    vyOffset = sin((wave + salt + index) * 0.9) * 0.28,
                    speed = 3.05,
                    damage = 1,
                    scale = 0.66,
                    variant = wave + salt + index + 3,
                    ttl = 3.6
                )
            }
        }
    }
}

private fun updateTurrets(
    turrets: MutableList<Turret>,
    projectiles: MutableList<Projectile>,
    target: Vec3,
    worldSpeed: Double,
    wave: Int,
    deltaSeconds: Double,
    elapsedSeconds: Double
) {
    turrets.forEachIndexed { index, turret ->
        turret.z += worldSpeed * deltaSeconds
        turret.cooldown -= deltaSeconds

        if (turret.cooldown <= 0.0 && turret.z < -3.3) {
            val sourceX = turret.x
            val sourceY = turret.y + 0.24
            val sourceZ = turret.z + 0.2
            fireEnemyPattern(
                projectiles = projectiles,
                source = Vec3(sourceX, sourceY, sourceZ),
                target = target,
                pattern = (index + wave) % 4,
                wave = wave,
                salt = index
            )
            turret.cooldown = 1.25 + index * 0.28
        }

        if (turret.z > -1.25 || turret.hp <= 0) {
            val lane = ((index % 3) - 1) * 1.95
            turret.x = lane + sin(elapsedSeconds * 0.73 + index) * 0.62
            turret.y = TERRAIN_Y + 0.56 + abs(cos(elapsedSeconds + index)) * 0.25
            turret.z = -18.0 - index * 4.5
            turret.cooldown = 0.65 + index * 0.35
            turret.hp = 3
        }
    }
}

private fun updateProjectiles(
    projectiles: MutableList<Projectile>,
    turrets: MutableList<Turret>,
    boss: Boss,
    powerUps: MutableList<PowerUp>,
    player: Vec3,
    deltaSeconds: Double,
    score: Int,
    onScoreChanged: (Int) -> Unit,
    onDamage: (Int) -> Boolean
) {
    var nextScore = score

    for (index in projectiles.lastIndex downTo 0) {
        val projectile = projectiles[index]
        if (projectile.fromPlayer && projectile.homingStrength > 0.0) {
            updateHomingProjectile(projectile, turrets, boss, deltaSeconds)
        }

        projectile.x += projectile.vx * deltaSeconds
        projectile.y += projectile.vy * deltaSeconds
        projectile.z += projectile.vz * deltaSeconds
        projectile.ttl -= deltaSeconds

        var remove = projectile.ttl <= 0.0 || projectile.z < -35.0 || projectile.z > 1.0

        if (!remove && projectile.fromPlayer) {
            turrets.forEach { turret ->
                if (!remove && distanceSquared(projectile.x, projectile.y, projectile.z, turret.x, turret.y, turret.z) < 0.48) {
                    turret.hp -= projectile.damage
                    remove = !projectile.piercing
                    if (turret.hp <= 0) {
                        nextScore += 1
                        if (nextScore % 2 == 0) {
                            powerUps += PowerUp(
                                x = turret.x,
                                y = turret.y + 0.32,
                                z = turret.z,
                                type = if (nextScore % 4 == 0) PowerUpType.Life else PowerUpType.Beam
                            )
                        }
                    }
                }
            }
        }

        if (!remove && projectile.fromPlayer && boss.active && !boss.defeated) {
            if (distanceSquared(projectile.x, projectile.y, projectile.z, boss.x, boss.y, boss.z) < 3.1) {
                boss.hp -= projectile.damage
                boss.hitFlashSeconds = 0.11
                remove = !projectile.piercing
                if (boss.hp <= 0) {
                    boss.defeated = true
                    boss.active = false
                    boss.nextSpawnDistance += 58.0 + boss.wave * 18.0
                    nextScore += 15
                    powerUps += PowerUp(
                        x = boss.x,
                        y = boss.y,
                        z = boss.z + 0.5,
                        type = PowerUpType.Beam
                    )
                }
            }
        }

        if (!remove && !projectile.fromPlayer) {
            if (intersectsShipHitbox(projectile, player)) {
                if (onDamage(projectile.damage)) {
                    projectiles.clear()
                    return
                }
                remove = true
            }
        }

        if (remove) {
            projectiles.removeAt(index)
        }
    }

    if (nextScore != score) {
        onScoreChanged(nextScore)
    }
}

private fun intersectsShipHitbox(projectile: Projectile, player: Vec3): Boolean {
    val radiusBoost = projectile.scale * 0.035
    val rx = 0.26 + radiusBoost
    val ry = 0.15 + radiusBoost
    val rz = 0.46 + radiusBoost
    val dx = (projectile.x - player.x) / rx
    val dy = (projectile.y - player.y) / ry
    val dz = (projectile.z - player.z) / rz
    return dx * dx + dy * dy + dz * dz <= 1.0
}

private fun updateHomingProjectile(
    projectile: Projectile,
    turrets: List<Turret>,
    boss: Boss,
    deltaSeconds: Double
) {
    val turretTarget = turrets
        .filter { it.hp > 0 && it.z < projectile.z }
        .minByOrNull {
            distanceSquared(projectile.x, projectile.y, projectile.z, it.x, it.y, it.z)
        }
    val bossTarget = if (boss.active && !boss.defeated && boss.z < projectile.z) boss else null
    val target = when {
        turretTarget == null -> bossTarget?.let { Vec3(it.x, it.y, it.z) }
        bossTarget == null -> Vec3(turretTarget.x, turretTarget.y, turretTarget.z)
        distanceSquared(projectile.x, projectile.y, projectile.z, turretTarget.x, turretTarget.y, turretTarget.z) <
            distanceSquared(projectile.x, projectile.y, projectile.z, bossTarget.x, bossTarget.y, bossTarget.z) ->
            Vec3(turretTarget.x, turretTarget.y, turretTarget.z)
        else -> Vec3(bossTarget.x, bossTarget.y, bossTarget.z)
    } ?: return

    val dx = target.x - projectile.x
    val dy = target.y - projectile.y
    val dz = target.z - projectile.z
    val distance = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001)
    val speed = sqrt(
        projectile.vx * projectile.vx +
            projectile.vy * projectile.vy +
            projectile.vz * projectile.vz
    ).coerceAtLeast(0.001)
    val blend = (projectile.homingStrength * deltaSeconds).coerceIn(0.0, 1.0)

    projectile.vx += (dx / distance * speed - projectile.vx) * blend
    projectile.vy += (dy / distance * speed - projectile.vy) * blend
    projectile.vz += (dz / distance * speed - projectile.vz) * blend
}

private fun updatePowerUps(
    powerUps: MutableList<PowerUp>,
    player: Vec3,
    worldSpeed: Double,
    deltaSeconds: Double,
    onLife: () -> Unit,
    onBeam: () -> Unit
) {
    for (index in powerUps.lastIndex downTo 0) {
        val powerUp = powerUps[index]
        powerUp.z += worldSpeed * 0.8 * deltaSeconds
        powerUp.y += sin(powerUp.z * 1.7) * 0.003

        val collected = distanceSquared(powerUp.x, powerUp.y, powerUp.z, player.x, player.y, player.z) < 0.58
        if (collected) {
            if (powerUp.type == PowerUpType.Life) {
                onLife()
            } else {
                onBeam()
            }
        }

        if (collected || powerUp.z > -1.0) {
            powerUps.removeAt(index)
        }
    }
}

private fun drawHud(
    hud: PrimitiveRenderer3D,
    frame: com.kengine.three.GpuFrame,
    health: Int,
    beamLevel: Int,
    score: Int,
    activeWeapon: WeaponType,
    chargeSeconds: Double,
    boss: Boss
) {
    val healthWidth = health.toFloat() / MAX_HEALTH.toFloat() * 1.25f
    hud.quad(
        frame = frame,
        center = Vec3(-2.88, 1.48, -3.0),
        width = 1.34f,
        height = 0.18f,
        color = Color.fromHex("1b1630")
    )
    hud.quad(
        frame = frame,
        center = Vec3(-2.88 - (1.25f - healthWidth) * 0.5f, 1.48, -3.0),
        width = healthWidth,
        height = 0.12f,
        color = Color.fromHex("ff3b5f")
    )

    repeat(beamLevel) { index ->
        hud.quad(
            frame = frame,
            center = Vec3(-3.45 + index * 0.22, 1.23, -3.0),
            width = 0.16f,
            height = 0.16f,
            color = Color.fromHex("6dfffb"),
            rotationRadians = (score * 0.15f) + index * 0.45f
        )
    }

    val weaponColor = when (activeWeapon) {
        WeaponType.Spread -> Color.fromHex("6dfffb")
        WeaponType.Homing -> Color.fromHex("7bff3b")
        WeaponType.Charge -> Color.fromHex("f0c84b")
    }
    hud.quad(
        frame = frame,
        center = Vec3(3.22, 1.44, -3.0),
        width = 0.26f + (chargeSeconds.coerceIn(0.0, 1.55) * 0.16).toFloat(),
        height = 0.26f + (chargeSeconds.coerceIn(0.0, 1.55) * 0.16).toFloat(),
        color = weaponColor,
        rotationRadians = score * 0.08f
    )

    if (boss.active && !boss.defeated) {
        val bossWidth = (boss.hp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f) * 1.55f
        hud.quad(
            frame = frame,
            center = Vec3(0.0, 1.48, -3.0),
            width = 1.68f,
            height = 0.14f,
            color = Color.fromHex("241032")
        )
        hud.quad(
            frame = frame,
            center = Vec3((-(1.55f - bossWidth) * 0.5f).toDouble(), 1.48, -3.0),
            width = bossWidth,
            height = 0.09f,
            color = Color.fromHex("ff8a24")
        )
    }
}

private fun distanceSquared(
    ax: Double,
    ay: Double,
    az: Double,
    bx: Double,
    by: Double,
    bz: Double
): Double {
    val dx = ax - bx
    val dy = ay - by
    val dz = az - bz
    return dx * dx + dy * dy + dz * dz
}

private fun createTurretMesh(gpu: GpuContext, hueShift: Float): GpuMesh {
    fun shifted(hex: String): Color {
        return Color.applyHueShift(Color.fromHex(hex), hueShift)
    }

    return GpuMesh.cube(
        gpu,
        CubeFaceColors(
            negativeZ = shifted("47116b"),
            positiveZ = shifted("ff3b5f"),
            negativeX = shifted("171c55"),
            positiveX = shifted("ff8f2a"),
            positiveY = shifted("8dffef"),
            negativeY = shifted("27124e")
        )
    )
}

private fun createBossMesh(
    gpu: GpuContext,
    hueShift: Float = 0f,
    flash: Boolean = false
): GpuMesh {
    val core = Vec3(0.0, 0.0, 0.0)
    val nose = Vec3(0.0, 0.12, -1.65)
    val tail = Vec3(0.0, 0.0, 1.1)
    val top = Vec3(0.0, 0.82, -0.12)
    val belly = Vec3(0.0, -0.72, -0.05)
    val leftWing = Vec3(-2.05, 0.05, -0.12)
    val rightWing = Vec3(2.05, 0.05, -0.12)
    val leftFang = Vec3(-0.82, -0.25, -1.18)
    val rightFang = Vec3(0.82, -0.25, -1.18)
    val leftTail = Vec3(-1.15, 0.12, 0.86)
    val rightTail = Vec3(1.15, 0.12, 0.86)
    val leftUpper = Vec3(-0.92, 0.62, -0.38)
    val rightUpper = Vec3(0.92, 0.62, -0.38)

    fun tri(a: Vec3, b: Vec3, c: Vec3, color: String): List<Vertex3D> {
        val material = if (flash) {
            Color.fromHex("fff2a0")
        } else {
            Color.applyHueShift(Color.fromHex(color), hueShift)
        }
        return listOf(Vertex3D(a, material), Vertex3D(b, material), Vertex3D(c, material))
    }

    val vertices = mutableListOf<Vertex3D>()
    vertices += tri(nose, leftFang, top, "ff4fb8")
    vertices += tri(nose, top, rightFang, "ff8a24")
    vertices += tri(leftFang, leftWing, top, "7b2cff")
    vertices += tri(rightFang, top, rightWing, "c8ff36")
    vertices += tri(leftWing, leftUpper, top, "3d2fff")
    vertices += tri(rightWing, top, rightUpper, "21e4bc")
    vertices += tri(leftWing, belly, leftFang, "1a174c")
    vertices += tri(rightFang, belly, rightWing, "1a174c")
    vertices += tri(leftWing, leftTail, belly, "ff2638")
    vertices += tri(rightWing, belly, rightTail, "ff2638")
    vertices += tri(leftTail, tail, belly, "5dffcb")
    vertices += tri(rightTail, belly, tail, "5dffcb")
    vertices += tri(leftUpper, leftTail, top, "f0c84b")
    vertices += tri(rightUpper, top, rightTail, "f0c84b")
    vertices += tri(top, leftTail, tail, "7429ff")
    vertices += tri(top, tail, rightTail, "ff4fb8")
    vertices += tri(belly, tail, core, "111b3d")
    vertices += tri(top, core, tail, "2f46ff")

    return GpuMesh.create(gpu, vertices)
}

private fun createTerrainMesh(gpu: GpuContext, hueShift: Float = 0f): GpuMesh {
    val vertices = mutableListOf<Vertex3D>()
    val columns = 12
    val rows = 8
    val floorHalfWidth = 4.55
    val wallHalfWidth = 6.85
    val depth = 4.9

    fun z(row: Int): Double {
        return -depth * 0.5 + depth * (row.toDouble() / rows.toDouble())
    }

    fun floorX(column: Int): Double {
        return -floorHalfWidth + floorHalfWidth * 2.0 * (column.toDouble() / columns.toDouble())
    }

    fun floorHeight(x: Double, row: Int): Double {
        val normalizedX = x / floorHalfWidth
        val sideRise = abs(normalizedX) * abs(normalizedX) * 0.28
        val rolling = sin(x * 1.15 + row * 0.72) * 0.18
        val chop = cos(x * 2.35 - row * 0.64) * 0.07
        val ridge = sin((x + row) * 0.47) * 0.1
        return sideRise + rolling + chop + ridge
    }

    fun wallHeight(side: Double, row: Int, base: Double): Double {
        return base + sin(row * 0.83 + side * 1.7) * 0.34 + cos(row * 1.41 + side) * 0.16
    }

    fun addQuad(a: Vec3, b: Vec3, c: Vec3, d: Vec3, color: Color) {
        vertices += Vertex3D(a, color)
        vertices += Vertex3D(b, color)
        vertices += Vertex3D(c, color)
        vertices += Vertex3D(a, color)
        vertices += Vertex3D(c, color)
        vertices += Vertex3D(d, color)
    }

    fun shifted(hex: String): Color {
        return Color.applyHueShift(Color.fromHex(hex), hueShift)
    }

    fun floorColor(column: Int, row: Int): Color {
        return when ((column + row) % 4) {
            0 -> shifted("1f3fb5")
            1 -> shifted("22d1c3")
            2 -> shifted("34306f")
            else -> shifted("72f08f")
        }
    }

    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val x0 = floorX(column)
            val x1 = floorX(column + 1)
            val z0 = z(row)
            val z1 = z(row + 1)

            addQuad(
                a = Vec3(x0, floorHeight(x0, row), z0),
                b = Vec3(x0, floorHeight(x0, row + 1), z1),
                c = Vec3(x1, floorHeight(x1, row + 1), z1),
                d = Vec3(x1, floorHeight(x1, row), z0),
                color = floorColor(column, row)
            )
        }

        val z0 = z(row)
        val z1 = z(row + 1)
        val leftInner0 = Vec3(-floorHalfWidth, floorHeight(-floorHalfWidth, row), z0)
        val leftInner1 = Vec3(-floorHalfWidth, floorHeight(-floorHalfWidth, row + 1), z1)
        val leftShelf0 = Vec3(-wallHalfWidth + 0.9, wallHeight(-1.0, row, 1.08), z0)
        val leftShelf1 = Vec3(-wallHalfWidth + 0.9, wallHeight(-1.0, row + 1, 1.08), z1)
        val leftTop0 = Vec3(-wallHalfWidth, wallHeight(-1.0, row, 2.58), z0)
        val leftTop1 = Vec3(-wallHalfWidth, wallHeight(-1.0, row + 1, 2.58), z1)

        val rightInner0 = Vec3(floorHalfWidth, floorHeight(floorHalfWidth, row), z0)
        val rightInner1 = Vec3(floorHalfWidth, floorHeight(floorHalfWidth, row + 1), z1)
        val rightShelf0 = Vec3(wallHalfWidth - 0.9, wallHeight(1.0, row, 1.16), z0)
        val rightShelf1 = Vec3(wallHalfWidth - 0.9, wallHeight(1.0, row + 1, 1.16), z1)
        val rightTop0 = Vec3(wallHalfWidth, wallHeight(1.0, row, 2.72), z0)
        val rightTop1 = Vec3(wallHalfWidth, wallHeight(1.0, row + 1, 2.72), z1)

        addQuad(leftInner0, leftInner1, leftShelf1, leftShelf0, shifted("7b2cff"))
        addQuad(leftShelf0, leftShelf1, leftTop1, leftTop0, shifted("ff4fb8"))
        addQuad(rightInner0, rightShelf0, rightShelf1, rightInner1, shifted("ff8a24"))
        addQuad(rightShelf0, rightTop0, rightTop1, rightShelf1, shifted("c8ff36"))
    }

    return GpuMesh.create(gpu, vertices)
}

private fun resolveShooterAsset(relativePath: String): String {
    val builtAssetPath = File.resolveAssetPath("assets/$relativePath")
    if (File.isExist(builtAssetPath)) {
        return builtAssetPath
    }

    val sourceAssetPath = File.resolveAssetPath("games/kengine-3d-space-shooter/assets/$relativePath")
    if (File.isExist(sourceAssetPath)) {
        return sourceAssetPath
    }

    return builtAssetPath
}
