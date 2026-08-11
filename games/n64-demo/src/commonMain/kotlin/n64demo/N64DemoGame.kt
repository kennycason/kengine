package n64demo

import com.kengine.PortableGame
import com.kengine.audio.AudioAssetId
import com.kengine.audio.AudioContext
import com.kengine.input.InputButton
import com.kengine.input.InputState
import com.kengine.render.RenderContext
import com.kengine.storage.PortableStorage

class N64DemoGame : PortableGame {
    override val storageNamespace: String = "n64demo"

    private var storage: PortableStorage? = null
    private var frame = 0
    private var previousInputMask = 0
    private var modelIndex = 0
    private var cameraYaw = DEFAULT_CAMERA_YAW
    private var cameraPitch = DEFAULT_CAMERA_PITCH
    private var cameraZoom = 0
    private var modelYaw = 0
    private var modelPitch = CRAFT_RACER_BASE_PITCH
    private var spinSpeedIndex = DEFAULT_SPIN_SPEED_INDEX
    private var autoSpin = true
    private var pendingSound = NO_SOUND

    private val renderer = N64WireModelRenderer3D()
    private val models = N64DemoModelAssets.all
    private val modelStats = Array(models.size) { index ->
        val model = models[index]
        "${model.vertexCount}v ${model.triangleCount}t ${model.edgeCount}e"
    }

    private val finishSound = AudioAssetId.sound("finish")
    private val chordSound = AudioAssetId.sound("chord")

    override fun attachStorage(storage: PortableStorage) {
        this.storage = storage
        loadSelectedModel()
    }

    override fun update(input: InputState) {
        val aJustPressed = input.justPressed(InputButton.A)
        val bJustPressed = input.justPressed(InputButton.B)
        val startJustPressed = input.justPressed(InputButton.START)
        val cLeftJustPressed = input.justPressed(InputButton.C_LEFT)
        val cRightJustPressed = input.justPressed(InputButton.C_RIGHT)

        if (aJustPressed) {
            modelIndex += 1
            if (modelIndex >= models.size) modelIndex = 0
            modelPitch = modelPitchBase()
            pendingSound = chordSound
            saveSelectedModel()
        }

        if (bJustPressed) {
            autoSpin = !autoSpin
            pendingSound = chordSound
        }

        if (startJustPressed) {
            resetCamera()
            pendingSound = finishSound
        }

        if (cLeftJustPressed) {
            adjustSpinSpeed(-1)
            pendingSound = chordSound
        }
        if (cRightJustPressed) {
            adjustSpinSpeed(1)
            pendingSound = chordSound
        }

        cameraYaw = wrapAngle(cameraYaw + input.axis(InputButton.DPAD_LEFT, InputButton.DPAD_RIGHT) * CAMERA_ORBIT_SPEED)
        cameraPitch = clampInt(
            cameraPitch + input.axis(InputButton.DPAD_DOWN, InputButton.DPAD_UP) * CAMERA_PITCH_SPEED,
            MIN_CAMERA_PITCH,
            MAX_CAMERA_PITCH
        )

        if (input.isPressed(InputButton.C_UP)) {
            cameraZoom = clampInt(cameraZoom - CAMERA_ZOOM_SPEED, MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM)
        }
        if (input.isPressed(InputButton.C_DOWN)) {
            cameraZoom = clampInt(cameraZoom + CAMERA_ZOOM_SPEED, MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM)
        }

        if (input.isPressed(InputButton.L) || input.isPressed(InputButton.Z)) {
            modelYaw = wrapAngle(modelYaw - currentManualSpinSpeed())
            autoSpin = false
        }
        if (input.isPressed(InputButton.R)) {
            modelYaw = wrapAngle(modelYaw + currentManualSpinSpeed())
            autoSpin = false
        }

        if (autoSpin) {
            modelYaw = wrapAngle(modelYaw + currentAutoSpinSpeed())
            modelPitch = modelPitchBase() + sinAngle((frame * 3) and ANGLE_MASK) / MODEL_PITCH_WOBBLE_DIVISOR
        }

        frame += 1
        previousInputMask = input.mask
    }

