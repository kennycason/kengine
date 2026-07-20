package com.kengine.three

import cnames.structs.SDL_GPUShader
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import sdl3.SDL_CreateGPUShader
import sdl3.SDL_GPUShaderCreateInfo
import sdl3.SDL_GPUShaderFormat
import sdl3.SDL_GPUShaderStage
import sdl3.SDL_GPU_SHADERFORMAT_DXIL
import sdl3.SDL_GPU_SHADERFORMAT_METALLIB
import sdl3.SDL_GPU_SHADERFORMAT_MSL
import sdl3.SDL_GPU_SHADERFORMAT_SPIRV
import sdl3.SDL_GetError
import sdl3.SDL_ReleaseGPUShader

enum class GpuShaderFormat3D(
    val displayName: String,
    internal val sdlFormat: SDL_GPUShaderFormat
) {
    METALLIB("Metal library", SDL_GPU_SHADERFORMAT_METALLIB),
    MSL("MSL", SDL_GPU_SHADERFORMAT_MSL),
    SPIRV("SPIR-V", SDL_GPU_SHADERFORMAT_SPIRV),
    DXIL("DXIL", SDL_GPU_SHADERFORMAT_DXIL);

    internal fun isIn(mask: SDL_GPUShaderFormat): Boolean {
        return (mask and sdlFormat) != 0u
    }

    companion object {
        internal val defaultRequestMask: SDL_GPUShaderFormat =
            SPIRV.sdlFormat or METALLIB.sdlFormat or MSL.sdlFormat or DXIL.sdlFormat

        internal fun fromMask(mask: SDL_GPUShaderFormat): List<GpuShaderFormat3D> {
            return entries.filter { it.isIn(mask) }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal data class GpuShaderProgram3D(
    val vertexShader: CPointer<SDL_GPUShader>,
    val fragmentShader: CPointer<SDL_GPUShader>
)

internal data class GpuShaderArtifact3D(
    val format: GpuShaderFormat3D,
    val entrypoint: String,
    val code: ByteArray
) {
    companion object {
        fun mslSource(
            source: String,
            entrypoint: String = DEFAULT_SHADER_ENTRYPOINT
        ): GpuShaderArtifact3D {
            return GpuShaderArtifact3D(
                format = GpuShaderFormat3D.MSL,
                entrypoint = entrypoint,
                code = source.encodeToByteArray()
            )
        }

        fun metallib(
            code: ByteArray,
            entrypoint: String = DEFAULT_SHADER_ENTRYPOINT
        ): GpuShaderArtifact3D {
            return GpuShaderArtifact3D(
                format = GpuShaderFormat3D.METALLIB,
                entrypoint = entrypoint,
                code = code
            )
        }

        fun spirv(
            code: ByteArray,
            entrypoint: String = DEFAULT_SHADER_ENTRYPOINT
        ): GpuShaderArtifact3D {
            return GpuShaderArtifact3D(
                format = GpuShaderFormat3D.SPIRV,
                entrypoint = entrypoint,
                code = code
            )
        }

        fun dxil(
            code: ByteArray,
            entrypoint: String = DEFAULT_SHADER_ENTRYPOINT
        ): GpuShaderArtifact3D {
            return GpuShaderArtifact3D(
                format = GpuShaderFormat3D.DXIL,
                entrypoint = entrypoint,
                code = code
            )
        }
    }
}

internal data class GpuShaderStageSource3D(
    val artifacts: List<GpuShaderArtifact3D>,
    val uniformBuffers: UInt,
    val samplers: UInt = 0u,
    val storageTextures: UInt = 0u,
    val storageBuffers: UInt = 0u
) {
    init {
        require(artifacts.isNotEmpty()) {
            "GpuShaderStageSource3D requires at least one shader artifact."
        }
    }
}

internal data class GpuShaderProgramSource3D(
    val label: String,
    val vertex: GpuShaderStageSource3D,
    val fragment: GpuShaderStageSource3D
)

@OptIn(ExperimentalForeignApi::class)
internal inline fun <T> GpuContext.withShaderProgram3D(
    source: GpuShaderProgramSource3D,
    block: (GpuShaderProgram3D) -> T
): T {
    var vertexShader: CPointer<SDL_GPUShader>? = null
    var fragmentShader: CPointer<SDL_GPUShader>? = null

    try {
        vertexShader = createBestShader3D(
            label = source.label,
            stage = SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX,
            source = source.vertex
        )
        fragmentShader = createBestShader3D(
            label = source.label,
            stage = SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_FRAGMENT,
            source = source.fragment
        )
        return block(GpuShaderProgram3D(vertexShader, fragmentShader))
    } finally {
        fragmentShader?.let { SDL_ReleaseGPUShader(device, it) }
        vertexShader?.let { SDL_ReleaseGPUShader(device, it) }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.createBestShader3D(
    label: String,
    stage: SDL_GPUShaderStage,
    source: GpuShaderStageSource3D
): CPointer<SDL_GPUShader> {
    val stageName = stage.shaderStageName()
    val candidates = source.artifactsFor(supportedShaderFormats)
    if (candidates.isEmpty()) {
        throw IllegalStateException(
            "No supported shader artifact for $label $stageName shader. " +
                "SDL GPU driver '$deviceDriver' supports ${supportedShaderFormats.displayNames()}, " +
                "but this shader provides ${source.artifacts.map { it.format }.displayNames()}."
        )
    }

    val failures = mutableListOf<String>()
    candidates.forEach { artifact ->
        try {
            return createShader3D(
                label = label,
                artifact = artifact,
                stage = stage,
                source = source
            )
        } catch (e: IllegalStateException) {
            failures += "${artifact.format.displayName}: ${e.message}"
        }
    }

    throw IllegalStateException(
        "Error creating $label $stageName shader with supported artifacts " +
            "${candidates.map { it.format }.displayNames()}: ${failures.joinToString("; ")}"
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun GpuContext.createShader3D(
    label: String,
    artifact: GpuShaderArtifact3D,
    stage: SDL_GPUShaderStage,
    source: GpuShaderStageSource3D
): CPointer<SDL_GPUShader> {
    return memScoped {
        artifact.code.usePinned { pinned ->
            val createInfo = alloc<SDL_GPUShaderCreateInfo>()
            createInfo.code_size = artifact.code.size.convert()
            createInfo.code = pinned.addressOf(0).reinterpret<UByteVar>()
            createInfo.entrypoint = artifact.entrypoint.cstr.ptr
            createInfo.format = artifact.format.sdlFormat
            createInfo.stage = stage
            createInfo.num_samplers = source.samplers
            createInfo.num_storage_textures = source.storageTextures
            createInfo.num_storage_buffers = source.storageBuffers
            createInfo.num_uniform_buffers = source.uniformBuffers
            createInfo.props = 0u

            SDL_CreateGPUShader(device, createInfo.ptr)
                ?: throw IllegalStateException(
                    "Error creating $label ${stage.shaderStageName()} shader " +
                        "(${artifact.format.displayName}, entrypoint ${artifact.entrypoint}): ${sdlErrorMessage3D()}"
                )
        }
    }
}

internal fun sdlErrorMessage3D(): String {
    return SDL_GetError()?.toKString()?.takeIf { it.isNotBlank() } ?: "unknown SDL error"
}

private fun GpuShaderStageSource3D.artifactsFor(
    supportedFormats: List<GpuShaderFormat3D>
): List<GpuShaderArtifact3D> {
    return supportedFormats.mapNotNull { format ->
        artifacts.firstOrNull { it.format == format }
    }
}

private fun Iterable<GpuShaderFormat3D>.displayNames(): String {
    return joinToString(", ") { it.displayName }.ifBlank { "none" }
}

private const val DEFAULT_SHADER_ENTRYPOINT = "main0"

@OptIn(ExperimentalForeignApi::class)
private fun SDL_GPUShaderStage.shaderStageName(): String {
    return when (this) {
        SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_VERTEX -> "vertex"
        SDL_GPUShaderStage.SDL_GPU_SHADERSTAGE_FRAGMENT -> "fragment"
        else -> "unknown-stage"
    }
}
