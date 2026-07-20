import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.hooks.context.useContext
import com.kengine.math.Vec3
import com.kengine.sdl.RenderBackend
import com.kengine.three.AnimatedModelInstance3D
import com.kengine.three.AnimatedModelAsset3D
import com.kengine.three.AnimatedModelSourceCache3D
import com.kengine.three.AnimationPose3D
import com.kengine.three.Camera3D
import com.kengine.three.DebugRenderer3D
import com.kengine.three.GpuContext
import com.kengine.three.GpuFrame
import com.kengine.three.GpuResourceScope3D
import com.kengine.three.GpuTextureCache3D
import com.kengine.three.ModelAsset3D
import com.kengine.three.ModelAssetLoader3D
import com.kengine.three.ModelAssetPathResolver3D
import com.kengine.three.ModelInfo3D
import com.kengine.three.ModelLoadOptions3D
import com.kengine.three.ModelSourceCache3D
import com.kengine.three.Node3D
import com.kengine.three.OrbitCameraController3D
import com.kengine.three.Scene3D
import com.kengine.three.SceneModel3D
import com.kengine.three.SceneRenderer3D
import com.kengine.three.createInstance
import com.kengine.three.setPose
import com.kengine.three.ui.GpuUiRenderer3D
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import sdl3.SDL_GetTicks
import sdl3.SDL_SetWindowTitle
import kotlin.math.sqrt