    override fun audio(audio: AudioContext) {
        if (pendingSound != NO_SOUND) {
            audio.playSound(pendingSound)
            pendingSound = NO_SOUND
        }
    }

    override fun draw(render: RenderContext) {
        render.clear(n64Rgba(10, 14, 24))
        drawBorder(render)

        val model = models[modelIndex]
        val cameraDistance = CAMERA_DISTANCE + cameraZoom
        renderer.configure(
            render = render,
            targetX = 0,
            targetY = CAMERA_TARGET_Y,
            targetZ = 0,
            yaw = cameraYaw,
            pitch = cameraPitch,
            cameraDistance = cameraDistance,
            projectionDistance = PROJECTION_DISTANCE,
            centerX = render.width / 2,
            centerY = SCREEN_CENTER_Y
        )

        if (DRAW_GROUND_GRID) {
            renderer.drawGroundGrid(
                halfSize = ARENA_HALF_SIZE,
                step = GRID_STEP,
                color = n64Rgba(38, 54, 76, 150),
                axisColor = n64Rgba(86, 124, 160, 210)
            )
        }
        renderer.drawArenaBounds(ARENA_HALF_SIZE, n64Rgba(214, 232, 248, 170))
        val triangleBudget = modelTriangleBudget(model)
        if (FAST_TRIANGLE_SELECTION) {
            renderer.drawModelTrianglesFast(
                model = model,
                centerX = 0,
                centerY = MODEL_CENTER_Y,
                centerZ = 0,
                size = MODEL_SIZE,
                rotationX = modelPitch,
                rotationY = modelYaw,
                rotationZ = 0,
                triangleBudget = triangleBudget
            )
        } else {
            renderer.drawModelTriangles(
                model = model,
                centerX = 0,
                centerY = MODEL_CENTER_Y,
                centerZ = 0,
                size = MODEL_SIZE,
                rotationX = modelPitch,
                rotationY = modelYaw,
                rotationZ = 0,
                triangleBudget = triangleBudget,
                overlayEdgeBudget = MODEL_EDGE_BUDGET
            )
        }

        if (DRAW_HUD_TEXT) {
            drawHud(render, model)
        }
    }

    override fun cleanup() {
        saveSelectedModel()
    }

    internal fun debugSpinSpeedLevel(): Int {
        return spinSpeedIndex + 1
    }

    private fun drawHud(render: RenderContext, model: N64BakedWireModel3D) {
        render.drawText("N64 OBJ LAB", 10, 8, n64Rgba(248, 252, 255), 1)
        render.drawText(model.name, 10, 20, n64Rgba(255, 214, 108), 1)
        render.drawText(modelStats[modelIndex], 10, 32, n64Rgba(184, 210, 230), 1)
        render.drawText("${renderer.lastDrawnTriangles}t ${renderer.lastDrawnEdges}e spd ${spinSpeedIndex + 1}", 10, 44, n64Rgba(142, 178, 204), 1)
        render.drawText(spinModeText(), 222, 8, n64Rgba(184, 210, 230), 1)
        render.drawText("A model  B auto  Start reset", 10, 214, n64Rgba(184, 210, 230), 1)
        render.drawText("Dpad orbit  C zoom/speed  Z/L/R spin", 10, 226, n64Rgba(184, 210, 230), 1)
    }

    private fun spinModeText(): String {
        return if (autoSpin) "auto ${spinSpeedIndex + 1}" else "manual ${spinSpeedIndex + 1}"
    }

    private fun drawBorder(render: RenderContext) {
        val color = n64Rgba(48, 72, 102)
        render.fillRect(0, 0, render.width, 2, color)
        render.fillRect(0, render.height - 2, render.width, 2, color)
        render.fillRect(0, 0, 2, render.height, color)
        render.fillRect(render.width - 2, 0, 2, render.height, color)
    }

