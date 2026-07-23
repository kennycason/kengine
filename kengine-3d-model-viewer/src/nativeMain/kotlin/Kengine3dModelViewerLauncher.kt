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
import com.kengine.three.ModelFormat3D
import com.kengine.three.ModelInfo3D
import com.kengine.three.ModelLoader3D
import com.kengine.three.ModelLoadOptions3D
import com.kengine.three.ModelSourceCache3D
import com.kengine.three.Node3D
import com.kengine.three.OrbitCameraController3D
import com.kengine.three.ParsedModel3D
import com.kengine.three.Scene3D
import com.kengine.three.SceneModel3D
import com.kengine.three.SceneRenderer3D
import com.kengine.three.createInstance
import com.kengine.three.importer.ModelAssetPreflightResult3D
import com.kengine.three.importer.ModelAssetPreflightStatus3D
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
const val DEFAULT_TARGET_SIZE = 2.2
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
        initialModelPresets = modelPresets,
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
            val assetResolver = ModelAssetPathResolver3D(sourceAssetRoot = config.assetRoot)
            val modelAssets = ModelAssetLoader3D(
                gpu = this,
                resources = resources,
                resolver = assetResolver,
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
            val assetHealth = ViewerAssetHealthCache(assetResolver)

            fun activateModel(
                preset: ViewerModelPreset,
                reload: Boolean = false
            ): ViewerLoadedModel {
                val previousForReload = if (reload) {
                    modelCache.remove(preset)
                } else {
                    null
                }
                val loadedModel = try {
                    modelCache.getOrPut(preset) {
                        loadViewerModel(modelAssets, preset)
                    }
                } catch (error: Throwable) {
                    previousForReload?.let { modelCache[preset] = it }
                    throw error
                }
                scene.clear()
                previousForReload?.cleanup()
                loadedModel.addTo(scene)
                controls.resetAnimationClock()
                printLoadedModel(preset, loadedModel)
                assetHealth.rememberLoaded(preset, loadedModel.info)
                return loadedModel
            }

            var activeModel = activateModel(controls.currentModelPreset)
            fun createCurrentCameraController(): OrbitCameraController3D {
                return createCameraController(
                    targetSize = controls.currentModelPreset.targetSize,
                    preset = controls.currentCameraPreset
                )
            }
            var cameraController = createCurrentCameraController()
            var viewerStatusText = "READY GLB GLTF OBJ"
            var pendingModelLoad: ViewerModelPreset? = null
            fun updateWindowTitle() {
                SDL_SetWindowTitle(sdl.window, viewerWindowTitle(controls, activeModel))
            }
            fun setViewerStatus(message: String) {
                viewerStatusText = message
            }
            fun printMessage(message: String) {
                println(message)
                setViewerStatus(message)
                updateWindowTitle()
            }
            fun printModelLoadError(error: Throwable) {
                val status = viewerModelLoadStatus(error)
                val details = viewerModelLoadDetails(error)
                println(status)
                if (!status.endsWith(details)) {
                    println("  details: $details")
                }
                setViewerStatus(status)
                updateWindowTitle()
            }
            fun activeAssetHealthText(): String {
                return assetHealth.get(controls.currentModelPreset)?.inspectorLine()
                    ?: viewerLoadedAssetHealthLine(activeModel.info)
            }
            fun printModelPreflight() {
                val report = assetHealth.inspect(
                    preset = controls.currentModelPreset,
                    resolvedPath = activeModel.info.assetPath,
                    refresh = true
                )
                val result = report.result
                println("Preflight ${report.resolvedPath}")
                println("  status=${result.status.name.lowercase()} action=${result.plan.action.name.lowercase()}")
                println("  message=${result.message}")
                result.modelInfo?.let { info ->
                    println("  format=${info.format} vertices=${info.vertexCount} meshes=${info.meshCount} primitives=${info.primitiveCount}")
                    println("  materials=${info.materialCount} textures=${info.textureCount} images=${info.imageCount}")
                    println("  animations=${info.animationCount} skins=${info.skinCount} slots=${viewerTextureSlotSummary(info)}")
                }
                setViewerStatus(viewerPreflightStatus(result))
                updateWindowTitle()
            }
            fun printModelPresetPreflightReport() {
                val reports = assetHealth.inspectAll(controls.modelPresetsSnapshot(), refresh = true)
                println("Model preset preflight report:")
                reports.forEachIndexed { index, report ->
                    println("  ${report.consoleLine(index)}")
                    println("     path=${report.resolvedPath}")
                }
                printMessage(viewerAssetHealthSummary(reports))
            }
            updateWindowTitle()
            val inspectorUi = ViewerInspectorUi(
                controls = controls,
                activeModel = { activeModel },
                statusText = { viewerStatusText },
                assetHealthText = ::activeAssetHealthText,
                onSelectModel = { step ->
                    val preset = controls.selectModel(step)
                    activeModel = activateModel(preset)
                    cameraController = createCurrentCameraController()
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
                    cameraController = createCurrentCameraController()
                },
                onSelectCamera = { step ->
                    printMessage(controls.cycleCameraPreset(step))
                    cameraController = createCurrentCameraController()
                },
                onPreflightModel = ::printModelPreflight,
                onPreflightAllModels = ::printModelPresetPreflightReport,
                onLoadModel = {
                    setViewerStatus("Choose GLB GLTF or OBJ")
                    val selectedPath = chooseViewerModelFile()
                    if (selectedPath == null) {
                        printMessage("Model load cancelled.")
                    } else {
                        try {
                            val preset = viewerModelPresetForFile(selectedPath)
                            pendingModelLoad = preset
                            setViewerStatus("Loading ${preset.label}")
                        } catch (error: Throwable) {
                            printModelLoadError(error)
                        }
                    }
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
                                    cameraController = createCurrentCameraController()
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
                                is ViewerControlAction.SelectCameraPreset -> {
                                    printMessage(controls.selectCameraPreset(controlAction.index))
                                    cameraController = createCurrentCameraController()
                                }
                                ViewerControlAction.PreflightModel -> {
                                    printModelPreflight()
                                }
                                ViewerControlAction.PreflightAllModels -> {
                                    printModelPresetPreflightReport()
                                }
                                ViewerControlAction.ResetView -> {
                                    printMessage("Viewer controls reset.")
                                    cameraController = createCurrentCameraController()
                                }
                                ViewerControlAction.PrintHelp -> {
                                    printViewerControls()
                                }
                                ViewerControlAction.PrintStatus -> {
                                    printViewerStatus(controls, activeModel, activeAssetHealthText())
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

                    pendingModelLoad?.let { preset ->
                        pendingModelLoad = null
                        try {
                            val loadedModel = activateModel(preset, reload = true)
                            controls.selectOrAddModel(preset)
                            activeModel = loadedModel
                            cameraController = createCurrentCameraController()
                            printMessage("Loaded file: ${preset.label}")
                        } catch (error: Throwable) {
                            printModelLoadError(error)
                        }
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
    AUTO,
    STATIC,
    NODE_ANIMATED,
    SKINNED
}

private data class ViewerConfig(
    val modelPath: String = DEFAULT_MODEL_PATH,
    val assetRoot: String = DEFAULT_ASSET_ROOT,
    val mode: ViewerModelMode = ViewerModelMode.AUTO,
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
                  --mode <mode>          auto, static, node, or skinned. Defaults to auto.
                  --target-size <size>   Normalized model height/extent target. Defaults to $DEFAULT_TARGET_SIZE.
                  --clip <name>          Animated clip name to preview when mode is auto, node, or skinned.
                  --help                 Print this help.

                Controls:
                  Mouse drag             Orbit camera.
                  Up/Down arrows         Zoom camera.
                  Left/Right arrows      Pan camera target.
                  1/2/3/4                Camera presets: front, three-quarter, side, top.
                  P                      Preflight the active model.
                  A                      Preflight all model presets.
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
                "auto" -> ViewerModelMode.AUTO
                "static" -> ViewerModelMode.STATIC
                "node", "node-animated" -> ViewerModelMode.NODE_ANIMATED
                "skinned", "skinned-textured-lit" -> ViewerModelMode.SKINNED
                else -> throw IllegalArgumentException("--mode must be auto, static, node, or skinned.")
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
        ViewerModelMode.AUTO -> loadViewerModelAuto(loader, preset, options)
        ViewerModelMode.STATIC -> loadStaticViewerModel(loader, preset, options)
        ViewerModelMode.NODE_ANIMATED,
        ViewerModelMode.SKINNED -> loadAnimatedViewerModel(loader, preset, options, preset.mode)
    }
}

private fun loadViewerModelAuto(
    loader: ModelAssetLoader3D,
    preset: ViewerModelPreset,
    options: ModelLoadOptions3D
): ViewerLoadedModel {
    return when (ModelLoader3D.detectFormat(preset.modelPath)) {
        ModelFormat3D.OBJ -> loadStaticViewerModel(loader, preset, options)
        ModelFormat3D.GLB,
        ModelFormat3D.GLTF -> {
            val source = loader.loadSource(ModelAsset3D(preset.modelPath, options))
            when (viewerAutoModeForModel(source.info)) {
                ViewerModelMode.STATIC -> uploadStaticViewerSource(loader, source)
                ViewerModelMode.SKINNED -> loadAnimatedViewerModel(loader, preset, options, ViewerModelMode.SKINNED)
                ViewerModelMode.NODE_ANIMATED -> loadAnimatedViewerModel(
                    loader = loader,
                    preset = preset,
                    options = options,
                    mode = ViewerModelMode.NODE_ANIMATED
                )
                ViewerModelMode.AUTO -> error("AUTO did not resolve to a concrete viewer model mode.")
            }
        }
    }
}

fun viewerAutoModeForModel(info: ModelInfo3D): ViewerModelMode {
    return when {
        info.animationCount <= 0 -> ViewerModelMode.STATIC
        info.skinCount > 0 -> ViewerModelMode.SKINNED
        else -> ViewerModelMode.NODE_ANIMATED
    }
}

private fun loadStaticViewerModel(
    loader: ModelAssetLoader3D,
    preset: ViewerModelPreset,
    options: ModelLoadOptions3D
): ViewerLoadedModel.Static {
    val source = loader.loadSource(ModelAsset3D(preset.modelPath, options))
    return uploadStaticViewerSource(loader, source)
}

private fun uploadStaticViewerSource(
    loader: ModelAssetLoader3D,
    source: ParsedModel3D
): ViewerLoadedModel.Static {
    val model = loader.uploadModel(source)
    return ViewerLoadedModel.Static(
        info = source.info,
        node = Node3D(SceneModel3D(model = model))
    )
}

private fun loadAnimatedViewerModel(
    loader: ModelAssetLoader3D,
    preset: ViewerModelPreset,
    options: ModelLoadOptions3D,
    mode: ViewerModelMode
): ViewerLoadedModel.Animated {
    val asset = when (mode) {
        ViewerModelMode.NODE_ANIMATED -> AnimatedModelAsset3D.nodeAnimatedLit(preset.modelPath, options)
        ViewerModelMode.SKINNED -> AnimatedModelAsset3D.skinnedTexturedLit(preset.modelPath, options)
        ViewerModelMode.AUTO,
        ViewerModelMode.STATIC -> error("$mode is not an animated viewer mode.")
    }
    val source = loader.loadAnimatedSource(asset)
    val animatedModel = loader.uploadAnimatedModel(source)
    val info = source.info
    val clipIndex = selectClipIndex(info, preset.clipName)
    return ViewerLoadedModel.Animated(
        info = info,
        node = Node3D(
            item = animatedModel.createInstance(
                pose = AnimationPose3D(clipIndex = clipIndex)
            )
        ),
        clipIndex = clipIndex
    )
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
    println("  textureSlots=${viewerTextureSlotSummary(info)}")
    if (info.animations.isNotEmpty()) {
        model.selectedClipName()?.let { selected ->
            println("  previewClip=$selected")
        } ?: println("  availableClips=${info.animations.size}")
        println("  clips=${info.animations.take(8).joinToString { it.name }}")
    }
}

private fun createCameraController(
    targetSize: Double,
    preset: ViewerCameraPreset
): OrbitCameraController3D {
    return OrbitCameraController3D(
        target = Vec3(0.0, targetSize * preset.targetHeightMultiplier, 0.0),
        distance = targetSize * preset.distanceMultiplier,
        yawRadians = preset.yawRadians,
        pitchRadians = preset.pitchRadians,
        minDistance = (targetSize * 0.12).coerceAtLeast(0.12),
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
    return "Kengine 3D Model Viewer - ${controls.currentModelPreset.label}$clip | ${controls.currentCameraPreset.label} | ${controls.currentLightPreset.label} | $playback"
}

private fun printViewerControls() {
    println(
        """
        Viewer controls:
          Mouse drag             Orbit camera.
          Up/Down arrows         Zoom camera.
          Left/Right arrows      Pan camera target.
          1/2/3/4                Camera presets: front, three-quarter, side, top.
          P                      Preflight the active model.
          A                      Preflight all model presets.
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
    model: ViewerLoadedModel,
    assetHealthText: String
) {
    val clip = model.selectedClipName() ?: "none"
    println("Viewer status:")
    println("  model=${controls.currentModelPreset.label}")
    println("  mode=${controls.currentModelPreset.mode.name.lowercase()} clip=$clip")
    println("  assetHealth=$assetHealthText")
    println("  camera=${controls.currentCameraPreset.label}")
    println("  background=${controls.currentBackground.label} light=${controls.currentLightPreset.label}")
    println("  animationSpeed=${controls.animationSpeed} paused=${controls.animationPaused} axes=${controls.showAxes}")
    println("  ambient=${controls.currentLight.ambientStrength} diffuse=${controls.currentLight.diffuseStrength}")
    println("  textureSlots=${viewerTextureSlotSummary(model.info)}")
}

private fun viewerTextureSlotSummary(info: ModelInfo3D): String {
    val slots = info.textureSlotUsage
    if (slots.totalSlotCount == 0) {
        return "none"
    }
    val values = mutableListOf<String>()
    if (slots.baseColor > 0) values += "base=${slots.baseColor}"
    if (slots.normal > 0) values += "normal=${slots.normal} rendered"
    if (slots.metallicRoughness > 0) values += "metallicRoughness=${slots.metallicRoughness}"
    if (slots.roughness > 0) values += "roughness=${slots.roughness}"
    if (slots.metallic > 0) values += "metallic=${slots.metallic}"
    if (slots.specular > 0) values += "specular=${slots.specular}"
    if (slots.emissive > 0) values += "emissive=${slots.emissive}"
    if (slots.ambient > 0) values += "ambient=${slots.ambient}"
    if (slots.alpha > 0) values += "alpha=${slots.alpha}"
    if (slots.displacement > 0) values += "displacement=${slots.displacement}"
    val pending = slots.secondarySlotCount - slots.normal
    if (pending > 0) {
        values += "metadataOnly=$pending"
    }
    return values.joinToString(", ")
}

private fun viewerPreflightStatus(result: ModelAssetPreflightResult3D): String {
    return when (result.status) {
        ModelAssetPreflightStatus3D.LOADABLE -> {
            val info = result.modelInfo
            if (info == null) {
                "Preflight OK"
            } else {
                "Preflight OK ${info.format.name} ${info.preflightSummary()}"
            }
        }
        ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED ->
            "Preflight export ${result.plan.inputFormat?.label ?: "source"} to GLB"
        ModelAssetPreflightStatus3D.UNSUPPORTED ->
            "Preflight unsupported format"
        ModelAssetPreflightStatus3D.INVALID_RUNTIME_ASSET ->
            "Preflight failed"
    }
}

private fun ModelInfo3D.preflightSummary(): String {
    val values = mutableListOf<String>()
    if (primitiveCount > 0) values += "P$primitiveCount"
    if (vertexCount > 0) values += "V$vertexCount"
    if (materialCount > 0) values += "M$materialCount"
    if (textureCount > 0) values += "T$textureCount"
    if (animationCount > 0) values += "A$animationCount"
    if (skinCount > 0) values += "S$skinCount"
    return values.joinToString(" ").ifBlank { "READY" }
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
