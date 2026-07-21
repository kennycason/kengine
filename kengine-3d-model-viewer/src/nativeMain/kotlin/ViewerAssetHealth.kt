import com.kengine.three.ModelAssetPathResolver3D
import com.kengine.three.ModelInfo3D
import com.kengine.three.importer.ModelAssetPreflight3D
import com.kengine.three.importer.ModelAssetPreflightResult3D
import com.kengine.three.importer.ModelAssetPreflightStatus3D

data class ViewerAssetHealthReport(
    val preset: ViewerModelPreset,
    val resolvedPath: String,
    val result: ModelAssetPreflightResult3D
) {
    val loadable: Boolean
        get() = result.loadable

    fun badge(): String {
        return when (result.status) {
            ModelAssetPreflightStatus3D.LOADABLE -> "OK"
            ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED -> "EXPORT"
            ModelAssetPreflightStatus3D.UNSUPPORTED -> "UNSUPPORTED"
            ModelAssetPreflightStatus3D.INVALID_RUNTIME_ASSET -> "FAILED"
        }
    }

    fun inspectorLine(): String {
        return when (result.status) {
            ModelAssetPreflightStatus3D.LOADABLE ->
                "HEALTH OK ${result.modelInfo?.healthStats() ?: result.plan.inputFormat?.label ?: "MODEL"}"
            ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED ->
                "HEALTH EXPORT ${result.plan.inputFormat?.label ?: "SOURCE"} TO GLB"
            ModelAssetPreflightStatus3D.UNSUPPORTED ->
                "HEALTH UNSUPPORTED FORMAT"
            ModelAssetPreflightStatus3D.INVALID_RUNTIME_ASSET ->
                "HEALTH FAILED ${result.plan.inputFormat?.label ?: "MODEL"}"
        }
    }

    fun consoleLine(index: Int): String {
        return "${index + 1}. [${badge()}] ${preset.label} -> ${result.message}"
    }
}

class ViewerAssetHealthCache(
    private val resolver: ModelAssetPathResolver3D
) {
    private val reportsByModelPath = mutableMapOf<String, ViewerAssetHealthReport>()

    fun inspect(
        preset: ViewerModelPreset,
        resolvedPath: String? = null,
        refresh: Boolean = false
    ): ViewerAssetHealthReport {
        if (!refresh) {
            reportsByModelPath[preset.modelPath]?.let { return it }
        }

        val path = resolvedPath ?: resolver.resolve(preset.modelPath)
        val report = ViewerAssetHealthReport(
            preset = preset,
            resolvedPath = path,
            result = ModelAssetPreflight3D.inspect(path)
        )
        reportsByModelPath[preset.modelPath] = report
        return report
    }

    fun inspectAll(
        presets: List<ViewerModelPreset>,
        refresh: Boolean = true
    ): List<ViewerAssetHealthReport> {
        return presets.map { inspect(it, refresh = refresh) }
    }

    fun get(preset: ViewerModelPreset): ViewerAssetHealthReport? {
        return reportsByModelPath[preset.modelPath]
    }

    fun rememberLoaded(
        preset: ViewerModelPreset,
        info: ModelInfo3D
    ) {
        val previous = reportsByModelPath[preset.modelPath]
        if (previous == null || previous.result.status != ModelAssetPreflightStatus3D.LOADABLE) {
            inspect(preset, resolvedPath = info.assetPath, refresh = true)
        }
    }
}

fun viewerAssetHealthSummary(
    reports: List<ViewerAssetHealthReport>
): String {
    val loadable = reports.count { it.result.status == ModelAssetPreflightStatus3D.LOADABLE }
    val export = reports.count { it.result.status == ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED }
    val failed = reports.count {
        it.result.status == ModelAssetPreflightStatus3D.INVALID_RUNTIME_ASSET ||
            it.result.status == ModelAssetPreflightStatus3D.UNSUPPORTED
    }
    return if (failed == 0 && export == 0) {
        "Asset report OK $loadable/${reports.size}"
    } else {
        "Asset report OK $loadable export $export failed $failed"
    }
}

fun viewerLoadedAssetHealthLine(info: ModelInfo3D): String {
    return "HEALTH LOADED ${info.healthStats()}"
}

private fun ModelInfo3D.healthStats(): String {
    val pieces = mutableListOf(format.name)
    if (primitiveCount > 0) pieces += "P$primitiveCount"
    if (vertexCount > 0) pieces += "V$vertexCount"
    if (materialCount > 0) pieces += "M$materialCount"
    if (textureCount > 0) pieces += "T$textureCount"
    if (animationCount > 0) pieces += "A$animationCount"
    if (skinCount > 0) pieces += "S$skinCount"
    return pieces.joinToString(" ")
}
