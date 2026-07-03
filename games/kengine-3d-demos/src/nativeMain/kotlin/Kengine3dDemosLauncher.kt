import com.kengine.createGameContext
import com.kengine.hooks.context.useContext
import com.kengine.graphics.Color
import com.kengine.log.Logger
import com.kengine.math.Vec3
import com.kengine.sdl.RenderBackend
import com.kengine.three.DirectionalLight3D
import com.kengine.three.GpuContext
import com.kengine.three.GpuMesh
import com.kengine.three.GpuTexture
import com.kengine.three.LitMeshRenderer3D
import com.kengine.three.MeshRenderer3D
import com.kengine.three.ObjMeshLoadOptions
import com.kengine.three.ObjMeshLoader
import com.kengine.three.OrbitCameraController3D
import com.kengine.three.PrimitiveRenderer3D
import com.kengine.three.TexturedGpuMesh
import com.kengine.three.TexturedLitMeshRenderer3D
import com.kengine.three.TexturedMeshRenderer3D
import com.kengine.three.Transform3D
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import sdl3.SDL_GetTicks
import kotlin.math.sin

@OptIn(ExperimentalForeignApi::class)
fun main() {
    createGameContext(
        title = "Kengine - 3D Demos",
        width = 960,
        height = 540,
        logLevel = Logger.Level.INFO,
        renderBackend = RenderBackend.SDL_GPU_3D
    ) {
        useContext(GpuContext.create(sdl), cleanup = true) {
            val cameraController = OrbitCameraController3D(
                target = Vec3(0.0, -0.25, -4.25),
                distance = 4.65,
                yawRadians = 0f,
                pitchRadians = 0.06f
            )
            val cube = GpuMesh.cube(this)
            val texturedCube = TexturedGpuMesh.cube(this)
            val importedShip = ObjMeshLoader.loadLit(
                gpu = this,
                assetPath = "assets/models/kenney-space-kit/craft_racer.obj",
                options = ObjMeshLoadOptions(targetSize = 1.55)
            )
            val importedTurret = ObjMeshLoader.loadLit(
                gpu = this,
                assetPath = "assets/models/kenney-space-kit/turret_double.obj",
                options = ObjMeshLoadOptions(targetSize = 1.35)
            )
            val uvTestMeteor = ObjMeshLoader.loadTexturedLit(
                gpu = this,
                assetPath = "assets/models/kenney-space-kit/meteor_detailed.obj",
                options = ObjMeshLoadOptions(targetSize = 0.78)
            )
            val modelLight = DirectionalLight3D(
                direction = Vec3(-0.45, -0.8, -0.55),
                color = Color.fromHex("f8fbff"),
                ambientStrength = 0.34f,
                diffuseStrength = 0.82f
            )
            val checkerboard = GpuTexture.checkerboard(this)
            val uvTestTexture = GpuTexture.fromFile(
                gpu = this,
                assetPath = "assets/textures/kenney-space-kit/meteor_detailed.png"
            )
            val meshes = MeshRenderer3D(this)
            val litMeshes = LitMeshRenderer3D(this)
            val texturedLitMeshes = TexturedLitMeshRenderer3D(this)
            val texturedMeshes = TexturedMeshRenderer3D(this)
            val primitives = PrimitiveRenderer3D(this)
            var previousTicks = SDL_GetTicks()

            try {
                while (isRunning) {
                    sdlEvent.pollEvents()
                    action.update()

                    val ticks = SDL_GetTicks()
                    val deltaSeconds = ((ticks - previousTicks).toDouble() / 1000.0).coerceIn(0.0, 0.1)
                    previousTicks = ticks
                    cameraController.update(
                        mouse = mouse.mouse,
                        keyboard = keyboard.keyboard,
                        deltaSeconds = deltaSeconds
                    )

                    val elapsedSeconds = ticks.toDouble() / 1000.0
                    val red = (0.035 + (sin(elapsedSeconds * 0.9) + 1.0) * 0.025).toFloat()
                    val green = (0.045 + (sin(elapsedSeconds * 0.7 + 2.0) + 1.0) * 0.025).toFloat()
                    val blue = (0.065 + (sin(elapsedSeconds * 0.5 + 4.0) + 1.0) * 0.035).toFloat()

                    render(red, green, blue, 1f, enableDepth = true) { frame ->
                        val time = elapsedSeconds.toFloat()
                        val camera = cameraController.camera()

                        primitives.quad(
                            frame = frame,
                            center = Vec3(0.0, -2.05, -5.25),
                            width = 5.2f,
                            height = 0.52f,
                            color = Color.fromHex("24405f"),
                            rotationRadians = -time * 0.1f
                        )
                        meshes.draw(
                            frame = frame,
                            mesh = cube,
                            transform = Transform3D(
                                position = Vec3(-2.72, -1.5, -4.35),
                                rotation = Vec3(
                                    (time * 0.72f).toDouble(),
                                    (time * 1.05f).toDouble(),
                                    (time * 0.28f).toDouble()
                                ),
                                scale = Vec3(0.54, 0.54, 0.54)
                            ),
                            camera = camera
                        )
                        litMeshes.draw(
                            frame = frame,
                            mesh = importedShip,
                            transform = Transform3D(
                                position = Vec3(-1.65, 0.72, -4.15),
                                rotation = Vec3(
                                    0.18 + sin(elapsedSeconds * 0.55) * 0.16,
                                    (time * 0.82f).toDouble(),
                                    0.0
                                )
                            ),
                            camera = camera,
                            light = modelLight
                        )
                        litMeshes.draw(
                            frame = frame,
                            mesh = importedTurret,
                            transform = Transform3D(
                                position = Vec3(1.65, 0.68, -4.18),
                                rotation = Vec3(
                                    -0.08,
                                    (-time * 0.62f).toDouble(),
                                    0.0
                                )
                            ),
                            camera = camera,
                            light = modelLight
                        )
                        texturedLitMeshes.draw(
                            frame = frame,
                            mesh = uvTestMeteor,
                            texture = uvTestTexture,
                            transform = Transform3D(
                                position = Vec3(0.0, -0.55, -4.05),
                                rotation = Vec3(
                                    (time * 0.38f).toDouble(),
                                    (time * 1.15f).toDouble(),
                                    0.2
                                )
                            ),
                            camera = camera,
                            light = modelLight
                        )
                        texturedMeshes.draw(
                            frame = frame,
                            mesh = texturedCube,
                            texture = checkerboard,
                            transform = Transform3D(
                                position = Vec3(2.72, -1.5, -4.35),
                                rotation = Vec3(
                                    (time * 0.48f).toDouble(),
                                    (-time * 0.86f).toDouble(),
                                    (time * 0.18f).toDouble()
                                ),
                                scale = Vec3(0.54, 0.54, 0.54)
                            ),
                            camera = camera
                        )
                        primitives.triangle(
                            frame = frame,
                            center = Vec3(0.0, -1.78, -3.9),
                            size = 0.46f,
                            color = Color.fromHex("f0c84b"),
                            rotationRadians = time * 0.4f
                        )
                    }
                    mouse.mouse.clearFrameState()
                    SDL_Delay(16u)
                }
            } finally {
                primitives.cleanup()
                texturedMeshes.cleanup()
                texturedLitMeshes.cleanup()
                litMeshes.cleanup()
                meshes.cleanup()
                uvTestTexture.cleanup()
                checkerboard.cleanup()
                uvTestMeteor.cleanup()
                importedTurret.cleanup()
                importedShip.cleanup()
                texturedCube.cleanup()
                cube.cleanup()
            }
        }
    }
}
