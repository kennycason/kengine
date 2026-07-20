import com.kengine.three.importer.ModelImportAction3D
import com.kengine.three.importer.ModelImportPlan3D
import com.kengine.three.importer.ModelImportPlanner3D

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

    val plan = try {
        ModelImportPlanner3D.plan(inputPath, config.outputPath)
    } catch (error: IllegalArgumentException) {
        println(error.message)
        return
    }
    println(plan.toCliText())
}

private data class ImporterCliConfig(
    val inputPath: String? = null,
    val outputPath: String? = null,
    val printHelp: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): ImporterCliConfig {
            var config = ImporterCliConfig()
            var index = 0
            while (index < args.size) {
                when (val arg = args[index]) {
                    "--help", "-h" -> return config.copy(printHelp = true)
                    "--output", "-o" -> {
                        config = config.copy(outputPath = args.valueAfter(index, arg))
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
                  kengine-3d-importer <model-path> [--output <model.glb>]

                Runtime-ready formats:
                  glb, gltf, obj

                Conversion-planned formats:
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

private fun ModelImportPlan3D.toCliText(): String {
    val builder = StringBuilder()
    builder.appendLine("Kengine 3D import plan")
    builder.appendLine("  input=$inputPath")
    builder.appendLine("  format=${inputFormat?.label ?: "unknown"}")
    builder.appendLine("  action=${action.name.lowercase()}")
    outputPath?.let { builder.appendLine("  output=$it") }
    builder.appendLine("  message=$message")
    if (action == ModelImportAction3D.CONVERT_TO_GLB) {
        builder.appendLine("  next=wire a converter adapter, then validate the GLB with ModelLoader3D.inspect")
    }
    return builder.toString().trimEnd()
}
