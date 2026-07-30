import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class KengineNintendoSwitchGamePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<KengineNintendoSwitchGameExtension>(
            "kengineNintendoSwitch",
            project
        )

        project.afterEvaluate {
            registerSwitchNroTasks(project, extension)
        }
    }

    private fun registerSwitchNroTasks(
        project: Project,
        extension: KengineNintendoSwitchGameExtension
    ) {
        val artifactBaseName = extension.artifactBaseName.get()
        val switchOutputDir = project.layout.buildDirectory.dir("switch")
        val switchNro = switchOutputDir.map { it.file("$artifactBaseName.nro") }
        val switchBackendProject = project.rootProject.findProject(":kengine-nintendo-switch")

        if (switchBackendProject == null) {
            project.tasks.register("buildSwitchNro") {
                group = "switch"
                description = "Builds the Nintendo Switch NRO for ${project.path}."

                doFirst {
                    throw GradleException(
                        "Switch backend is not enabled. Re-run with -Pkengine.enableNintendoSwitch=true."
                    )
                }
            }
            return
        }

        val backendBuildTaskName = extension.backendBuildTaskName.orNull
            ?: kengineNintendoSwitchBuildTaskName(artifactBaseName)
        val backendNro = switchBackendProject.layout.buildDirectory.file(
            "switch/games/$artifactBaseName/$artifactBaseName.nro"
        )

        project.tasks.register<Copy>("packageSwitchNro") {
            group = "switch"
            description = "Copies the backend-built Nintendo Switch NRO for ${project.path}."
            dependsOn(":kengine-nintendo-switch:$backendBuildTaskName")

            from(backendNro)
            into(switchOutputDir)
            rename { "$artifactBaseName.nro" }

            outputs.file(switchNro)
        }

        project.tasks.register("buildSwitchNro") {
            group = "switch"
            description = "Builds the Nintendo Switch NRO for ${project.path}."
            dependsOn("packageSwitchNro")
        }
    }
}
