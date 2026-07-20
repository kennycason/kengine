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
import com.kengine.three.DirectionalLight3D
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
import com.kengine.three.setPose
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import sdl3.SDL_GetTicks

private const val WINDOW_WIDTH = 1280
private const val WINDOW_HEIGHT = 760
private const val DEFAULT_ASSET_ROOT = "../games/mario-3d/assets"
private const val DEFAULT_MODEL_PATH = "models/Mario64Animated.glb"
private const val DEFAULT_TARGET_SIZE = 2.2

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
            val scene = Scene3D(
                DirectionalLight3D(
                    direction = Vec3(-0.35, -0.85, -0.45),
                    color = Color.fromHex("fff4dc"),
                    ambientStrength = 0.48f,
                    diffuseStrength = 0.82f
                )
            )
            val model = loadViewerModel(modelAssets, scene, config)
            resources.track(scene)
            printLoadedModel(config, model.info)

            val cameraController = OrbitCameraController3D(
                target = Vec3(0.0, config.targetSize * 0.42, 0.0),
                distance = config.targetSize * 2.7,
                yawRadians = 0.0f,
                pitchRadians = 0.2f,
                minDistance = config.targetSize * 0.75,
                maxDistance = config.targetSize * 7.0
            )
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

                    if (keyboardState.isEscapePressed()) {
                        isRunning = false
                    }

                    cameraController.update(
                        mouse = mouse.mouse,
                        keyboard = keyboardState,
                        deltaSeconds = deltaSeconds
                    )
                    model.update(elapsedSeconds)
                    scene.prepareForDraw()
                    val camera = cameraController.camera()

                    render(0.045f, 0.055f, 0.07f, 1f, enableDepth = true) { frame ->
                        sceneRenderer.draw(scene, frame, camera)
                        drawAxes(debugRenderer, frame, camera, config.targetSize)
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

private enum class ViewerModelMode {
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

private sealed class ViewerLoadedModel {
    abstract val info: ModelInfo3D

    open fun update(elapsedSeconds: Double) {
    }

    data class Static(
        override val info: ModelInfo3D,
        val node: Node3D<SceneModel3D>
    ) : ViewerLoadedModel()

    data class Animated(
        override val info: ModelInfo3D,
        val node: Node3D<AnimatedModelInstance3D>,
        val clipIndex: Int,
        val clipDurationSeconds: Double
    ) : ViewerLoadedModel() {
        override fun update(elapsedSeconds: Double) {
            val time = if (clipDurationSeconds > 0.0) {
                elapsedSeconds % clipDurationSeconds
            } else {
                elapsedSeconds
            }
            node.setPose(AnimationPose3D(clipIndex = clipIndex, timeSeconds = time))
        }
    }
}

private fun loadViewerModel(
    loader: ModelAssetLoader3D,
    scene: Scene3D,
    config: ViewerConfig
): ViewerLoadedModel {
    val options = ModelLoadOptions3D(
        targetSize = config.targetSize,
        defaultColor = Color.fromHex("ffffff")
    )
    return when (config.mode) {
        ViewerModelMode.STATIC -> {
            val asset = ModelAsset3D(config.modelPath, options)
            val source = loader.loadSource(asset)
            val model = loader.uploadModel(source)
            ViewerLoadedModel.Static(
                info = source.info,
                node = scene.addModelNode(model)
            )
        }
        ViewerModelMode.NODE_ANIMATED,
        ViewerModelMode.SKINNED -> {
            val asset = when (config.mode) {
                ViewerModelMode.NODE_ANIMATED -> AnimatedModelAsset3D.nodeAnimatedLit(config.modelPath, options)
                ViewerModelMode.SKINNED -> AnimatedModelAsset3D.skinnedTexturedLit(config.modelPath, options)
                ViewerModelMode.STATIC -> error("Static mode is handled separately.")
            }
            val source = loader.loadAnimatedSource(asset)
            val animatedModel = loader.uploadAnimatedModel(source)
            val info = source.info
            val clipIndex = selectClipIndex(info, config.clipName)
            val clipDuration = info.animations.getOrNull(clipIndex)?.durationSeconds ?: 0.0
            ViewerLoadedModel.Animated(
                info = info,
                node = scene.addAnimatedModelNode(
                    model = animatedModel,
                    pose = AnimationPose3D(clipIndex = clipIndex)
                ),
                clipIndex = clipIndex,
                clipDurationSeconds = clipDuration
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
    config: ViewerConfig,
    info: ModelInfo3D
) {
    println("Loaded ${config.mode.name.lowercase()} model: ${info.assetPath}")
    println("  format=${info.format} vertices=${info.vertexCount} meshes=${info.meshCount} primitives=${info.primitiveCount}")
    println("  materials=${info.materialCount} textures=${info.textureCount} skins=${info.skinCount} animations=${info.animationCount}")
    if (info.animations.isNotEmpty()) {
        val selected = config.clipName ?: info.animations.first().name
        println("  previewClip=$selected")
        println("  clips=${info.animations.take(8).joinToString { it.name }}")
    }
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
