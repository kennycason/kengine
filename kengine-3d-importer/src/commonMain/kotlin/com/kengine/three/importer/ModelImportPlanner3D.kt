package com.kengine.three.importer

enum class ModelImportAction3D {
    LOAD_DIRECTLY,
    EXTERNAL_EXPORT_REQUIRED,
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

        fun externalExportExtensions(): List<String> {
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
    val suggestedRuntimePath: String?,
    val message: String
) {
    val runtimeReady: Boolean
        get() = action == ModelImportAction3D.LOAD_DIRECTLY

    val requiresExternalExport: Boolean
        get() = action == ModelImportAction3D.EXTERNAL_EXPORT_REQUIRED
}

object ModelImportPlanner3D {
    fun plan(
        inputPath: String,
        suggestedRuntimePath: String? = null
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
                suggestedRuntimePath = null,
                message = unsupportedFormatMessage(inputPath)
            )
        }

        if (format.runtimeReady) {
            return ModelImportPlan3D(
                inputPath = inputPath,
                inputFormat = format,
                action = ModelImportAction3D.LOAD_DIRECTLY,
                suggestedRuntimePath = null,
                message = "${format.label} is runtime-ready. Load it directly with ModelLoader3D."
            )
        }

        val resolvedOutput = suggestedRuntimePath ?: inputPath.withExtension("glb")
        require(resolvedOutput.fileName().substringAfterLast('.', "").lowercase() == "glb") {
            "Suggested runtime model path must end in .glb: $resolvedOutput"
        }
        return ModelImportPlan3D(
            inputPath = inputPath,
            inputFormat = format,
            action = ModelImportAction3D.EXTERNAL_EXPORT_REQUIRED,
            suggestedRuntimePath = resolvedOutput,
            message = "${format.label} is not a Kengine runtime format. Export it to GLB from your asset tool, then load the GLB."
        )
    }

    private fun unsupportedFormatMessage(inputPath: String): String {
        return "Unsupported model format for asset: $inputPath. " +
            "Runtime formats: ${ModelImportFormat3D.runtimeExtensions().joinToString(", ")}. " +
            "Source formats that require external GLB export: ${ModelImportFormat3D.externalExportExtensions().joinToString(", ")}."
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
