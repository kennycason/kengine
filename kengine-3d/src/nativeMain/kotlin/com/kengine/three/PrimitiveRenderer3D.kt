package com.kengine.three

import cnames.structs.SDL_GPUGraphicsPipeline
import com.kengine.graphics.Color
import com.kengine.math.Vec3
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_ReleaseGPUGraphicsPipeline

@OptIn(ExperimentalForeignApi::class)
class PrimitiveRenderer3D(
    private val gpu: GpuContext
) {
    private val pipeline: CPointer<SDL_GPUGraphicsPipeline>
    private var cleanedUp = false

    init {
        pipeline = gpu.createRendererPipeline3D(Kengine3DRendererPresets.PRIMITIVE)
    }

    fun triangle(
        frame: GpuFrame,
        center: Vec3,
        size: Float,
        color: Color,
        rotationRadians: Float = 0f
    ) {
        drawShape(
            frame = frame,
            center = center,
            width = size,
            height = size,
            color = color,
            rotationRadians = rotationRadians,
            shape = Shape.TRIANGLE
        )
    }

    fun quad(
        frame: GpuFrame,
        center: Vec3,
        width: Float,
        height: Float,
        color: Color,
        rotationRadians: Float = 0f
    ) {
        drawShape(
            frame = frame,
            center = center,
            width = width,
            height = height,
            color = color,
            rotationRadians = rotationRadians,
            shape = Shape.QUAD
        )
    }

    fun cleanup() {
        if (cleanedUp) {
            return
        }

        cleanedUp = true
        SDL_ReleaseGPUGraphicsPipeline(gpu.device, pipeline)
    }

    private fun drawShape(
        frame: GpuFrame,
        center: Vec3,
        width: Float,
        height: Float,
        color: Color,
        rotationRadians: Float,
        shape: Shape
    ) {
        check(!cleanedUp) {
            "PrimitiveRenderer3D has already been cleaned up."
        }

        val placement = project(frame, center, width, height)
        val uniforms = floatArrayOf(
            placement.x,
            placement.y,
            placement.depth,
            shape.uniformValue,
            placement.width,
            placement.height,
            rotationRadians,
            0f,
            color.r.toFloat() / 255f,
            color.g.toFloat() / 255f,
            color.b.toFloat() / 255f,
            color.a.toFloat() / 255f
        )

        frame.pushVertexUniformFloats3D(uniforms)
        frame.drawPrimitives3D(pipeline, vertexCount = shape.vertexCount)
    }

    private fun project(frame: GpuFrame, center: Vec3, width: Float, height: Float): ProjectedShape {
        val distance = (-center.z.toFloat()).coerceIn(NEAR_Z, FAR_Z)
        val aspect = frame.width.toFloat() / frame.height.toFloat()
        val clipX = (center.x.toFloat() / distance) / aspect
        val clipY = center.y.toFloat() / distance
        val clipDepth = ((distance - NEAR_Z) / (FAR_Z - NEAR_Z)).coerceIn(0f, 1f)
        val clipWidth = (width / distance) / aspect
        val clipHeight = height / distance

        return ProjectedShape(
            x = clipX,
            y = clipY,
            depth = clipDepth,
            width = clipWidth,
            height = clipHeight
        )
    }

    private data class ProjectedShape(
        val x: Float,
        val y: Float,
        val depth: Float,
        val width: Float,
        val height: Float
    )

    private enum class Shape(
        val uniformValue: Float,
        val vertexCount: UInt
    ) {
        TRIANGLE(0f, 3u),
        QUAD(1f, 6u)
    }

    companion object {
        private const val NEAR_Z = 0.35f
        private const val FAR_Z = 8f
    }
}
