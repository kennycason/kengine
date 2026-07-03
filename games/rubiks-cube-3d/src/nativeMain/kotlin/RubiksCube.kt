import com.kengine.graphics.Color
import com.kengine.math.Vec3
import com.kengine.three.CubeFaceColors
import com.kengine.three.GpuContext
import com.kengine.three.GpuFrame
import com.kengine.three.GpuMesh
import com.kengine.three.Mat4
import com.kengine.three.MeshRenderer3D
import com.kengine.three.PerspectiveCamera
import kotlin.math.PI
import kotlin.math.min
import kotlin.random.Random

private val HALF_PI = (PI / 2.0).toFloat()

enum class SliceAxis {
    X,
    Y,
    Z
}

data class SliceMove(
    val axis: SliceAxis,
    val layer: Int,
    val direction: Int
) {
    init {
        require(layer in -1..1) {
            "Rubik's cube layer must be -1, 0, or 1."
        }
        require(direction == -1 || direction == 1) {
            "Rubik's cube turn direction must be -1 or 1."
        }
    }
}

private data class GridPosition(
    val x: Int,
    val y: Int,
    val z: Int
)

private data class Cubie(
    var position: GridPosition,
    var orientation: Mat4,
    val mesh: GpuMesh
)

private data class ActiveSliceMove(
    val move: SliceMove,
    val durationSeconds: Float,
    var elapsedSeconds: Float = 0f
) {
    val progress: Float
        get() = min(elapsedSeconds / durationSeconds, 1f)

    val angleRadians: Float
        get() = easeOutCubic(progress) * move.direction * HALF_PI

    val isComplete: Boolean
        get() = progress >= 1f

    private fun easeOutCubic(value: Float): Float {
        val inverse = 1f - value
        return 1f - inverse * inverse * inverse
    }
}

class RubiksCube(
    private val gpu: GpuContext
) {
    private val cubies = mutableListOf<Cubie>()
    private val moveQueue = ArrayDeque<SliceMove>()
    private var activeMove: ActiveSliceMove? = null

    init {
        reset()
    }

    fun reset() {
        activeMove = null
        moveQueue.clear()
        cubies.forEach { it.mesh.cleanup() }
        cubies.clear()

        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    val position = GridPosition(x, y, z)
                    cubies += Cubie(
                        position = position,
                        orientation = Mat4.identity(),
                        mesh = GpuMesh.cube(gpu, colorsFor(position))
                    )
                }
            }
        }
    }

    fun enqueue(move: SliceMove) {
        moveQueue.addLast(move)
    }

    fun scramble(turns: Int = 22) {
        val axes = SliceAxis.entries
        repeat(turns) {
            moveQueue.addLast(
                SliceMove(
                    axis = axes[Random.nextInt(axes.size)],
                    layer = Random.nextInt(3) - 1,
                    direction = if (Random.nextBoolean()) 1 else -1
                )
            )
        }
    }

    fun update(deltaSeconds: Float) {
        if (activeMove == null && moveQueue.isNotEmpty()) {
            activeMove = ActiveSliceMove(
                move = moveQueue.removeFirst(),
                durationSeconds = if (moveQueue.size > 8) 0.11f else 0.22f
            )
        }

        val move = activeMove ?: return
        move.elapsedSeconds += deltaSeconds
        if (move.isComplete) {
            commit(move.move)
            activeMove = null
        }
    }

    fun draw(
        frame: GpuFrame,
        renderer: MeshRenderer3D,
        rootModel: Mat4,
        camera: PerspectiveCamera
    ) {
        val active = activeMove
        cubies.forEach { cubie ->
            val baseModel =
                Mat4.translation(
                    Vec3(
                        cubie.position.x * CUBIE_SPACING,
                        cubie.position.y * CUBIE_SPACING,
                        cubie.position.z * CUBIE_SPACING
                    )
                ) *
                    cubie.orientation *
                    Mat4.scale(Vec3(CUBIE_SIZE, CUBIE_SIZE, CUBIE_SIZE))

            val model = if (active != null && active.move.includes(cubie.position)) {
                rootModel * rotationMatrix(active.move.axis, active.angleRadians) * baseModel
            } else {
                rootModel * baseModel
            }

            renderer.draw(frame, cubie.mesh, model, camera)
        }
    }

    fun cleanup() {
        activeMove = null
        moveQueue.clear()
        cubies.forEach { it.mesh.cleanup() }
        cubies.clear()
    }

    private fun commit(move: SliceMove) {
        val finalRotation = rotationMatrix(move.axis, move.direction * HALF_PI)
        cubies.forEach { cubie ->
            if (move.includes(cubie.position)) {
                cubie.position = rotate(cubie.position, move.axis, move.direction)
                cubie.orientation = finalRotation * cubie.orientation
            }
        }
    }

    private fun SliceMove.includes(position: GridPosition): Boolean {
        return when (axis) {
            SliceAxis.X -> position.x == layer
            SliceAxis.Y -> position.y == layer
            SliceAxis.Z -> position.z == layer
        }
    }

    private fun rotate(position: GridPosition, axis: SliceAxis, direction: Int): GridPosition {
        return when (axis) {
            SliceAxis.X -> if (direction > 0) {
                GridPosition(position.x, -position.z, position.y)
            } else {
                GridPosition(position.x, position.z, -position.y)
            }
            SliceAxis.Y -> if (direction > 0) {
                GridPosition(position.z, position.y, -position.x)
            } else {
                GridPosition(-position.z, position.y, position.x)
            }
            SliceAxis.Z -> if (direction > 0) {
                GridPosition(-position.y, position.x, position.z)
            } else {
                GridPosition(position.y, -position.x, position.z)
            }
        }
    }

    private fun rotationMatrix(axis: SliceAxis, angleRadians: Float): Mat4 {
        return when (axis) {
            SliceAxis.X -> Mat4.rotationX(angleRadians)
            SliceAxis.Y -> Mat4.rotationY(angleRadians)
            SliceAxis.Z -> Mat4.rotationZ(angleRadians)
        }
    }

    private fun colorsFor(position: GridPosition): CubeFaceColors {
        return CubeFaceColors(
            negativeZ = if (position.z == -1) BACK else HIDDEN,
            positiveZ = if (position.z == 1) FRONT else HIDDEN,
            negativeX = if (position.x == -1) LEFT else HIDDEN,
            positiveX = if (position.x == 1) RIGHT else HIDDEN,
            positiveY = if (position.y == 1) UP else HIDDEN,
            negativeY = if (position.y == -1) DOWN else HIDDEN
        )
    }

    companion object {
        private const val CUBIE_SPACING = 0.96
        private const val CUBIE_SIZE = 0.88

        private val HIDDEN = Color.fromHex("111318")
        private val FRONT = Color.fromHex("2ebf6d")
        private val BACK = Color.fromHex("2459d6")
        private val RIGHT = Color.fromHex("d62839")
        private val LEFT = Color.fromHex("f28c28")
        private val UP = Color.fromHex("f4f0df")
        private val DOWN = Color.fromHex("f5d547")
    }
}
