import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.input.keyboard.KeyboardInputEventSubscriber
import com.kengine.input.keyboard.Keys
import com.kengine.input.mouse.MouseInputEventSubscriber
import com.kengine.log.Logger
import com.kengine.math.Vec3
import com.kengine.sdl.RenderBackend
import com.kengine.three.GpuContext
import com.kengine.three.Mat4
import com.kengine.three.MeshRenderer3D
import com.kengine.three.PerspectiveCamera
import com.kengine.three.PrimitiveRenderer3D
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_Delay
import sdl3.SDL_GetTicks
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.tan

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val width = 960
    val height = 540
    val fovDegrees = 52f

    createGameContext(
        title = "Kengine - 3D Rubik's Cube",
        width = width,
        height = height,
        logLevel = Logger.Level.INFO,
        renderBackend = RenderBackend.SDL_GPU_3D
    ) {
        com.kengine.hooks.context.useContext(GpuContext.create(sdl), cleanup = true) {
            val camera = PerspectiveCamera(
                position = Vec3(0.0, 0.0, 0.0),
                fovDegrees = fovDegrees,
                near = 0.1f,
                far = 100f
            )
            val meshes = MeshRenderer3D(this)
            val primitives = PrimitiveRenderer3D(this)
            val rubiksCube = RubiksCube(this)
            val pointer = CubePointerControls(width, height, fovDegrees)
            val keys = KeyEdges(
                Keys.S,
                Keys.C,
                Keys.U,
                Keys.D,
                Keys.L,
                Keys.R,
                Keys.F,
                Keys.B,
                Keys.ONE,
                Keys.TWO,
                Keys.THREE,
                Keys.FOUR,
                Keys.FIVE,
                Keys.SIX
            )

            var previousTicks = SDL_GetTicks()

            try {
                while (isRunning) {
                    sdlEvent.pollEvents()
                    action.update()

                    val now = SDL_GetTicks()
                    val deltaSeconds = ((now - previousTicks).toDouble() / 1000.0).toFloat()
                    previousTicks = now

                    val mouseInput = mouse.mouse
                    val keyboardInput = keyboard.keyboard

                    pointer.update(mouseInput)?.let { rubiksCube.enqueue(it) }
                    keys.update(keyboardInput)
                    handleKeys(keys, keyboardInput, rubiksCube)
                    rubiksCube.update(deltaSeconds.coerceAtMost(0.05f))

                    val root = Mat4.translation(Vec3(0.0, 0.0, -7.2)) *
                        Mat4.rotationX(pointer.pitchRadians) *
                        Mat4.rotationY(pointer.yawRadians)

                    render(0.024f, 0.027f, 0.034f, 1f, enableDepth = true) { frame ->
                        primitives.quad(
                            frame = frame,
                            center = Vec3(0.0, -2.35, -8.7),
                            width = 5.2f,
                            height = 1.1f,
                            color = Color.fromHex("202936"),
                            rotationRadians = 0f
                        )
                        rubiksCube.draw(frame, meshes, root, camera)
                    }

                    mouseInput.clearFrameState()
                    SDL_Delay(16u)
                }
            } finally {
                rubiksCube.cleanup()
                primitives.cleanup()
                meshes.cleanup()
            }
        }
    }
}

private fun handleKeys(
    keys: KeyEdges,
    keyboard: KeyboardInputEventSubscriber,
    rubiksCube: RubiksCube
) {
    val reverse = keyboard.isPressed(Keys.LSHIFT) || keyboard.isPressed(Keys.RSHIFT)
    val direction = if (reverse) -1 else 1

    if (keys.justPressed(Keys.S)) {
        rubiksCube.scramble()
    }
    if (keys.justPressed(Keys.C)) {
        rubiksCube.reset()
    }

    if (keys.justPressed(Keys.U) || keys.justPressed(Keys.ONE)) {
        rubiksCube.enqueue(SliceMove(SliceAxis.Y, 1, direction))
    }
    if (keys.justPressed(Keys.D) || keys.justPressed(Keys.TWO)) {
        rubiksCube.enqueue(SliceMove(SliceAxis.Y, -1, -direction))
    }
    if (keys.justPressed(Keys.L) || keys.justPressed(Keys.THREE)) {
        rubiksCube.enqueue(SliceMove(SliceAxis.X, -1, direction))
    }
    if (keys.justPressed(Keys.R) || keys.justPressed(Keys.FOUR)) {
        rubiksCube.enqueue(SliceMove(SliceAxis.X, 1, -direction))
    }
    if (keys.justPressed(Keys.F) || keys.justPressed(Keys.FIVE)) {
        rubiksCube.enqueue(SliceMove(SliceAxis.Z, 1, -direction))
    }
    if (keys.justPressed(Keys.B) || keys.justPressed(Keys.SIX)) {
        rubiksCube.enqueue(SliceMove(SliceAxis.Z, -1, direction))
    }
}

