import com.kengine.graphics.Color
import com.kengine.input.keyboard.KeyboardInputEventSubscriber
import com.kengine.input.keyboard.Keys
import com.kengine.math.Vec3
import com.kengine.three.DirectionalLight3D
import kotlin.math.PI
import kotlin.math.roundToInt

data class ViewerModelPreset(
    val label: String,
    val modelPath: String,
    val mode: ViewerModelMode,
    val targetSize: Double,
    val clipName: String? = null
)

data class ViewerBackgroundPreset(
    val label: String,
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1f
)

data class ViewerLightPreset(
    val label: String,
    val light: DirectionalLight3D
)

data class ViewerCameraPreset(
    val label: String,
    val yawRadians: Float,
    val pitchRadians: Float,
    val distanceMultiplier: Double,
    val targetHeightMultiplier: Double = 0.42
)

sealed class ViewerControlAction {
    data class SelectModel(val step: Int) : ViewerControlAction()
    data class SelectClip(val step: Int) : ViewerControlAction()
    data class SelectCameraPreset(val index: Int) : ViewerControlAction()
    object PreflightModel : ViewerControlAction()
    object PreflightAllModels : ViewerControlAction()
    object ResetView : ViewerControlAction()
    object PrintHelp : ViewerControlAction()
    object PrintStatus : ViewerControlAction()
    data class Message(val text: String) : ViewerControlAction()
}