private const val WINDOW_WIDTH = 1280
private const val WINDOW_HEIGHT = 760
private const val DEFAULT_ASSET_ROOT = "assets"
private const val DEFAULT_MODEL_PATH = "models/Mario64Animated.glb"
private const val DEFAULT_TARGET_SIZE = 2.2
private const val DEFAULT_UI_FONT_PATH = "assets/fonts/arcade_classic.ttf"

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    val config = try {
        ViewerConfig.parse(args)
    } catch (error: IllegalArgumentException) {
        println(error.message)
        println()
        ViewerConfig.printUsage()
        return
    }

    if (config.printHelp) {
        ViewerConfig.printUsage()
        return
    }

    val defaultPresets = defaultViewerModelPresets()
    val defaultPresetIndex = defaultPresets.indexOfFirst { it.matches(config) }
    val modelPresets = if (defaultPresetIndex >= 0) {
        defaultPresets
    } else {
        listOf(
            ViewerModelPreset(
                label = "CLI model",
                modelPath = config.modelPath,
                mode = config.mode,
                targetSize = config.targetSize,
                clipName = config.clipName
            )
        ) + defaultPresets
    }
    val controls = ViewerControlState(
        modelPresets = modelPresets,
        initialModelPresetIndex = defaultPresetIndex.coerceAtLeast(0)
    )

    createGameContext(
        title = "Kengine 3D Model Viewer - ${config.modelPath.substringAfterLast('/')}",
        width = WINDOW_WIDTH,
        height = WINDOW_HEIGHT,
        renderBackend = RenderBackend.SDL_GPU_3D
    ) {
        useContext(GpuContext.create(sdl), cleanup = true) {
            val resources = GpuResourceScope3D()
            val textureCache = resources.track(GpuTextureCache3D(this))
            val modelAssets = ModelAssetLoader3D(
                gpu = this,
                resources = resources,
                resolver = ModelAssetPathResolver3D(sourceAssetRoot = config.assetRoot),
                textureCache = textureCache,
                sourceCache = ModelSourceCache3D(),
                animatedSourceCache = AnimatedModelSourceCache3D()
            )
            val sceneRenderer = resources.track(SceneRenderer3D(this))
            val debugRenderer = resources.track(DebugRenderer3D(this)) { it.cleanup() }
            val uiFont = font.addFont("viewer-ui", DEFAULT_UI_FONT_PATH, fontSize = 14f)
            val uiRenderer = resources.track(GpuUiRenderer3D(this, uiFont))
            val scene = Scene3D(controls.currentLight)
            resources.track(scene)
            val modelCache = mutableMapOf<ViewerModelPreset, ViewerLoadedModel>()

            fun activateModel(
                preset: ViewerModelPreset,
                reload: Boolean = false
            ): ViewerLoadedModel {
                scene.clear()
                if (reload) {
                    modelCache.remove(preset)?.cleanup()
                }
                val loadedModel = modelCache.getOrPut(preset) {
                    loadViewerModel(modelAssets, preset)
                }
                loadedModel.addTo(scene)
                controls.resetAnimationClock()
                printLoadedModel(preset, loadedModel)
                return loadedModel
            }

            var activeModel = activateModel(controls.currentModelPreset)
            var cameraController = createCameraController(controls.currentModelPreset.targetSize)
            fun updateWindowTitle() {
                SDL_SetWindowTitle(sdl.window, viewerWindowTitle(controls, activeModel))
            }
            fun printMessage(message: String) {
                println(message)
                updateWindowTitle()
            }
            updateWindowTitle()
            val inspectorUi = ViewerInspectorUi(
                controls = controls,
                activeModel = { activeModel },
                onSelectModel = { step ->
                    val preset = controls.selectModel(step)
                    activeModel = activateModel(preset)
                    cameraController = createCameraController(preset.targetSize)
                    updateWindowTitle()
                },
                onSelectClip = { step ->
                    val clipName = activeModel.selectClip(step)
                    controls.resetAnimationClock()
                    printMessage(
                        clipName?.let { "Animation clip: $it" }
                            ?: "This model has no animation clips."
                    )
                },
                onResetView = {
                    printMessage(controls.resetControls())
                    cameraController = createCameraController(controls.currentModelPreset.targetSize)
                },
                onReloadModel = {
                    activeModel = activateModel(controls.currentModelPreset, reload = true)
                    cameraController = createCameraController(controls.currentModelPreset.targetSize)
                    printMessage("Loaded model: ${controls.currentModelPreset.label}")
                },
                onMessage = ::printMessage,
                onChanged = ::updateWindowTitle
            )
            printViewerControls()
            var previousTicks = SDL_GetTicks()
            var controlsPrimed = false

            try {
                while (isRunning) {
                    sdlEvent.pollEvents()
                    action.update()

                    val ticks = SDL_GetTicks()
                    val deltaSeconds = ((ticks - previousTicks).toDouble() / 1000.0).coerceIn(0.0, 0.1)
                    previousTicks = ticks
                    val elapsedSeconds = ticks.toDouble() / 1000.0
                    val keyboardState = keyboard.keyboard

                    if (keyboardState.isEscapePressed()) {
                        isRunning = false
                    }

                    val uiHandledMouse = inspectorUi.handleMouse(mouse.mouse)

                    if (!controlsPrimed) {
                        controls.primeKeyboard(keyboardState)
                        controlsPrimed = true
                    } else {
                        controls.handleKeyboard(keyboardState).forEach { controlAction ->
                            when (controlAction) {
                                is ViewerControlAction.SelectModel -> {
                                    val preset = controls.selectModel(controlAction.step)
                                    activeModel = activateModel(preset)
                                    cameraController = createCameraController(preset.targetSize)
                                    updateWindowTitle()
                                }
                                is ViewerControlAction.SelectClip -> {
                                    val clipName = activeModel.selectClip(controlAction.step)
                                    controls.resetAnimationClock()
                                    println(
                                        clipName?.let { "Animation clip: $it" }
                                            ?: "This model has no animation clips."
                                    )
                                    updateWindowTitle()
                                }
                                ViewerControlAction.ResetView -> {
                                    cameraController = createCameraController(controls.currentModelPreset.targetSize)
                                    println("Viewer controls reset.")
                                    updateWindowTitle()
                                }
                                ViewerControlAction.PrintHelp -> {
                                    printViewerControls()
                                }
                                ViewerControlAction.PrintStatus -> {
                                    printViewerStatus(controls, activeModel)
                                }
                                is ViewerControlAction.Message -> {
                                    println(controlAction.text)
                                    updateWindowTitle()
                                }
                            }
                        }
                    }
                    if (!uiHandledMouse) {
                        cameraController.update(
                            mouse = mouse.mouse,
                            keyboard = keyboardState,
                            deltaSeconds = deltaSeconds
                        )
                    }
                    controls.updateAnimationClock(deltaSeconds)
                    scene.light = controls.currentLight
                    activeModel.update(controls.animationTimeSeconds)
                    scene.prepareForDraw()
                    inspectorUi.prepare(uiRenderer)
                    val camera = cameraController.camera()
                    val background = controls.currentBackground

                    render(background.r, background.g, background.b, background.a, enableDepth = true) { frame ->
                        sceneRenderer.draw(scene, frame, camera)
                        if (controls.showAxes) {
                            drawAxes(debugRenderer, frame, camera, controls.currentModelPreset.targetSize)
                        }
                        drawLightDirection(
                            debugRenderer = debugRenderer,
                            frame = frame,
                            camera = camera,
                            lightDirection = controls.currentLight.direction,
                            targetSize = controls.currentModelPreset.targetSize
                        )
                        inspectorUi.render(uiRenderer, frame)
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

enum class ViewerModelMode {
    STATIC,
    NODE_ANIMATED,
    SKINNED
}

private data class ViewerConfig(
    val modelPath: String = DEFAULT_MODEL_PATH,
    val assetRoot: String = DEFAULT_ASSET_ROOT,
    val mode: ViewerModelMode = ViewerModelMode.SKINNED,
    val targetSize: Double = DEFAULT_TARGET_SIZE,
    val clipName: String? = null,
    val printHelp: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): ViewerConfig {
            var config = ViewerConfig()
            var index = 0
            while (index < args.size) {
                when (val arg = args[index]) {
                    "--help", "-h" -> return config.copy(printHelp = true)
                    "--model" -> {
                        config = config.copy(modelPath = args.valueAfter(index, arg))
                        index += 1
                    }
                    "--asset-root" -> {
                        config = config.copy(assetRoot = args.valueAfter(index, arg))
                        index += 1
                    }
                    "--mode" -> {
                        config = config.copy(mode = parseMode(args.valueAfter(index, arg)))
                        index += 1
                    }
                    "--target-size" -> {
                        val value = args.valueAfter(index, arg).toDoubleOrNull()
                            ?: throw IllegalArgumentException("--target-size requires a number.")
                        config = config.copy(targetSize = value)
                        index += 1
                    }
                    "--clip" -> {
                        config = config.copy(clipName = args.valueAfter(index, arg))
                        index += 1
                    }
                    else -> throw IllegalArgumentException("Unknown argument: $arg")
                }
                index += 1
            }
            require(config.targetSize > 0.0) {
                "--target-size must be greater than zero."
            }
            return config
        }

        fun printUsage() {
            println(
                """
                Kengine 3D Model Viewer

                Usage:
                  ./gradlew :kengine-3d-model-viewer:runDebugExecutableMacosArm64
                  ./gradlew :kengine-3d-model-viewer:linkDebugExecutableMacosArm64
                  cd kengine-3d-model-viewer
                  ./build/bin/macosArm64/debugExecutable/kengine-3d-model-viewer.kexe --model "models/Super Mario 64 Bowser.glb" --mode static

                Options:
                  --model <path>         Model path relative to --asset-root, or an absolute path.
                  --asset-root <path>    Source asset root. Defaults to $DEFAULT_ASSET_ROOT.
                  --mode <mode>          static, node, or skinned. Defaults to skinned.
                  --target-size <size>   Normalized model height/extent target. Defaults to $DEFAULT_TARGET_SIZE.
                  --clip <name>          Animated clip name to preview when mode is node or skinned.
                  --help                 Print this help.

                Controls:
                  Mouse drag             Orbit camera.
                  Up/Down arrows         Zoom camera.
                  Left/Right arrows      Pan camera target.
                  M/N                    Next/previous model preset.
                  C/V                    Next/previous animation clip.
                  Space                  Pause/resume animation.
                  Z/X                    Decrease/increase animation speed.
                  B                      Cycle background preset.
                  L                      Cycle lighting preset.
                  J/K                    Decrease/increase ambient light.
                  U/I                    Decrease/increase diffuse light.
                  G                      Toggle axes.
                  R                      Reset viewer controls and camera.
                  H or F1                Print controls.
                  T                      Print current viewer status.
                  Escape                 Quit.
                """.trimIndent()
            )
        }

        private fun parseMode(value: String): ViewerModelMode {
            return when (value.lowercase()) {
                "static" -> ViewerModelMode.STATIC
                "node", "node-animated" -> ViewerModelMode.NODE_ANIMATED
                "skinned", "skinned-textured-lit" -> ViewerModelMode.SKINNED
                else -> throw IllegalArgumentException("--mode must be static, node, or skinned.")
            }
        }

        private fun Array<String>.valueAfter(
            index: Int,
            option: String
        ): String {
            return getOrNull(index + 1)
                ?: throw IllegalArgumentException("$option requires a value.")
        }
    }
}

sealed class ViewerLoadedModel {
    abstract val info: ModelInfo3D

    abstract fun addTo(scene: Scene3D)

    open fun update(animationTimeSeconds: Double) {
    }

    open fun selectedClipName(): String? {
        return null
    }

    open fun selectClip(step: Int): String? {
        return null
    }

    abstract fun cleanup()

    data class Static(
        override val info: ModelInfo3D,
        val node: Node3D<SceneModel3D>
    ) : ViewerLoadedModel() {
        override fun addTo(scene: Scene3D) {
            scene.addNode(node)
        }

        override fun cleanup() {
            node.cleanup()
        }
    }

    data class Animated(
        override val info: ModelInfo3D,
        val node: Node3D<AnimatedModelInstance3D>,
        var clipIndex: Int
    ) : ViewerLoadedModel() {
        override fun addTo(scene: Scene3D) {
            scene.addNode(node)
        }

        override fun cleanup() {
            node.cleanup()
        }

        override fun update(animationTimeSeconds: Double) {
            val clipDurationSeconds = info.animations.getOrNull(clipIndex)?.durationSeconds ?: 0.0
            val time = if (clipDurationSeconds > 0.0) {
                animationTimeSeconds % clipDurationSeconds
            } else {
                animationTimeSeconds
            }
            node.setPose(AnimationPose3D(clipIndex = clipIndex, timeSeconds = time))
        }

        override fun selectedClipName(): String? {
            return info.animations.getOrNull(clipIndex)?.name
        }

        override fun selectClip(step: Int): String? {
            if (info.animations.isEmpty()) {
                return null
            }
            clipIndex = ((clipIndex + step) % info.animations.size + info.animations.size) % info.animations.size
            return selectedClipName()
        }
    }
}

private fun loadViewerModel(
    loader: ModelAssetLoader3D,
    preset: ViewerModelPreset
): ViewerLoadedModel {
    val options = ModelLoadOptions3D(
        targetSize = preset.targetSize,
        defaultColor = Color.fromHex("ffffff")
    )
    return when (preset.mode) {
        ViewerModelMode.STATIC -> {
            val asset = ModelAsset3D(preset.modelPath, options)
            val source = loader.loadSource(asset)
            val model = loader.uploadModel(source)
            ViewerLoadedModel.Static(
                info = source.info,
                node = Node3D(SceneModel3D(model = model))
            )
        }
        ViewerModelMode.NODE_ANIMATED,
        ViewerModelMode.SKINNED -> {
            val asset = when (preset.mode) {
                ViewerModelMode.NODE_ANIMATED -> AnimatedModelAsset3D.nodeAnimatedLit(preset.modelPath, options)
                ViewerModelMode.SKINNED -> AnimatedModelAsset3D.skinnedTexturedLit(preset.modelPath, options)
                ViewerModelMode.STATIC -> error("Static mode is handled separately.")
            }
            val source = loader.loadAnimatedSource(asset)
            val animatedModel = loader.uploadAnimatedModel(source)
            val info = source.info
            val clipIndex = selectClipIndex(info, preset.clipName)
            ViewerLoadedModel.Animated(
                info = info,
                node = Node3D(
                    item = animatedModel.createInstance(
                        pose = AnimationPose3D(clipIndex = clipIndex)
                    )
                ),
                clipIndex = clipIndex
            )
        }
    }
}

private fun selectClipIndex(
    info: ModelInfo3D,
    clipName: String?
): Int {
    if (info.animations.isEmpty()) {
        return 0
    }
    if (clipName == null) {
        return 0
    }
    val index = info.animations.indexOfFirst { it.name == clipName }
    require(index >= 0) {
        "Animation clip '$clipName' was not found. Available clips: ${info.animations.joinToString { it.name }}"
    }
    return index
}

private fun printLoadedModel(
    preset: ViewerModelPreset,
    model: ViewerLoadedModel
) {
    val info = model.info
    println("Loaded ${preset.label} (${preset.mode.name.lowercase()}): ${info.assetPath}")
    println("  format=${info.format} vertices=${info.vertexCount} meshes=${info.meshCount} primitives=${info.primitiveCount}")
    println("  materials=${info.materialCount} textures=${info.textureCount} skins=${info.skinCount} animations=${info.animationCount}")
    if (info.animations.isNotEmpty()) {
        model.selectedClipName()?.let { selected ->
            println("  previewClip=$selected")
        } ?: println("  availableClips=${info.animations.size}")
        println("  clips=${info.animations.take(8).joinToString { it.name }}")
    }
}

private fun createCameraController(targetSize: Double): OrbitCameraController3D {
    return OrbitCameraController3D(
        target = Vec3(0.0, targetSize * 0.42, 0.0),
        distance = targetSize * 2.7,
        yawRadians = 0.0f,
        pitchRadians = 0.2f,
        minDistance = targetSize * 0.75,
        maxDistance = targetSize * 7.0
    )
}

private fun ViewerModelPreset.matches(config: ViewerConfig): Boolean {
    return modelPath == config.modelPath &&
        mode == config.mode &&
        targetSize == config.targetSize &&
        clipName == config.clipName
}

private fun viewerWindowTitle(
    controls: ViewerControlState,
    model: ViewerLoadedModel
): String {
    val clip = model.selectedClipName()?.let { " | clip: $it" } ?: ""
    val playback = if (controls.animationPaused) "paused" else "${controls.animationSpeed}x"
    return "Kengine 3D Model Viewer - ${controls.currentModelPreset.label}$clip | ${controls.currentLightPreset.label} | $playback"
}

private fun printViewerControls() {
    println(
        """
        Viewer controls:
          Mouse drag             Orbit camera.
          Up/Down arrows         Zoom camera.
          Left/Right arrows      Pan camera target.
          M/N                    Next/previous model preset.
          C/V                    Next/previous animation clip.
          Space                  Pause/resume animation.
          Z/X                    Decrease/increase animation speed.
          B                      Cycle background preset.
          L                      Cycle lighting preset.
          J/K                    Decrease/increase ambient light.
          U/I                    Decrease/increase diffuse light.
          G                      Toggle axes.
          R                      Reset viewer controls and camera.
          H or F1                Print controls.
          T                      Print current viewer status.
          Escape                 Quit.
        """.trimIndent()
    )
}

private fun printViewerStatus(
    controls: ViewerControlState,
    model: ViewerLoadedModel
) {
    val clip = model.selectedClipName() ?: "none"
    println("Viewer status:")
    println("  model=${controls.currentModelPreset.label}")
    println("  mode=${controls.currentModelPreset.mode.name.lowercase()} clip=$clip")
    println("  background=${controls.currentBackground.label} light=${controls.currentLightPreset.label}")
    println("  animationSpeed=${controls.animationSpeed} paused=${controls.animationPaused} axes=${controls.showAxes}")
    println("  ambient=${controls.currentLight.ambientStrength} diffuse=${controls.currentLight.diffuseStrength}")
}

private fun drawAxes(
    debugRenderer: DebugRenderer3D,
    frame: GpuFrame,
    camera: Camera3D,
    targetSize: Double
) {
    val length = targetSize * 0.8
    debugRenderer.line(
        frame = frame,
        camera = camera,
        start = Vec3(0.0, 0.0, 0.0),
        end = Vec3(length, 0.0, 0.0),
        color = Color.fromHex("ff4a58")
    )
    debugRenderer.line(
        frame = frame,
        camera = camera,
        start = Vec3(0.0, 0.0, 0.0),
        end = Vec3(0.0, length, 0.0),
        color = Color.fromHex("6dff82")
    )
    debugRenderer.line(
        frame = frame,
        camera = camera,
        start = Vec3(0.0, 0.0, 0.0),
        end = Vec3(0.0, 0.0, length),
        color = Color.fromHex("5ca8ff")
    )
}

private fun drawLightDirection(
    debugRenderer: DebugRenderer3D,
    frame: GpuFrame,
    camera: Camera3D,
    lightDirection: Vec3,
    targetSize: Double
) {
    val direction = normalize(Vec3(-lightDirection.x, -lightDirection.y, -lightDirection.z))
    val start = Vec3(0.0, targetSize * 0.62, 0.0)
    val length = targetSize * 0.95
    debugRenderer.ray(
        frame = frame,
        camera = camera,
        origin = start,
        direction = direction,
        length = length,
        color = Color.fromHex("ffe070")
    )
    debugRenderer.wireSphere(
        frame = frame,
        camera = camera,
        center = Vec3(
            start.x + direction.x * length,
            start.y + direction.y * length,
            start.z + direction.z * length
        ),
        radius = targetSize * 0.045,
        color = Color.fromHex("ffe070")
    )
}

private fun normalize(value: Vec3): Vec3 {
    val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
    if (length <= 0.0000001) {
        return Vec3(0.0, 1.0, 0.0)
    }
    return Vec3(value.x / length, value.y / length, value.z / length)
}
