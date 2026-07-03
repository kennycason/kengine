package com.kengine.three

import cnames.structs.SDL_GPUCommandBuffer
import cnames.structs.SDL_GPURenderPass
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
data class GpuFrame(
    val commandBuffer: CPointer<SDL_GPUCommandBuffer>,
    val renderPass: CPointer<SDL_GPURenderPass>,
    val width: UInt,
    val height: UInt
)