class ViewerControlState(
    initialModelPresets: List<ViewerModelPreset>,
    initialModelPresetIndex: Int = 0
) {
    private val modelPresets = initialModelPresets.toMutableList()

    init {
        require(modelPresets.isNotEmpty()) {
            "Model viewer requires at least one model preset."
        }
        require(initialModelPresetIndex in modelPresets.indices) {
            "Initial model preset index must be inside the preset list."
        }
    }

    private val keyEdges = KeyboardEdgeTracker()
    private var lightPresetIndex = 0
    private var lightAmbientStrength = defaultLightPresets[lightPresetIndex].light.ambientStrength
    private var lightDiffuseStrength = defaultLightPresets[lightPresetIndex].light.diffuseStrength
    private var backgroundPresetIndex = 0
    private var cameraPresetIndex = 0

    var modelPresetIndex: Int = initialModelPresetIndex
        private set
    var showAxes: Boolean = true
        private set
    var animationPaused: Boolean = false
        private set
    var animationTimeSeconds: Double = 0.0
        private set
    var animationSpeed: Double = 1.0
        private set

    val currentModelPreset: ViewerModelPreset
        get() = modelPresets[modelPresetIndex]

    fun modelPresetsSnapshot(): List<ViewerModelPreset> {
        return modelPresets.toList()
    }

    val currentBackground: ViewerBackgroundPreset
        get() = defaultBackgroundPresets[backgroundPresetIndex]

    val currentLightPreset: ViewerLightPreset
        get() = defaultLightPresets[lightPresetIndex]

    val currentLight: DirectionalLight3D
        get() = currentLightPreset.light.copy(
            ambientStrength = lightAmbientStrength,
            diffuseStrength = lightDiffuseStrength
        )

    val currentCameraPreset: ViewerCameraPreset
        get() = defaultCameraPresets[cameraPresetIndex]

    fun primeKeyboard(keyboard: KeyboardInputEventSubscriber) {
        keyEdges.prime(keyboard, controlledKeys)
    }

    fun updateAnimationClock(deltaSeconds: Double) {
        if (!animationPaused) {
            animationTimeSeconds += deltaSeconds * animationSpeed
        }
    }

    fun selectModel(step: Int): ViewerModelPreset {
        modelPresetIndex = wrappedIndex(modelPresetIndex, step, modelPresets.size)
        resetAnimationClock()
        return currentModelPreset
    }

    fun selectOrAddModel(preset: ViewerModelPreset): ViewerModelPreset {
        val existingIndex = modelPresets.indexOfFirst { it.modelPath == preset.modelPath }
        modelPresetIndex = if (existingIndex >= 0) {
            modelPresets[existingIndex] = preset
            existingIndex
        } else {
            modelPresets += preset
            modelPresets.lastIndex
        }
        resetAnimationClock()
        return currentModelPreset
    }

    fun resetAnimationClock() {
        animationTimeSeconds = 0.0
    }

    fun toggleAnimationPlayback(): String {
        animationPaused = !animationPaused
        return if (animationPaused) "Animation paused." else "Animation playing."
    }

    fun stopAnimationPlayback(): String {
        animationPaused = true
        resetAnimationClock()
        return "Animation stopped."
    }

    fun setAnimationSpeed(value: Double): String {
        animationSpeed = value.coerceIn(0.25, 3.0)
        return "Animation speed: ${animationSpeed.oneDecimal()}x"
    }

    fun adjustAnimationSpeed(delta: Double): String {
        return setAnimationSpeed(animationSpeed + delta)
    }

    fun cycleBackground(step: Int = 1): String {
        backgroundPresetIndex = wrappedIndex(backgroundPresetIndex, step, defaultBackgroundPresets.size)
        return "Background: ${currentBackground.label}"
    }

    fun cycleLight(step: Int = 1): String {
        lightPresetIndex = wrappedIndex(lightPresetIndex, step, defaultLightPresets.size)
        lightAmbientStrength = currentLightPreset.light.ambientStrength
        lightDiffuseStrength = currentLightPreset.light.diffuseStrength
        return "Light preset: ${currentLightPreset.label}"
    }

    fun selectCameraPreset(index: Int): String {
        cameraPresetIndex = index.coerceIn(defaultCameraPresets.indices)
        return "Camera: ${currentCameraPreset.label}"
    }

    fun cycleCameraPreset(step: Int = 1): String {
        cameraPresetIndex = wrappedIndex(cameraPresetIndex, step, defaultCameraPresets.size)
        return "Camera: ${currentCameraPreset.label}"
    }

    fun setAmbientStrength(value: Double): String {
        lightAmbientStrength = value.toFloat().coerceIn(0.0f, 1.0f)
        return "Ambient light: ${lightAmbientStrength.twoDecimals()}"
    }

    fun adjustAmbientStrength(delta: Float): String {
        return setAmbientStrength((lightAmbientStrength + delta).toDouble())
    }

    fun setDiffuseStrength(value: Double): String {
        lightDiffuseStrength = value.toFloat().coerceIn(0.0f, 1.5f)
        return "Diffuse light: ${lightDiffuseStrength.twoDecimals()}"
    }

    fun adjustDiffuseStrength(delta: Float): String {
        return setDiffuseStrength((lightDiffuseStrength + delta).toDouble())
    }

    fun toggleAxes(): String {
        showAxes = !showAxes
        return if (showAxes) "Axes visible." else "Axes hidden."
    }

    fun resetControls(): String {
        resetViewState()
        return "Viewer controls reset."
    }

    fun handleKeyboard(keyboard: KeyboardInputEventSubscriber): List<ViewerControlAction> {
        val actions = mutableListOf<ViewerControlAction>()

        if (keyEdges.justPressed(keyboard, Keys.M)) {
            actions += ViewerControlAction.SelectModel(step = 1)
        }
        if (keyEdges.justPressed(keyboard, Keys.N)) {
            actions += ViewerControlAction.SelectModel(step = -1)
        }
        if (keyEdges.justPressed(keyboard, Keys.C)) {
            actions += ViewerControlAction.SelectClip(step = 1)
        }
        if (keyEdges.justPressed(keyboard, Keys.V)) {
            actions += ViewerControlAction.SelectClip(step = -1)
        }
        if (keyEdges.justPressed(keyboard, Keys.SPACE)) {
            actions += ViewerControlAction.Message(toggleAnimationPlayback())
        }
        if (keyEdges.justPressed(keyboard, Keys.Z)) {
            actions += ViewerControlAction.Message(adjustAnimationSpeed(-0.25))
        }
        if (keyEdges.justPressed(keyboard, Keys.X)) {
            actions += ViewerControlAction.Message(adjustAnimationSpeed(0.25))
        }
        if (keyEdges.justPressed(keyboard, Keys.B)) {
            actions += ViewerControlAction.Message(cycleBackground())
        }
        if (keyEdges.justPressed(keyboard, Keys.L)) {
            actions += ViewerControlAction.Message(cycleLight())
        }
        if (keyEdges.justPressed(keyboard, Keys.J)) {
            actions += ViewerControlAction.Message(adjustAmbientStrength(-0.05f))
        }
        if (keyEdges.justPressed(keyboard, Keys.K)) {
            actions += ViewerControlAction.Message(adjustAmbientStrength(0.05f))
        }
        if (keyEdges.justPressed(keyboard, Keys.U)) {
            actions += ViewerControlAction.Message(adjustDiffuseStrength(-0.05f))
        }
        if (keyEdges.justPressed(keyboard, Keys.I)) {
            actions += ViewerControlAction.Message(adjustDiffuseStrength(0.05f))
        }
        if (keyEdges.justPressed(keyboard, Keys.G)) {
            actions += ViewerControlAction.Message(toggleAxes())
        }
        if (keyEdges.justPressed(keyboard, Keys.ONE)) {
            actions += ViewerControlAction.SelectCameraPreset(index = 0)
        }
        if (keyEdges.justPressed(keyboard, Keys.TWO)) {
            actions += ViewerControlAction.SelectCameraPreset(index = 1)
        }
        if (keyEdges.justPressed(keyboard, Keys.THREE)) {
            actions += ViewerControlAction.SelectCameraPreset(index = 2)
        }
        if (keyEdges.justPressed(keyboard, Keys.FOUR)) {
            actions += ViewerControlAction.SelectCameraPreset(index = 3)
        }
        if (keyEdges.justPressed(keyboard, Keys.P)) {
            actions += ViewerControlAction.PreflightModel
        }
        if (keyEdges.justPressed(keyboard, Keys.A)) {
            actions += ViewerControlAction.PreflightAllModels
        }
        if (keyEdges.justPressed(keyboard, Keys.R)) {
            resetViewState()
            actions += ViewerControlAction.ResetView
        }
        if (keyEdges.justPressed(keyboard, Keys.H) || keyEdges.justPressed(keyboard, Keys.F1)) {
            actions += ViewerControlAction.PrintHelp
        }
        if (keyEdges.justPressed(keyboard, Keys.T)) {
            actions += ViewerControlAction.PrintStatus
        }

        return actions
    }

    private fun resetViewState() {
        animationPaused = false
        animationSpeed = 1.0
        resetAnimationClock()
        showAxes = true
        cameraPresetIndex = 0
        backgroundPresetIndex = 0
        lightPresetIndex = 0
        lightAmbientStrength = currentLightPreset.light.ambientStrength
        lightDiffuseStrength = currentLightPreset.light.diffuseStrength
    }

    private fun wrappedIndex(
        index: Int,
        step: Int,
        size: Int
    ): Int {
        return ((index + step) % size + size) % size
    }

    private class KeyboardEdgeTracker {
        private val pressedLastFrame = mutableSetOf<UInt>()

        fun prime(
            keyboard: KeyboardInputEventSubscriber,
            keys: List<UInt>
        ) {
            keys.forEach { key ->
                if (keyboard.isPressed(key)) {
                    pressedLastFrame += key
                } else {
                    pressedLastFrame -= key
                }
            }
        }

        fun justPressed(
            keyboard: KeyboardInputEventSubscriber,
            key: UInt
        ): Boolean {
            val isPressed = keyboard.isPressed(key)
            val wasPressed = key in pressedLastFrame
            if (isPressed) {
                pressedLastFrame += key
            } else {
                pressedLastFrame -= key
            }
            return isPressed && !wasPressed
        }
    }
}