private class CubePointerControls(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val fovDegrees: Float
) {
    var yawRadians: Float = -0.58f
        private set
    var pitchRadians: Float = 0.42f
        private set

    private var mode = PointerMode.NONE
    private var pickedFace: PickedFace? = null
    private var lastX = 0.0
    private var lastY = 0.0
    private var startX = 0.0
    private var startY = 0.0

    fun update(mouse: MouseInputEventSubscriber): SliceMove? {
        val cursor = mouse.cursor()
        if (mouse.wasLeftJustPressed()) {
            startX = cursor.x
            startY = cursor.y
            lastX = cursor.x
            lastY = cursor.y
            pickedFace = pickFace(cursor.x, cursor.y)
            mode = if (pickedFace == null) PointerMode.ORBIT else PointerMode.TURN
        }

        if (!mouse.isLeftPressed()) {
            mode = PointerMode.NONE
            pickedFace = null
            return null
        }

        return when (mode) {
            PointerMode.ORBIT -> {
                val dx = cursor.x - lastX
                val dy = cursor.y - lastY
                yawRadians += (dx * 0.0085).toFloat()
                pitchRadians = (pitchRadians + (dy * 0.0085).toFloat())
                    .coerceIn(-MAX_PITCH, MAX_PITCH)
                lastX = cursor.x
                lastY = cursor.y
                null
            }
            PointerMode.TURN -> {
                val dx = cursor.x - startX
                val dy = cursor.y - startY
                if (dx * dx + dy * dy < TURN_DRAG_THRESHOLD * TURN_DRAG_THRESHOLD) {
                    null
                } else {
                    mode = PointerMode.WAIT_RELEASE
                    pickedFace?.toMove(dx, dy)
                }
            }
            PointerMode.WAIT_RELEASE,
            PointerMode.NONE -> null
        }
    }

    private fun pickFace(mouseX: Double, mouseY: Double): PickedFace? {
        val aspect = screenWidth.toDouble() / screenHeight.toDouble()
        val halfFovTan = tan(fovDegrees.toDouble() * PI / 180.0 * 0.5)
        val ndcX = (mouseX / screenWidth.toDouble()) * 2.0 - 1.0
        val ndcY = 1.0 - (mouseY / screenHeight.toDouble()) * 2.0
        val cameraRay = DVec3(
            ndcX * aspect * halfFovTan,
            ndcY * halfFovTan,
            -1.0
        ).normalized()

        val localOrigin = rotateY(
            rotateX(DVec3(0.0, 0.0, CUBE_DISTANCE), -pitchRadians.toDouble()),
            -yawRadians.toDouble()
        )
        val localRay = rotateY(
            rotateX(cameraRay, -pitchRadians.toDouble()),
            -yawRadians.toDouble()
        ).normalized()

        val hit = intersectCube(localOrigin, localRay) ?: return null
        val axis = hit.axis()
        val layer = when (axis) {
            SliceAxis.X -> if (hit.point.x >= 0.0) 1 else -1
            SliceAxis.Y -> if (hit.point.y >= 0.0) 1 else -1
            SliceAxis.Z -> if (hit.point.z >= 0.0) 1 else -1
        }

        return PickedFace(
            axis = axis,
            layer = layer,
            hitPoint = hit.point,
            screenX = mouseX,
            screenY = mouseY,
            pointer = this
        )
    }

    private fun intersectCube(origin: DVec3, direction: DVec3): CubeHit? {
        var tMin = Double.NEGATIVE_INFINITY
        var tMax = Double.POSITIVE_INFINITY

        fun testAxis(start: Double, velocity: Double): Boolean {
            if (abs(velocity) < 0.000001) {
                return start in -CUBE_EXTENT..CUBE_EXTENT
            }

            val t1 = (-CUBE_EXTENT - start) / velocity
            val t2 = (CUBE_EXTENT - start) / velocity
            val near = minOf(t1, t2)
            val far = maxOf(t1, t2)
            tMin = maxOf(tMin, near)
            tMax = minOf(tMax, far)
            return tMin <= tMax
        }

        if (!testAxis(origin.x, direction.x)) return null
        if (!testAxis(origin.y, direction.y)) return null
        if (!testAxis(origin.z, direction.z)) return null
        if (tMax < 0.0) return null

        val t = if (tMin >= 0.0) tMin else tMax
        return CubeHit(origin + direction * t)
    }

    fun project(localPoint: DVec3): DVec2 {
        val rotated = rotateX(rotateY(localPoint, yawRadians.toDouble()), pitchRadians.toDouble())
        val cameraPoint = DVec3(rotated.x, rotated.y, rotated.z - CUBE_DISTANCE)
        val aspect = screenWidth.toDouble() / screenHeight.toDouble()
        val halfFovTan = tan(fovDegrees.toDouble() * PI / 180.0 * 0.5)
        val ndcX = (cameraPoint.x / -cameraPoint.z) / (halfFovTan * aspect)
        val ndcY = (cameraPoint.y / -cameraPoint.z) / halfFovTan
        return DVec2(
            (ndcX + 1.0) * 0.5 * screenWidth.toDouble(),
            (1.0 - ndcY) * 0.5 * screenHeight.toDouble()
        )
    }

    companion object {
        private val MAX_PITCH = ((PI / 2.0) * 0.78).toFloat()
        private const val CUBE_DISTANCE = 7.2
        private const val CUBE_EXTENT = 1.42
        private const val TURN_DRAG_THRESHOLD = 18.0
    }
}

