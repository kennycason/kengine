import com.kengine.createGameContext
import com.kengine.graphics.Color
import com.kengine.input.keyboard.KeyboardInputEventSubscriber
import com.kengine.input.keyboard.Keys
import com.kengine.input.mouse.MouseInputEventSubscriber
import com.kengine.log.Logger
import com.kengine.math.Vec3
import com.kengine.sdl.RenderBackend
import com.kengine.three.GpuContext
import com.kengine.three.GpuFrame
import com.kengine.three.GpuMesh
import com.kengine.three.Mat4
import com.kengine.three.MeshRenderer3D
import com.kengine.three.PerspectiveCamera
import com.kengine.three.PrimitiveRenderer3D
import com.kengine.three.Vertex3D
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
            val highlight = StickerHighlight(this)
            val apiServer = RubiksCubeApiServer()
            apiServer.start()
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

                    apiServer.drain(rubiksCube)
                    pointer.update(mouseInput, canTurn = rubiksCube.isIdle)?.let { rubiksCube.enqueue(it) }
                    keys.update(keyboardInput)
                    handleKeys(keys, keyboardInput, rubiksCube)
                    rubiksCube.update(deltaSeconds.coerceAtMost(0.05f))

                    val hovered = pointer.hover(mouseInput, canPick = rubiksCube.isIdle)

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
                        hovered?.let { highlight.draw(frame, meshes, root, camera, it, rubiksCube) }
                    }

                    mouseInput.clearFrameState()
                    SDL_Delay(16u)
                }
            } finally {
                apiServer.stop()
                rubiksCube.cleanup()
                highlight.cleanup()
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

private val logger = Logger("RubiksCube3D")

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
    private var lockedHit: CubieFaceHit? = null
    private var lastX = 0.0
    private var lastY = 0.0
    private var startX = 0.0
    private var startY = 0.0
    // Edge-detect right/middle press since only left has wasJustPressed in the API
    private var prevRightPressed = false
    private var prevMiddlePressed = false

    fun update(mouse: MouseInputEventSubscriber, canTurn: Boolean): SliceMove? {
        val cursor = mouse.cursor()
        val rightJustPressed = mouse.isRightPressed() && !prevRightPressed
        val middleJustPressed = mouse.isMiddlePressed() && !prevMiddlePressed
        prevRightPressed = mouse.isRightPressed()
        prevMiddlePressed = mouse.isMiddlePressed()

        if (!canTurn && mode == PointerMode.TURN) {
            pickedFace = null
            lockedHit = null
            mode = PointerMode.NONE
        }

        // Only start a new interaction when idle
        if (mode == PointerMode.NONE) {
            when {
                // Right/middle click: orbit (rotate camera)
                rightJustPressed || middleJustPressed -> {
                    lastX = cursor.x
                    lastY = cursor.y
                    mode = PointerMode.ORBIT
                    logger.info { "Orbit start" }
                }
                // Left click: pick a sticker and prepare to turn a layer
                canTurn && mouse.wasLeftJustPressed() -> {
                    val rawHit = pickHit(cursor.x, cursor.y)
                    if (rawHit != null) {
                        val face = PickedFace(
                            hitFaceAxis = rawHit.faceAxis,
                            hitFaceSign = rawHit.faceSign,
                            hitPoint = rawHit.point,
                            layerX = rawHit.gridX,
                            layerY = rawHit.gridY,
                            layerZ = rawHit.gridZ,
                            pointer = this
                        )
                        startX = cursor.x
                        startY = cursor.y
                        lastX = cursor.x
                        lastY = cursor.y
                        pickedFace = face
                        lockedHit = rawHit
                        mode = PointerMode.TURN
                        logger.info {
                            "Pick: face=${face.hitFaceAxis} " +
                            "hit=(${fmtD(face.hitPoint.x)},${fmtD(face.hitPoint.y)},${fmtD(face.hitPoint.z)}) " +
                            "layers=(X${face.layerX},Y${face.layerY},Z${face.layerZ})"
                        }
                    } else {
                        logger.info { "Left click: no face hit at (${cursor.x.toInt()},${cursor.y.toInt()})" }
                    }
                }
            }
        }

        return when (mode) {
            PointerMode.NONE -> null

            PointerMode.ORBIT -> {
                if (!mouse.isRightPressed() && !mouse.isMiddlePressed()) {
                    mode = PointerMode.NONE
                    null
                } else {
                    val dx = cursor.x - lastX
                    val dy = cursor.y - lastY
                    yawRadians += (dx * 0.0085).toFloat()
                    pitchRadians = (pitchRadians + (dy * 0.0085).toFloat()).coerceIn(-MAX_PITCH, MAX_PITCH)
                    lastX = cursor.x
                    lastY = cursor.y
                    null
                }
            }

            PointerMode.TURN -> {
                if (!mouse.isLeftPressed()) {
                    mode = PointerMode.NONE
                    pickedFace = null
                    lockedHit = null
                    null
                } else {
                    val dx = cursor.x - startX
                    val dy = cursor.y - startY
                    if (dx * dx + dy * dy < TURN_DRAG_THRESHOLD * TURN_DRAG_THRESHOLD) {
                        null
                    } else {
                        mode = PointerMode.WAIT_RELEASE
                        lockedHit = null
                        logger.info { "Drag: (${fmtD(dx)}, ${fmtD(dy)})" }
                        pickedFace?.toMove(dx, dy)
                    }
                }
            }

            PointerMode.WAIT_RELEASE -> {
                if (!mouse.isLeftPressed()) {
                    mode = PointerMode.NONE
                    pickedFace = null
                    lockedHit = null
                }
                null
            }
        }
    }

    fun hover(mouse: MouseInputEventSubscriber, canPick: Boolean): CubieFaceHit? {
        if (!canPick) {
            return null
        }

        return when (mode) {
            PointerMode.ORBIT -> null
            PointerMode.TURN, PointerMode.WAIT_RELEASE -> lockedHit
            PointerMode.NONE -> {
                val cursor = mouse.cursor()
                pickHit(cursor.x, cursor.y)
            }
        }
    }

    private fun fmtD(v: Double) = (v * 10).toInt().let { if (it >= 0) "+${it}" else "$it" }.padStart(5)

    private fun pickHit(mouseX: Double, mouseY: Double): CubieFaceHit? {
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

        return intersectCubies(localOrigin, localRay)
    }

    // Tests all 26 outer cubies individually and returns the nearest hit with an exact face axis.
    // This is far more accurate than testing the whole-cube AABB, which can't distinguish which
    // sticker on a corner piece (e.g. red vs green on the top-right-front corner) was clicked.
    private fun intersectCubies(origin: DVec3, direction: DVec3): CubieFaceHit? {
        var nearestT = Double.POSITIVE_INFINITY
        var result: CubieFaceHit? = null

        for (gx in -1..1) {
            for (gy in -1..1) {
                for (gz in -1..1) {
                    if (gx == 0 && gy == 0 && gz == 0) continue

                    val cx = gx * CUBIE_SPACING
                    val cy = gy * CUBIE_SPACING
                    val cz = gz * CUBIE_SPACING

                    val hit = intersectAABB(
                        origin, direction,
                        cx - CUBIE_HALF, cy - CUBIE_HALF, cz - CUBIE_HALF,
                        cx + CUBIE_HALF, cy + CUBIE_HALF, cz + CUBIE_HALF
                    ) ?: continue

                    if (hit.t < nearestT) {
                        nearestT = hit.t

                        result = CubieFaceHit(
                            point = origin + direction * hit.t,
                            faceAxis = hit.faceAxis,
                            faceSign = hit.faceSign,
                            gridX = gx,
                            gridY = gy,
                            gridZ = gz
                        ).takeIf { it.isOuterSticker() }
                    }
                }
            }
        }

        return result
    }

    private fun intersectAABB(
        origin: DVec3, direction: DVec3,
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double
    ): AabbHit? {
        var tMin = Double.NEGATIVE_INFINITY
        var tMax = Double.POSITIVE_INFINITY
        var entryAxis: SliceAxis? = null
        var entrySign = 1
        var exitAxis: SliceAxis? = null
        var exitSign = 1

        fun testSlab(axis: SliceAxis, start: Double, dir: Double, min: Double, max: Double): Boolean {
            if (abs(dir) < 0.000001) return start in min..max
            val t1 = (min - start) / dir
            val t2 = (max - start) / dir
            val near = minOf(t1, t2)
            val far = maxOf(t1, t2)

            if (near > tMin) {
                tMin = near
                entryAxis = axis
                entrySign = if (t1 <= t2) -1 else 1
            }
            if (far < tMax) {
                tMax = far
                exitAxis = axis
                exitSign = if (t1 <= t2) 1 else -1
            }
            return tMin <= tMax
        }

        if (!testSlab(SliceAxis.X, origin.x, direction.x, minX, maxX)) return null
        if (!testSlab(SliceAxis.Y, origin.y, direction.y, minY, maxY)) return null
        if (!testSlab(SliceAxis.Z, origin.z, direction.z, minZ, maxZ)) return null
        if (tMax < 0.0) return null

        return if (tMin >= 0.0) {
            AabbHit(tMin, entryAxis ?: return null, entrySign)
        } else {
            AabbHit(tMax, exitAxis ?: return null, exitSign)
        }
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
        private const val CUBIE_SPACING = 0.96   // matches RubiksCube.CUBIE_SPACING
        private const val CUBIE_HALF = 0.44       // CUBIE_SIZE / 2 = 0.88 / 2
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
    val hitFaceAxis: SliceAxis,
    val hitFaceSign: Int,
    val hitPoint: DVec3,
    val layerX: Int,
    val layerY: Int,
    val layerZ: Int,
    val pointer: CubePointerControls
) {
    fun toMove(dx: Double, dy: Double): SliceMove {
        // Normalize drag so alignment is a true cosine (0–1) for logging and comparison.
        val drag = DVec2(dx, dy).normalized()
        val screenBase = pointer.project(hitPoint)

        val tangent = bestStickerPlaneDrag(drag, screenBase)
        val normal = axisVector(hitFaceAxis, hitFaceSign)
        val bestAxis = dominantAxis(normal.cross(tangent.vector))
        val bestLayer = when (bestAxis) {
            SliceAxis.X -> layerX
            SliceAxis.Y -> layerY
            SliceAxis.Z -> layerZ
        }
        val positiveMotion = rotateAroundAxis(hitPoint, bestAxis, 0.16) - hitPoint
        val direction = if (positiveMotion.dot(tangent.vector) >= 0.0) 1 else -1
        val move = SliceMove(bestAxis, bestLayer, direction)
        logger.info {
            "Turn: drag=(${dx.toInt()},${dy.toInt()}) face=$hitFaceAxis$hitFaceSign -> $bestAxis layer=$bestLayer dir=$direction tangent=${tangent.axis}${tangent.sign} (alignment=${(tangent.alignment * 100).toInt()}%)"
        }
        return move
    }

    private fun bestStickerPlaneDrag(drag: DVec2, screenBase: DVec2): SignedAxis {
        var best = SignedAxis(axis = SliceAxis.X, sign = 1, alignment = Double.NEGATIVE_INFINITY)

        for (axis in SliceAxis.entries) {
            if (axis == hitFaceAxis) continue

            for (sign in listOf(-1, 1)) {
                val vector = axisVector(axis, sign)
                val screenDirection = (pointer.project(hitPoint + vector * 0.16) - screenBase).normalized()
                val alignment = screenDirection.dot(drag)
                if (alignment > best.alignment) {
                    best = SignedAxis(axis = axis, sign = sign, alignment = alignment)
                }
            }
        }

        return best
    }
}

private data class SignedAxis(
    val axis: SliceAxis,
    val sign: Int,
    val alignment: Double
) {
    val vector: DVec3
        get() = axisVector(axis, sign)
}

private data class CubieFaceHit(
    val point: DVec3,
    val faceAxis: SliceAxis,
    val faceSign: Int,   // +1 = positive face (X+/Y+/Z+), -1 = negative face
    val gridX: Int,
    val gridY: Int,
    val gridZ: Int
) {
    fun isOuterSticker(): Boolean {
        return when (faceAxis) {
            SliceAxis.X -> (gridX == 1 && faceSign == 1) || (gridX == -1 && faceSign == -1)
            SliceAxis.Y -> (gridY == 1 && faceSign == 1) || (gridY == -1 && faceSign == -1)
            SliceAxis.Z -> (gridZ == 1 && faceSign == 1) || (gridZ == -1 && faceSign == -1)
        }
    }
}

private data class AabbHit(
    val t: Double,
    val faceAxis: SliceAxis,
    val faceSign: Int
)

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

    fun normalized(): DVec2 {
        val length = kotlin.math.sqrt(x * x + y * y)
        return if (length < 0.000001) DVec2(1.0, 0.0) else DVec2(x / length, y / length)
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

    operator fun minus(other: DVec3): DVec3 {
        return DVec3(x - other.x, y - other.y, z - other.z)
    }

    operator fun times(value: Double): DVec3 {
        return DVec3(x * value, y * value, z * value)
    }

    fun dot(other: DVec3): Double {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: DVec3): DVec3 {
        return DVec3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    fun normalized(): DVec3 {
        val length = kotlin.math.sqrt(x * x + y * y + z * z)
        return DVec3(x / length, y / length, z / length)
    }
}

private fun axisVector(axis: SliceAxis, sign: Int): DVec3 {
    val signed = sign.toDouble()
    return when (axis) {
        SliceAxis.X -> DVec3(signed, 0.0, 0.0)
        SliceAxis.Y -> DVec3(0.0, signed, 0.0)
        SliceAxis.Z -> DVec3(0.0, 0.0, signed)
    }
}

private fun dominantAxis(vector: DVec3): SliceAxis {
    val ax = abs(vector.x)
    val ay = abs(vector.y)
    val az = abs(vector.z)
    return when {
        ax >= ay && ax >= az -> SliceAxis.X
        ay >= ax && ay >= az -> SliceAxis.Y
        else -> SliceAxis.Z
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

// Renders a hover highlight on the sticker face under the cursor:
// a white outline border and a lightened-color fill, both drawn as flat quads
// slightly in front of the cubie face surface.
private class StickerHighlight(private val gpu: GpuContext) {
    private val borderMesh: GpuMesh = flatQuad(BORDER_COLOR)
    private val fillMeshByColor: MutableMap<Color, GpuMesh> = mutableMapOf()

    fun draw(frame: GpuFrame, renderer: MeshRenderer3D, rootModel: Mat4, camera: PerspectiveCamera, hit: CubieFaceHit, rubiksCube: RubiksCube) {
        val cx = hit.gridX * CUBIE_SPACING
        val cy = hit.gridY * CUBIE_SPACING
        val cz = hit.gridZ * CUBIE_SPACING
        val orient = faceOrientation(hit.faceAxis, hit.faceSign)
        val cubieTx = Mat4.translation(Vec3(cx, cy, cz))

        // Border: slightly larger, pushed a tiny bit outward
        val borderModel = rootModel * cubieTx * orient *
            Mat4.translation(Vec3(0.0, 0.0, CUBIE_HALF + 0.003)) *
            Mat4.scale(Vec3(CUBIE_SIZE + 0.04, CUBIE_SIZE + 0.04, 1.0))
        renderer.draw(frame, borderMesh, borderModel, camera)

        // Fill: look up the actual current sticker color and use its darkened variant.
        val currentColor = rubiksCube.getStickerColor(hit.gridX, hit.gridY, hit.gridZ, hit.faceAxis, hit.faceSign)
        val fillMesh = fillMeshFor(currentColor ?: return)
        val fillModel = rootModel * cubieTx * orient *
            Mat4.translation(Vec3(0.0, 0.0, CUBIE_HALF + 0.005)) *
            Mat4.scale(Vec3(CUBIE_SIZE - 0.04, CUBIE_SIZE - 0.04, 1.0))
        renderer.draw(frame, fillMesh, fillModel, camera)
    }

    fun cleanup() {
        borderMesh.cleanup()
        fillMeshByColor.values.forEach { it.cleanup() }
    }

    // Flat quad in the XY plane at Z=0, vertices at ±0.5 — scaled and pushed by the caller.
    private fun flatQuad(color: Color): GpuMesh = GpuMesh.create(gpu, listOf(
        Vertex3D(Vec3(-0.5, -0.5, 0.0), color),
        Vertex3D(Vec3(-0.5,  0.5, 0.0), color),
        Vertex3D(Vec3( 0.5,  0.5, 0.0), color),
        Vertex3D(Vec3(-0.5, -0.5, 0.0), color),
        Vertex3D(Vec3( 0.5,  0.5, 0.0), color),
        Vertex3D(Vec3( 0.5, -0.5, 0.0), color),
    ))

    private fun fillMeshFor(color: Color): GpuMesh {
        return fillMeshByColor.getOrPut(color) { flatQuad(darken(color)) }
    }

    // Rotation that aligns the flat quad (facing +Z by default) to the given cubie face.
    private fun faceOrientation(axis: SliceAxis, sign: Int): Mat4 = when (axis) {
        SliceAxis.X -> if (sign > 0) Mat4.rotationY((PI / 2).toFloat())  else Mat4.rotationY((-PI / 2).toFloat())
        SliceAxis.Y -> if (sign > 0) Mat4.rotationX((-PI / 2).toFloat()) else Mat4.rotationX((PI / 2).toFloat())
        SliceAxis.Z -> if (sign > 0) Mat4.identity()                     else Mat4.rotationY(PI.toFloat())
    }

    private fun darken(color: Color): Color = Color(
        r = (color.r.toInt() * 7 / 10).toUByte(),
        g = (color.g.toInt() * 7 / 10).toUByte(),
        b = (color.b.toInt() * 7 / 10).toUByte(),
        a = 255u
    )

    companion object {
        private const val CUBIE_SPACING = 0.96
        private const val CUBIE_HALF = 0.44
        private const val CUBIE_SIZE = 0.88
        private val BORDER_COLOR = Color.fromHex("ffffff")
    }
}
