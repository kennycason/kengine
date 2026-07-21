import com.kengine.three.ModelFormat3D
import com.kengine.three.ModelInfo3D
import com.kengine.three.importer.ModelAssetPreflightResult3D
import com.kengine.three.importer.ModelAssetPreflightStatus3D
import com.kengine.three.importer.ModelImportAction3D
import com.kengine.three.importer.ModelImportFormat3D
import com.kengine.three.importer.ModelImportPlan3D
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewerAssetHealthTest {
    @Test
    fun summaryReportsCleanPresetBatch() {
        val reports = listOf(
            report("Mario", ModelAssetPreflightStatus3D.LOADABLE),
            report("Bowser", ModelAssetPreflightStatus3D.LOADABLE)
        )

        assertEquals("Asset report OK 2/2", viewerAssetHealthSummary(reports))
    }

    @Test
    fun summaryCountsExportAndFailedPresets() {
        val reports = listOf(
            report("Mario", ModelAssetPreflightStatus3D.LOADABLE),
            report("Vehicle", ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED),
            report("Broken", ModelAssetPreflightStatus3D.INVALID_RUNTIME_ASSET),
            report("Unknown", ModelAssetPreflightStatus3D.UNSUPPORTED)
        )

        assertEquals("Asset report OK 1 export 1 failed 2", viewerAssetHealthSummary(reports))
    }

    @Test
    fun loadedAssetLineIncludesUsefulModelStats() {
        val info = ModelInfo3D(
            assetPath = "models/mario.glb",
            format = ModelFormat3D.GLB,
            vertexCount = 42,
            primitiveCount = 3,
            materialCount = 2,
            textureCount = 1,
            animationCount = 4,
            skinCount = 1
        )

        assertEquals("HEALTH LOADED GLB P3 V42 M2 T1 A4 S1", viewerLoadedAssetHealthLine(info))
    }

    private fun report(
        label: String,
        status: ModelAssetPreflightStatus3D
    ): ViewerAssetHealthReport {
        val action = when (status) {
            ModelAssetPreflightStatus3D.LOADABLE,
            ModelAssetPreflightStatus3D.INVALID_RUNTIME_ASSET -> ModelImportAction3D.LOAD_DIRECTLY
            ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED -> ModelImportAction3D.EXTERNAL_EXPORT_REQUIRED
            ModelAssetPreflightStatus3D.UNSUPPORTED -> ModelImportAction3D.UNSUPPORTED
        }
        val format = when (status) {
            ModelAssetPreflightStatus3D.EXTERNAL_EXPORT_REQUIRED -> ModelImportFormat3D.FBX
            ModelAssetPreflightStatus3D.UNSUPPORTED -> null
            else -> ModelImportFormat3D.GLB
        }
        return ViewerAssetHealthReport(
            preset = ViewerModelPreset(
                label = label,
                modelPath = "models/$label.glb",
                mode = ViewerModelMode.AUTO,
                targetSize = 2.0
            ),
            resolvedPath = "/assets/models/$label.glb",
            result = ModelAssetPreflightResult3D(
                plan = ModelImportPlan3D(
                    inputPath = "/assets/models/$label.glb",
                    inputFormat = format,
                    action = action,
                    suggestedRuntimePath = null,
                    message = "$label test message"
                ),
                status = status,
                modelInfo = if (status == ModelAssetPreflightStatus3D.LOADABLE) {
                    ModelInfo3D(
                        assetPath = "/assets/models/$label.glb",
                        format = ModelFormat3D.GLB,
                        vertexCount = 3,
                        primitiveCount = 1
                    )
                } else {
                    null
                },
                message = "$label test message"
            )
        )
    }
}
