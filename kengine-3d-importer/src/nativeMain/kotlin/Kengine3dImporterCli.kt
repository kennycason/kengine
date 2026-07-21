package com.kengine.three.importer.cli

import com.kengine.three.ModelInfo3D
import com.kengine.three.importer.ModelAssetPreflight3D
import com.kengine.three.importer.ModelAssetPreflightResult3D
import com.kengine.three.importer.ModelAssetPreflightStatus3D

fun main(args: Array<String>) {
    val config = try {
        ImporterCliConfig.parse(args)
    } catch (error: IllegalArgumentException) {
        println(error.message)
        println()
        ImporterCliConfig.printUsage()
        return
    }

    if (config.printHelp) {
        ImporterCliConfig.printUsage()
        return
    }

    val inputPath = config.inputPath
    if (inputPath == null) {
        ImporterCliConfig.printUsage()
        return
    }

    val result = try {
        ModelAssetPreflight3D.inspect(inputPath, config.suggestedRuntimePath)
    } catch (error: IllegalArgumentException) {
        println(error.message)
        return
    }
    println(result.toCliText())
}

private data class ImporterCliConfig(
    val inputPath: String? = null,
    val suggestedRuntimePath: String? = null,
    val printHelp: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): ImporterCliConfig {
            var config = ImporterCliConfig()
            var index = 0
            while (index < args.size) {
                when (val arg = args[index]) {
                    "--help", "-h" -> return config.copy(printHelp = true)
                    "--suggested-output", "--output", "-o" -> {
                        config = config.copy(suggestedRuntimePath = args.valueAfter(index, arg))
                        index += 1
                    }
                    else -> {
                        require(!arg.startsWith("-")) {
                            "Unknown argument: $arg"
                        }
                        require(config.inputPath == null) {
                            "Only one input model path can be planned at a time."
                        }
                        config = config.copy(inputPath = arg)
                    }
                }
                index += 1
            }
            return config
        }

        fun printUsage() {
            println(
                """
                Kengine 3D Importer

                Usage:
                  kengine-3d-importer <model-path> [--suggested-output <model.glb>]

                Runtime-ready formats:
                  glb, gltf, obj

                Source formats requiring external GLB export:
                  fbx, usdz, usd, usda, usdc
                """.trimIndent()
            )
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

private fun ModelAssetPreflightResult3D.toCliText(): String {
    val importPlan = plan
    val builder = StringBuilder()
    builder.appendLine("Kengine 3D asset preflight")
    builder.appendLine("  input=${importPlan.inputPath}")
    builder.appendLine("  format=${importPlan.inputFormat?.label ?: "unknown"}")
    builder.appendLine("  action=${importPlan.action.name.lowercase()}")
    builder.appendLine("  status=${status.name.lowercase()}")
    importPlan.suggestedRuntimePath?.let { builder.appendLine("  suggestedRuntime=$it") }
    builder.appendLine("  message=$message")
    modelInfo?.let { builder.appendModelInfo(it) }
    if (status == ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED) {
        builder.appendLine("  next=export this source model to GLB from your asset tool, then run preflight on the GLB")
    }
    return builder.toString().trimEnd()
}

private fun StringBuilder.appendModelInfo(info: ModelInfo3D) {
    appendLine("  meshes=${info.meshCount} primitives=${info.primitiveCount} vertices=${info.vertexCount}")
    appendLine("  materials=${info.materialCount} textures=${info.textureCount} images=${info.imageCount}")
    appendLine("  animations=${info.animationCount} skins=${info.skinCount}")
    val slots = info.textureSlotUsage
    if (slots.totalSlotCount > 0) {
        appendLine(
            "  textureSlots=base:${slots.baseColor} normal:${slots.normal} " +
                "metadata:${(slots.secondarySlotCount - slots.normal).coerceAtLeast(0)}"
        )
    }
}
