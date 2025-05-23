
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

class SdlDylibCopier(private val project: Project) {

    fun registerSDLDylibs() {
        val dylibsToCopy = listOf(
            "/usr/local/lib/libSDL3.0.dylib",
            "/usr/local/lib/libSDL3_image.0.dylib",
            "/usr/local/lib/libSDL3_mixer.0.dylib",
            "/usr/local/lib/libSDL3_net.dylib",  // fails if versioned lib included, symlink exits, not sure error
            "/usr/local/lib/libSDL3_ttf.0.dylib"
        )
        val dylibTargetDirs = listOf(
            "${project.buildDir}/bin/native/Frameworks",
            "${project.buildDir}/bin/native/debugExecutable/Frameworks",
            "${project.buildDir}/bin/native/debugTest/Frameworks"
        )
        registerCopyTasks(project, dylibsToCopy, dylibTargetDirs)
    }

    private fun registerCopyTasks(
        project: Project,
        dylibsToCopy: List<String>,
        dylibTargetDirs: List<String>
    ) {
        dylibsToCopy.forEach { dylibPath ->
            val dylibName = dylibPath.substringAfterLast("/")

            dylibTargetDirs.forEach { toDir ->
                val targetDir = toDir.substringAfter("${project.buildDir}/bin/native/")

                // Generate a **unique task name** using the module name
                val taskName = generateTaskName(project, dylibName, targetDir)

                // Check for existing tasks to avoid duplicates
                if (project.tasks.findByName(taskName) == null) {
                    project.tasks.register<Copy>(taskName) {
                        description = "Copy $dylibName to $targetDir for module ${project.name}"
                        from(dylibPath)
                        into(toDir)

                        doFirst {
                            println("[${project.name}] Copying $dylibPath to $toDir")
                        }
                    }
                } else {
                    println("Task $taskName already exists. Skipping registration.")
                }
            }
        }

        // Attach dependencies to the `nativeTest` task
        project.tasks.named("nativeTest") {
            dependsOn(
                dylibsToCopy.flatMap { dylibPath ->
                    val dylibName = dylibPath.substringAfterLast("/")
                    dylibTargetDirs.map { toDir ->
                        val targetDir = toDir.substringAfter("${project.buildDir}/bin/native/")
                        generateTaskName(project, dylibName, targetDir)
                    }
                }
            )
        }
    }

    private fun generateTaskName(project: Project, dylibName: String, targetDir: String): String {
        val moduleName = project.name.capitalize() // Use the module name
        val prefix = dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()
        return if (targetDir.contains("debugTest"))
            "copy${moduleName}${prefix}ToDebugTestFrameworks"
        else
            "copy${moduleName}${prefix}ToFrameworks"
    }
}
