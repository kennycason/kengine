package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUGraphicsPipeline
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalForeignApi::class)
class DebugRenderer3D(
    private val gpu: GpuContext
) {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private val lineMesh = GpuMesh.create(gpu, lineVertices())
    private val sphereMesh = GpuMesh.create(gpu, unitWireSphereVertices())
    private val boxMesh = GpuMesh.create(gpu, unitWireBoxVertices())
    private var cleanedUp = false

    init {
        pipeline = try {
            gpu.createRendererPipeline3D(Kengine3DRendererPresets.DEBUG)
        } catch (e: Throwable) {
            lineMesh.cleanup()
            sphereMesh.cleanup()
            boxMesh.cleanup()
            throw e
        }
    }

    fun line(
        frame: GpuFrame,
        camera: Camera3D,
        start: Vec3,
        end: Vec3,
        color: Color
    ) {
        draw(frame, lineMesh, lineMatrix(start, end), camera, color)
    }

    fun ray(
        frame: GpuFrame,
        camera: Camera3D,
        origin: Vec3,
        direction: Vec3,
        length: Double,
        color: Color
    ) {
        val unitDirection = normalize(direction)
        line(
            frame = frame,
            camera = camera,
            start = origin,
            end = add(origin, scale(unitDirection, length)),
            color = color
        )
    }

    fun wireSphere(
        frame: GpuFrame,
        camera: Camera3D,
        center: Vec3,
        radius: Double,
        color: Color
    ) {
        draw(
            frame = frame,
            mesh = sphereMesh,
            modelMatrix = Mat4.translation(center) * Mat4.scale(Vec3(radius, radius, radius)),
            camera = camera,
            color = color
        )
    }

    fun wireSphere(
        frame: GpuFrame,
        camera: Camera3D,
        sphere: SphereCollider3D,
        color: Color
    ) {
        wireSphere(
            frame = frame,
            camera = camera,
            center = sphere.center,
            radius = sphere.radius,
            color = color
        )
    }

    fun wireCapsule(
        frame: GpuFrame,
        camera: Camera3D,
        capsule: CapsuleCollider3D,
        color: Color
    ) {
        wireSphere(frame, camera, capsule.start, capsule.radius, color)
        wireSphere(frame, camera, capsule.end, capsule.radius, color)
        line(frame, camera, capsule.start, capsule.end, color)

        val axis = normalize(subtract(capsule.end, capsule.start))
        val reference = if (abs(axis.y) < 0.88) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val right = normalize(cross(reference, axis))
        val forward = normalize(cross(axis, right))
        listOf(right, scale(right, -1.0), forward, scale(forward, -1.0)).forEach { side ->
            val offset = scale(side, capsule.radius)
            line(
                frame = frame,
                camera = camera,
                start = add(capsule.start, offset),
                end = add(capsule.end, offset),
                color = color
            )
        }
    }

    fun wireAabb(
        frame: GpuFrame,
        camera: Camera3D,
        aabb: AabbCollider3D,
        color: Color
    ) {
        val size = Vec3(
            aabb.max.x - aabb.min.x,
            aabb.max.y - aabb.min.y,
            aabb.max.z - aabb.min.z
        )
        val center = Vec3(
            aabb.min.x + size.x * 0.5,
            aabb.min.y + size.y * 0.5,
            aabb.min.z + size.z * 0.5
        )
        draw(
            frame = frame,
            mesh = boxMesh,
            modelMatrix = Mat4.translation(center) * Mat4.scale(size),
            camera = camera,
            color = color
        )
    }

    fun contactPoint(
        frame: GpuFrame,
        camera: Camera3D,
        point: Vec3,
        normal: Vec3,
        color: Color,
        pointRadius: Double = 0.11,
        normalLength: Double = 0.85
    ) {
        wireSphere(
            frame = frame,
            camera = camera,
            center = point,
            radius = pointRadius,
            color = color
        )
        ray(
            frame = frame,
            camera = camera,
            origin = point,
            direction = normal,
            length = normalLength,
            color = color
        )
    }

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        lineMesh.cleanup()
        sphereMesh.cleanup()
        boxMesh.cleanup()
        SDL_ReleaseGPUGraphicsPipeline(gpu.device, pipeline)
    }

    private fun draw(
        frame: GpuFrame,
        mesh: GpuMesh,
        modelMatrix: Mat4,
        camera: Camera3D,
        color: Color
    ) {
        check(!cleanedUp) {
            "DebugRenderer3D has already been cleaned up."
        }

        val aspect = frame.width.toFloat() / frame.height.toFloat()
        frame.pushVertexUniformFloats3D(coloredModelViewProjectionUniforms3D(aspect, modelMatrix, camera, color))
        frame.drawPrimitives3D(
            pipeline = pipeline,
            vertexCount = mesh.vertexCount,
            vertexBuffer = GpuVertexBufferDrawBinding3D(mesh.vertexBuffer)
        )
    }

    companion object {
        private const val SEGMENTS = 32
        private val dummyColor = Color.fromHex("ffffff")

        private fun lineVertices(): List<Vertex3D> {
            return listOf(
                Vertex3D(Vec3(0.0, 0.0, 0.0), dummyColor),
                Vertex3D(Vec3(0.0, 1.0, 0.0), dummyColor)
            )
        }

        private fun unitWireSphereVertices(): List<Vertex3D> {
            val vertices = mutableListOf<Vertex3D>()

            fun addRing(point: (Double) -> Vec3) {
                for (index in 0 until SEGMENTS) {
                    val a = (index.toDouble() / SEGMENTS.toDouble()) * kotlin.math.PI * 2.0
                    val b = ((index + 1).toDouble() / SEGMENTS.toDouble()) * kotlin.math.PI * 2.0
                    vertices += Vertex3D(point(a), dummyColor)
                    vertices += Vertex3D(point(b), dummyColor)
                }
            }

            addRing { angle -> Vec3(cos(angle), sin(angle), 0.0) }
            addRing { angle -> Vec3(cos(angle), 0.0, sin(angle)) }
            addRing { angle -> Vec3(0.0, cos(angle), sin(angle)) }
            return vertices
        }

        private fun unitWireBoxVertices(): List<Vertex3D> {
            val corners = listOf(
                Vec3(-0.5, -0.5, -0.5),
                Vec3(0.5, -0.5, -0.5),
                Vec3(0.5, -0.5, 0.5),
                Vec3(-0.5, -0.5, 0.5),
                Vec3(-0.5, 0.5, -0.5),
                Vec3(0.5, 0.5, -0.5),
                Vec3(0.5, 0.5, 0.5),
                Vec3(-0.5, 0.5, 0.5)
            )
            val edges = listOf(
                0 to 1,
                1 to 2,
                2 to 3,
                3 to 0,
                4 to 5,
                5 to 6,
                6 to 7,
                7 to 4,
                0 to 4,
                1 to 5,
                2 to 6,
                3 to 7
            )
            return edges.flatMap { edge ->
                listOf(Vertex3D(corners[edge.first], dummyColor), Vertex3D(corners[edge.second], dummyColor))
            }
        }

        private fun lineMatrix(
            start: Vec3,
            end: Vec3
        ): Mat4 {
            val delta = subtract(end, start)
            return Mat4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    delta.x.toFloat(), delta.y.toFloat(), delta.z.toFloat(), 0f,
                    0f, 0f, 1f, 0f,
                    start.x.toFloat(), start.y.toFloat(), start.z.toFloat(), 1f
                )
            )
        }

        private fun add(
            a: Vec3,
            b: Vec3
        ): Vec3 {
            return Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
        }

        private fun subtract(
            a: Vec3,
            b: Vec3
        ): Vec3 {
            return Vec3(a.x - b.x, a.y - b.y, a.z - b.z)
        }

        private fun scale(
            value: Vec3,
            scale: Double
        ): Vec3 {
            return Vec3(value.x * scale, value.y * scale, value.z * scale)
        }

        private fun cross(
            a: Vec3,
            b: Vec3
        ): Vec3 {
            return Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
            )
        }

        private fun normalize(value: Vec3): Vec3 {
            val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
            if (length <= 0.0000001) {
                return Vec3(0.0, 1.0, 0.0)
            }
            return Vec3(value.x / length, value.y / length, value.z / length)
        }
    }
}
