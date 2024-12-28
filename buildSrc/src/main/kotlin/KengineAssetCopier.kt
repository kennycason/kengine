import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.io.File

class KengineAssetCopier(private val project: Project) {

    fun registerAssetCopyTasks() {
        // Register asset copy tasks for different build types
        registerCopyTask("copyReleaseAssets", "releaseExecutable")
        registerCopyTask("copyDebugAssets", "debugExecutable")

        // Make build task depend on asset copying
        project.tasks.named("build") {
            dependsOn("copyReleaseAssets", "copyDebugAssets")
        }
    }

    fun buildIncludeBinaryArgsForAssets(): List<String> {
        val assetsDir = File(project.projectDir, "assets")
        if (!(assetsDir.exists() && assetsDir.isDirectory)) {
            println("Warning: 'assets' directory not found or is not a directory.")
            return listOf()
        }
        return assetsDir
            .walkTopDown()
            .filter { it.isFile }
            .map { "-include-binary=${it.absolutePath}" }
            .toList()
    }

    private fun registerCopyTask(taskName: String, buildType: String) {
        project.tasks.register<Copy>(taskName) {
            from("assets")
            into("${project.buildDir}/bin/native/${buildType}/assets")
        }
    }
}
