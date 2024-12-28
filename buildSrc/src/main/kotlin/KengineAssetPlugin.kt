import org.gradle.api.Plugin
import org.gradle.api.Project

class KengineAssetPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.afterEvaluate {
            val assetCopier = KengineAssetCopier(project)
            assetCopier.registerAssetCopyTasks()

            // Make the binary args available as a project extension
            project.extensions.add("assetBinaryArgs", assetCopier.buildIncludeBinaryArgsForAssets())
        }
    }
}