private val controlledKeys = listOf(
    Keys.M,
    Keys.N,
    Keys.C,
    Keys.V,
    Keys.SPACE,
    Keys.Z,
    Keys.X,
    Keys.B,
    Keys.L,
    Keys.J,
    Keys.K,
    Keys.U,
    Keys.I,
    Keys.G,
    Keys.ONE,
    Keys.TWO,
    Keys.THREE,
    Keys.FOUR,
    Keys.P,
    Keys.A,
    Keys.R,
    Keys.H,
    Keys.F1,
    Keys.T
)

val defaultCameraPresets = listOf(
    ViewerCameraPreset(
        label = "Front",
        yawRadians = 0.0f,
        pitchRadians = 0.2f,
        distanceMultiplier = 2.7
    ),
    ViewerCameraPreset(
        label = "Three quarter",
        yawRadians = (PI * 0.25).toFloat(),
        pitchRadians = 0.26f,
        distanceMultiplier = 2.9
    ),
    ViewerCameraPreset(
        label = "Side",
        yawRadians = (PI * 0.5).toFloat(),
        pitchRadians = 0.2f,
        distanceMultiplier = 2.7
    ),
    ViewerCameraPreset(
        label = "Top",
        yawRadians = 0.0f,
        pitchRadians = 1.12f,
        distanceMultiplier = 3.2,
        targetHeightMultiplier = 0.16
    )
)

