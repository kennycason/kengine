import com.kengine.createGameContext
import com.kengine.hooks.context.useContext
import com.kengine.graphics.Color
import com.kengine.log.Logger
import com.kengine.math.Vec3
import com.kengine.sdl.RenderBackend
import com.kengine.three.GpuContext
import com.kengine.three.GpuMesh
import com.kengine.three.GpuTexture
import com.kengine.three.MeshRenderer3D
import com.kengine.three.PerspectiveCamera
import com.kengine.three.PrimitiveRenderer3D
import com.kengine.three.TexturedGpuMesh
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
            val camera = PerspectiveCamera(
                position = Vec3(0.0, 0.0, 0.0),
                fovDegrees = 58f,
                near = 0.1f,
                far = 100f
            )
            val cube = GpuMesh.cube(this)
            val texturedCube = TexturedGpuMesh.cube(this)
            val checkerboard = GpuTexture.checkerboard(this)
            val meshes = MeshRenderer3D(this)
            val texturedMeshes = TexturedMeshRenderer3D(this)
            val primitives = PrimitiveRenderer3D(this)

            try {
                while (isRunning) {
                    sdlEvent.pollEvents()
                    action.update()

                    val elapsedSeconds = SDL_GetTicks().toDouble() / 1000.0
                    val red = (0.035 + (sin(elapsedSeconds * 0.9) + 1.0) * 0.025).toFloat()
                    val green = (0.045 + (sin(elapsedSeconds * 0.7 + 2.0) + 1.0) * 0.025).toFloat()
                    val blue = (0.065 + (sin(elapsedSeconds * 0.5 + 4.0) + 1.0) * 0.035).toFloat()

                    render(red, green, blue, 1f, enableDepth = true) { frame ->
                        val time = elapsedSeconds.toFloat()

                        primitives.quad(
                            frame = frame,
                            center = Vec3(0.0, -0.58, -5.8),
                            width = 6.0f,
                            height = 1.5f,
                            color = Color.fromHex("24405f"),
                            rotationRadians = -time * 0.18f
                        )
                        meshes.draw(
                            frame = frame,
                            mesh = cube,
                            transform = Transform3D(
                                position = Vec3(-1.25, 0.0, -3.45),
                                rotation = Vec3(
                                    (time * 0.72f).toDouble(),
                                    (time * 1.05f).toDouble(),
                                    (time * 0.28f).toDouble()
                                )
                            ),
                            camera = camera
                        )
                        texturedMeshes.draw(
                            frame = frame,
                            mesh = texturedCube,
                            texture = checkerboard,
                            transform = Transform3D(
                                position = Vec3(1.25, 0.02, -3.45),
                                rotation = Vec3(
                                    (time * 0.48f).toDouble(),
                                    (-time * 0.86f).toDouble(),
                                    (time * 0.18f).toDouble()
                                )
                            ),
                            camera = camera
                        )
                        primitives.triangle(
                            frame = frame,
                            center = Vec3(0.62, -0.36, -4.6),
                            size = 1.65f,
                            color = Color.fromHex("f0c84b"),
                            rotationRadians = time * 0.4f
                        )
                    }
                    SDL_Delay(16u)
                }
            } finally {
                primitives.cleanup()
                texturedMeshes.cleanup()
                meshes.cleanup()
                checkerboard.cleanup()
                texturedCube.cleanup()
                cube.cleanup()
            }
        }
    }
}
