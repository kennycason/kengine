package com.kengine.three.importer

import com.kengine.three.ModelInfo3D
import com.kengine.three.ModelLoader3D

enum class ModelAssetPreflightStatus3D {
    LOADABLE,
    EXTERNAL_EXPORT_REQUIRED,
    UNSUPPORTED,
    INVALID_RUNTIME_ASSET
}

data class ModelAssetPreflightResult3D(
    val plan: ModelImportPlan3D,
    val status: ModelAssetPreflightStatus3D,
    val modelInfo: ModelInfo3D? = null,
    val message: String
) {
    val loadable: Boolean
        get() = status == ModelAssetPreflightStatus3D.LOADABLE
}

object ModelAssetPreflight3D {
    fun inspect(
        inputPath: String,
        suggestedRuntimePath: String? = null
    ): ModelAssetPreflightResult3D {
        val plan = ModelImportPlanner3D.plan(inputPath, suggestedRuntimePath)
        return when (plan.action) {
            ModelImportAction3D.LOAD_DIRECTLY -> inspectRuntimeAsset(plan)
            ModelImportAction3D.EXTERNAL_EXPORT_REQUIRED -> ModelAssetPreflightResult3D(
                plan = plan,
                status = ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED,
                message = plan.message
            )
            ModelImportAction3D.UNSUPPORTED -> ModelAssetPreflightResult3D(
                plan = plan,
                status = ModelAssetPreflightStatus3D.UNSUPPORTED,
                message = plan.message
            )
        }
    }

    private fun inspectRuntimeAsset(plan: ModelImportPlan3D): ModelAssetPreflightResult3D {
        return try {
            val info = ModelLoader3D.inspect(plan.inputPath)
            ModelAssetPreflightResult3D(
                plan = plan,
                status = ModelAssetPreflightStatus3D.LOADABLE,
                modelInfo = info,
                message = "${plan.inputFormat?.label ?: "Model"} inspected successfully: ${info.summaryText()}."
            )
        } catch (error: Throwable) {
            ModelAssetPreflightResult3D(
                plan = plan,
                status = ModelAssetPreflightStatus3D.INVALID_RUNTIME_ASSET,
                message = "Runtime model inspection failed: ${error.firstUsefulMessage()}"
            )
        }
    }

    private fun ModelInfo3D.summaryText(): String {
        val pieces = mutableListOf<String>()
        if (meshCount > 0) pieces += "meshes=$meshCount"
        if (primitiveCount > 0) pieces += "primitives=$primitiveCount"
        if (vertexCount > 0) pieces += "vertices=$vertexCount"
        if (materialCount > 0) pieces += "materials=$materialCount"
        if (textureCount > 0) pieces += "textures=$textureCount"
        if (animationCount > 0) pieces += "animations=$animationCount"
        if (skinCount > 0) pieces += "skins=$skinCount"
        return pieces.joinToString(", ").ifBlank { "format=$format" }
    }

    private fun Throwable.firstUsefulMessage(): String {
        var current: Throwable? = this
        var remaining = 6
        while (current != null && remaining > 0) {
            val message = current.message?.trim()
            if (!message.isNullOrBlank()) {
                return message
            }
            current = current.cause
            remaining -= 1
        }
        return "unknown error"
    }
}