fun defaultViewerModelPresets(): List<ViewerModelPreset> {
    return listOf(
        ViewerModelPreset(
            label = "Mario animated",
            modelPath = "models/Mario64Animated.glb",
            mode = ViewerModelMode.SKINNED,
            targetSize = 2.2
        ),
        ViewerModelPreset(
            label = "Mario static",
            modelPath = "models/Mario 64 Model.glb",
            mode = ViewerModelMode.STATIC,
            targetSize = 2.2
        ),
        ViewerModelPreset(
            label = "Bowser static",
            modelPath = "models/Super Mario 64 Bowser.glb",
            mode = ViewerModelMode.STATIC,
            targetSize = 2.6
        ),
        ViewerModelPreset(
            label = "Goomba animated",
            modelPath = "models/Animated Goomba Super Mario Bros.glb",
            mode = ViewerModelMode.NODE_ANIMATED,
            targetSize = 2.0
        ),
        ViewerModelPreset(
            label = "Ridley animated",
            modelPath = "models/Ridley64.glb",
            mode = ViewerModelMode.SKINNED,
            targetSize = 2.8
        ),
        ViewerModelPreset(
            label = "Battlefield static",
            modelPath = "models/Super Mario 64 Bob-Omb Battlefield.glb",
            mode = ViewerModelMode.STATIC,
            targetSize = 5.2
        ),
        ViewerModelPreset(
            label = "Kenney craft",
            modelPath = "models/kenney-space-kit/craft_racer.obj",
            mode = ViewerModelMode.STATIC,
            targetSize = 2.2
        ),
        ViewerModelPreset(
            label = "Kenney turret",
            modelPath = "models/kenney-space-kit/turret_double.obj",
            mode = ViewerModelMode.STATIC,
            targetSize = 2.2
        ),
        ViewerModelPreset(
            label = "Kenney meteor",
            modelPath = "models/kenney-space-kit/meteor_detailed.obj",
            mode = ViewerModelMode.STATIC,
            targetSize = 1.8
        )
    )
}

val defaultBackgroundPresets = listOf(
    ViewerBackgroundPreset("Charcoal", 0.045f, 0.055f, 0.07f),
    ViewerBackgroundPreset("Neutral gray", 0.19f, 0.20f, 0.21f),
    ViewerBackgroundPreset("Deep green", 0.035f, 0.075f, 0.06f),
    ViewerBackgroundPreset("Warm studio", 0.18f, 0.145f, 0.105f)
)

val defaultLightPresets = listOf(
    ViewerLightPreset(
        label = "Warm studio",
        light = DirectionalLight3D(
            direction = Vec3(-0.35, -0.85, -0.45),
            color = Color.fromHex("fff4dc"),
            ambientStrength = 0.48f,
            diffuseStrength = 0.82f
        )
    ),
    ViewerLightPreset(
        label = "Cool side",
        light = DirectionalLight3D(
            direction = Vec3(0.85, -0.55, -0.18),
            color = Color.fromHex("dcecff"),
            ambientStrength = 0.34f,
            diffuseStrength = 0.95f
        )
    ),
    ViewerLightPreset(
        label = "Top soft",
        light = DirectionalLight3D(
            direction = Vec3(-0.1, -1.0, -0.12),
            color = Color.fromHex("ffffff"),
            ambientStrength = 0.58f,
            diffuseStrength = 0.55f
        )
    ),
    ViewerLightPreset(
        label = "High contrast",
        light = DirectionalLight3D(
            direction = Vec3(-0.7, -0.38, -0.62),
            color = Color.fromHex("fff0c4"),
            ambientStrength = 0.18f,
            diffuseStrength = 1.18f
        )
    )
)

private fun Double.oneDecimal(): String {
    return ((this * 10.0).roundToInt().toDouble() / 10.0).toString()
}

private fun Float.twoDecimals(): String {
    return ((this * 100.0f).roundToInt().toFloat() / 100.0f).toString()
}
