import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class KengineN64GamePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<KengineN64GameExtension>(
            "kengineN64",
            project
        )

        project.afterEvaluate {
            registerN64Z64Tasks(project, extension)
        }
    }

    private fun registerN64Z64Tasks(
        project: Project,
        extension: KengineN64GameExtension
    ) {
        val artifactBaseName = extension.artifactBaseName.get()
        val n64Z64 = project.layout.buildDirectory.file("n64/$artifactBaseName.z64")

        if (!project.isNintendo64Enabled()) {
            project.registerDisabledN64Task()
            return
        }

        project.rootProject.findProject(":kengine-n64")
            ?: throw GradleException("N64 backend project :kengine-n64 is not included in this build.")

        val backendDockerBuildTaskName = kengineN64DockerBuildTaskName(artifactBaseName)

        project.tasks.register("buildN64Z64") {
            group = "n64"
            description = "Builds the Nintendo 64 Z64 ROM for ${project.path}."
            dependsOn(":kengine-n64:$backendDockerBuildTaskName")
        }

        project.tasks.register<Exec>("runN64") {
            group = "n64"
            description = "Builds and launches the $artifactBaseName ROM in ares."
            dependsOn("buildN64Z64")

            doFirst {
                val rom = n64Z64.get().asFile
                if (!rom.exists()) {
                    throw GradleException("ROM not found: ${rom.absolutePath}. Run buildN64Z64 first.")
                }
            }

            commandLine("open", "-a", "ares", n64Z64.get().asFile.absolutePath)
        }
    }

    private fun Project.isNintendo64Enabled(): Boolean {
        return providers.gradleProperty("kengine.enableNintendo64")
            .map { it.toBoolean() }
            .orElse(false)
            .get()
    }

    private fun Project.registerDisabledN64Task() {
        val projectPath = path
        tasks.register("buildN64Z64") {
            group = "n64"
            description = "Builds the Nintendo 64 Z64 ROM for $projectPath."

            doFirst {
                throw GradleException(
                    "Nintendo 64 backend is disabled. Re-run with -Pkengine.enableNintendo64=true."
                )
            }
        }
    }
}
