
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

class SdlDylibCopier(private val project: Project) {

    fun registerSDLDylibs() {
        // Determine which libraries to copy based on the module
        val dylibsToCopy = when (project.name) {
            "kengine-network" -> listOf(
                "/usr/local/lib/libSDL3.0.dylib",
                "/usr/local/lib/libSDL3_net.0.dylib",
                "/usr/local/lib/libSDL3_image.0.dylib",
                "/usr/local/lib/libSDL3_ttf.0.dylib"
            )
            "kengine-physics" -> listOf(
                "/usr/local/lib/libSDL3.0.dylib",
                "/opt/homebrew/lib/libchipmunk.dylib"
            )
            "kengine-sound" -> listOf(
                "/usr/local/lib/libSDL3.0.dylib",
                "/usr/local/lib/libSDL3_mixer.0.dylib",
                "/usr/local/lib/libSDL3_image.0.dylib",
                "/usr/local/lib/libSDL3_ttf.0.dylib"
            )
            else -> listOf(
                "/usr/local/lib/libSDL3.0.dylib",
                "/usr/local/lib/libSDL3_image.0.dylib",
                "/usr/local/lib/libSDL3_mixer.0.dylib",
                "/usr/local/lib/libSDL3_ttf.0.dylib"
            )
        }

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

                        // Ensure the target directory exists and has write permissions
                        doFirst {
                            println("[${project.name}] Copying $dylibPath to $toDir")
                            project.mkdir(toDir)

                            // Delete the target file if it already exists to avoid permission issues
                            val targetFile = project.file("$toDir/$dylibName")
                            if (targetFile.exists()) {
                                targetFile.delete()
                            }
                        }

                        // Set file permissions after copying
                        fileMode = 0b111101101 // rwxr-xr-x (755)
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