    private fun resetCamera() {
        cameraYaw = DEFAULT_CAMERA_YAW
        cameraPitch = DEFAULT_CAMERA_PITCH
        cameraZoom = 0
        modelYaw = 0
        modelPitch = modelPitchBase()
        spinSpeedIndex = DEFAULT_SPIN_SPEED_INDEX
        autoSpin = true
    }

    private fun loadSelectedModel() {
        val data = storage?.load("model-index") ?: return
        if (data.isNotEmpty() && data[0].toInt() in models.indices) {
            modelIndex = data[0].toInt()
            modelPitch = modelPitchBase()
        }
    }

    private fun saveSelectedModel() {
        val data = ByteArray(1)
        data[0] = modelIndex.toByte()
        storage?.save("model-index", data)
    }

    private fun InputState.justPressed(button: InputButton): Boolean {
        val bit = InputState.bitFor(button)
        return (mask and bit) != 0 && (previousInputMask and bit) == 0
    }

    private fun adjustSpinSpeed(delta: Int) {
        spinSpeedIndex = clampInt(spinSpeedIndex + delta, 0, MODEL_AUTO_SPIN_SPEEDS.lastIndex)
    }

    private fun currentAutoSpinSpeed(): Int {
        return MODEL_AUTO_SPIN_SPEEDS[spinSpeedIndex]
    }

    private fun currentManualSpinSpeed(): Int {
        return currentAutoSpinSpeed() * MODEL_MANUAL_SPIN_MULTIPLIER
    }

    private fun modelPitchBase(): Int {
        return if (models[modelIndex] === N64DemoModelAssets.craftRacer) {
            CRAFT_RACER_BASE_PITCH
        } else {
            DEFAULT_MODEL_PITCH
        }
    }

    private fun modelTriangleBudget(model: N64BakedWireModel3D): Int {
        return if (DRAW_ALL_MODEL_TRIANGLES) model.triangleCount else MODEL_TRIANGLE_BUDGET
    }

    private companion object {
        const val NO_SOUND = 0
        const val WORLD_SCALE = N64WireModelRenderer3D.WORLD_SCALE
        const val ANGLE_FULL = N64WireModelRenderer3D.ANGLE_FULL
        const val ANGLE_MASK = ANGLE_FULL - 1

        const val ARENA_HALF_SIZE = 7 * WORLD_SCALE
        const val GRID_STEP = WORLD_SCALE
        const val CAMERA_TARGET_Y = WORLD_SCALE
        const val CAMERA_DISTANCE = 11 * WORLD_SCALE
        const val CAMERA_ZOOM_SPEED = WORLD_SCALE / 4
        const val MIN_CAMERA_ZOOM = -7 * WORLD_SCALE
        const val MAX_CAMERA_ZOOM = 6 * WORLD_SCALE
        const val DEFAULT_CAMERA_YAW = 96
        const val DEFAULT_CAMERA_PITCH = -108
        const val MIN_CAMERA_PITCH = -180
        const val MAX_CAMERA_PITCH = 40
        const val CAMERA_ORBIT_SPEED = 13
        const val CAMERA_PITCH_SPEED = 7
        const val PROJECTION_DISTANCE = 136
        const val SCREEN_CENTER_Y = 132

        const val MODEL_CENTER_Y = 2 * WORLD_SCALE
        const val MODEL_SIZE = 5 * WORLD_SCALE
        const val MODEL_TRIANGLE_BUDGET = 160
        const val MODEL_EDGE_BUDGET = 0
        const val DRAW_ALL_MODEL_TRIANGLES = true
        const val DRAW_GROUND_GRID = false
        const val DRAW_HUD_TEXT = false
        const val FAST_TRIANGLE_SELECTION = true
        const val DEFAULT_MODEL_PITCH = 0
        const val CRAFT_RACER_BASE_PITCH = -ANGLE_FULL / 8
        const val MODEL_PITCH_WOBBLE_DIVISOR = 80
        const val DEFAULT_SPIN_SPEED_INDEX = 2
        const val MODEL_MANUAL_SPIN_MULTIPLIER = 3
        val MODEL_AUTO_SPIN_SPEEDS = intArrayOf(6, 9, 12, 18, 26)
    }
}
