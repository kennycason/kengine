package com.kengine.three.importer

enum class ModelImportAction3D {
    LOAD_DIRECTLY,
    CONVERT_TO_GLB,
    UNSUPPORTED
}

enum class ModelImportFormat3D(
    val label: String,
    val extensions: Set<String>,
    val runtimeReady: Boolean
) {
    GLB("GLB", setOf("glb"), runtimeReady = true),
    GLTF("GLTF", setOf("gltf"), runtimeReady = true),
    OBJ("OBJ", setOf("obj"), runtimeReady = true),
    FBX("FBX", setOf("fbx"), runtimeReady = false),
    USDZ("USDZ", setOf("usdz"), runtimeReady = false),
    USD("USD", setOf("usd"), runtimeReady = false),
    USDA("USDA", setOf("usda"), runtimeReady = false),
    USDC("USDC", setOf("usdc"), runtimeReady = false);

    companion object {
        fun detect(path: String): ModelImportFormat3D? {
            val extension = path.fileName()
                .substringAfterLast('.', missingDelimiterValue = "")
                .lowercase()
            if (extension.isBlank()) {
                return null
            }
            return values().firstOrNull { extension in it.extensions }
        }

        fun runtimeExtensions(): List<String> {
            return values()
                .filter { it.runtimeReady }
                .flatMap { it.extensions }
                .sorted()
        }

        fun conversionCandidateExtensions(): List<String> {
            return values()
                .filterNot { it.runtimeReady }
                .flatMap { it.extensions }
                .sorted()
        }
    }
}

data class ModelImportPlan3D(
    val inputPath: String,
    val inputFormat: ModelImportFormat3D?,
    val action: ModelImportAction3D,
    val outputPath: String?,
    val message: String
) {
    val runtimeReady: Boolean
        get() = action == ModelImportAction3D.LOAD_DIRECTLY

    val requiresConversion: Boolean
        get() = action == ModelImportAction3D.CONVERT_TO_GLB
}

object ModelImportPlanner3D {
    fun plan(
        inputPath: String,
        outputPath: String? = null
    ): ModelImportPlan3D {
        require(inputPath.isNotBlank()) {
            "Model import input path must not be blank."
        }

        val format = ModelImportFormat3D.detect(inputPath)
        if (format == null) {
            return ModelImportPlan3D(
                inputPath = inputPath,
                inputFormat = null,
                action = ModelImportAction3D.UNSUPPORTED,
                outputPath = null,
                message = unsupportedFormatMessage(inputPath)
            )
        }

        if (format.runtimeReady) {
            return ModelImportPlan3D(
                inputPath = inputPath,
                inputFormat = format,
                action = ModelImportAction3D.LOAD_DIRECTLY,
                outputPath = outputPath,
                message = "${format.label} is runtime-ready. Load it directly with ModelLoader3D."
            )
        }

        val resolvedOutput = outputPath ?: inputPath.withExtension("glb")
        require(resolvedOutput.fileName().substringAfterLast('.', "").lowercase() == "glb") {
            "Model import conversion output must end in .glb: $resolvedOutput"
        }
        return ModelImportPlan3D(
            inputPath = inputPath,
            inputFormat = format,
            action = ModelImportAction3D.CONVERT_TO_GLB,
            outputPath = resolvedOutput,
            message = "${format.label} requires offline conversion to GLB before runtime loading."
        )
    }

    private fun unsupportedFormatMessage(inputPath: String): String {
        return "Unsupported model format for asset: $inputPath. " +
            "Runtime formats: ${ModelImportFormat3D.runtimeExtensions().joinToString(", ")}. " +
            "Conversion candidates: ${ModelImportFormat3D.conversionCandidateExtensions().joinToString(", ")}."
    }
}

private fun String.fileName(): String {
    return substringAfterLast('/').substringAfterLast('\\')
}

private fun String.withExtension(extension: String): String {
    val fileStart = maxOf(lastIndexOf('/'), lastIndexOf('\\')) + 1
    val extensionStart = lastIndexOf('.')
    return if (extensionStart >= fileStart) {
        substring(0, extensionStart + 1) + extension
    } else {
        "$this.$extension"
    }
}