private enum class PointerMode {
    NONE,
    ORBIT,
    TURN,
    WAIT_RELEASE
}

private data class PickedFace(
    val axis: SliceAxis,
    val layer: Int,
    val hitPoint: DVec3,
    val screenX: Double,
    val screenY: Double,
    val pointer: CubePointerControls
) {
    fun toMove(dx: Double, dy: Double): SliceMove {
        val positiveDirectionPoint = rotateAroundAxis(hitReferencePoint(), axis, 0.16)
        val baseScreen = pointer.project(hitReferencePoint())
        val positiveScreen = pointer.project(positiveDirectionPoint)
        val positiveMotion = positiveScreen - baseScreen
        val drag = DVec2(dx, dy)
        val direction = if (positiveMotion.dot(drag) >= 0.0) 1 else -1
        return SliceMove(axis, layer, direction)
    }

    private fun hitReferencePoint(): DVec3 {
        val perpendicularMagnitude = when (axis) {
            SliceAxis.X -> hitPoint.y * hitPoint.y + hitPoint.z * hitPoint.z
            SliceAxis.Y -> hitPoint.x * hitPoint.x + hitPoint.z * hitPoint.z
            SliceAxis.Z -> hitPoint.x * hitPoint.x + hitPoint.y * hitPoint.y
        }

        if (perpendicularMagnitude > 0.08) {
            return hitPoint
        }

        return when (axis) {
            SliceAxis.X -> DVec3(layer * 1.42, 0.85, 0.0)
            SliceAxis.Y -> DVec3(0.85, layer * 1.42, 0.0)
            SliceAxis.Z -> DVec3(0.0, 0.85, layer * 1.42)
        }
    }
}

private data class CubeHit(
    val point: DVec3
) {
    fun axis(): SliceAxis {
        val ax = abs(point.x)
        val ay = abs(point.y)
        val az = abs(point.z)
        return when {
            ax >= ay && ax >= az -> SliceAxis.X
            ay >= ax && ay >= az -> SliceAxis.Y
            else -> SliceAxis.Z
        }
    }
}

private data class DVec2(
    val x: Double,
    val y: Double
) {
    operator fun minus(other: DVec2): DVec2 {
        return DVec2(x - other.x, y - other.y)
    }

    fun dot(other: DVec2): Double {
        return x * other.x + y * other.y
    }
}

private data class DVec3(
    val x: Double,
    val y: Double,
    val z: Double
) {
    operator fun plus(other: DVec3): DVec3 {
        return DVec3(x + other.x, y + other.y, z + other.z)
    }

    operator fun times(value: Double): DVec3 {
        return DVec3(x * value, y * value, z * value)
    }

    fun normalized(): DVec3 {
        val length = kotlin.math.sqrt(x * x + y * y + z * z)
        return DVec3(x / length, y / length, z / length)
    }
}

private fun rotateAroundAxis(point: DVec3, axis: SliceAxis, angleRadians: Double): DVec3 {
    return when (axis) {
        SliceAxis.X -> rotateX(point, angleRadians)
        SliceAxis.Y -> rotateY(point, angleRadians)
        SliceAxis.Z -> rotateZ(point, angleRadians)
    }
}

private fun rotateX(point: DVec3, angleRadians: Double): DVec3 {
    val c = kotlin.math.cos(angleRadians)
    val s = kotlin.math.sin(angleRadians)
    return DVec3(
        point.x,
        point.y * c - point.z * s,
        point.y * s + point.z * c
    )
}

private fun rotateY(point: DVec3, angleRadians: Double): DVec3 {
    val c = kotlin.math.cos(angleRadians)
    val s = kotlin.math.sin(angleRadians)
    return DVec3(
        point.x * c + point.z * s,
        point.y,
        -point.x * s + point.z * c
    )
}

private fun rotateZ(point: DVec3, angleRadians: Double): DVec3 {
    val c = kotlin.math.cos(angleRadians)
    val s = kotlin.math.sin(angleRadians)
    return DVec3(
        point.x * c - point.y * s,
        point.x * s + point.y * c,
        point.z
    )
}

private class KeyEdges(
    vararg keys: UInt
) {
    private val trackedKeys = keys.toList()
    private val previous = mutableMapOf<UInt, Boolean>()
    private val current = mutableMapOf<UInt, Boolean>()

    fun update(keyboard: KeyboardInputEventSubscriber) {
        trackedKeys.forEach { key ->
            previous[key] = current[key] ?: false
            current[key] = keyboard.isPressed(key)
        }
    }

    fun justPressed(key: UInt): Boolean {
        return current[key] == true && previous[key] != true
    }
}
