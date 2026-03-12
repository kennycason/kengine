
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.io.File

class SdlDylibCopier(private val project: Project) {

    companion object {
        // Search paths in priority order: Homebrew (Apple Silicon), then /usr/local (source builds)
        private val LIB_SEARCH_PATHS = listOf(
            "/opt/homebrew/lib",
            "/usr/local/lib"
        )
    }

    /**
     * Resolve a dylib by name, searching brew and source-build locations.
     * Returns the first path found, or throws with a helpful message.
     */
    private fun resolveDylib(name: String): String {
        // Try versioned (.0.dylib) first, then unversioned (.dylib)
        val candidates = listOf(name, name.replace(".0.dylib", ".dylib"))
        for (candidate in candidates) {
            for (searchPath in LIB_SEARCH_PATHS) {
                val path = "$searchPath/$candidate"
                if (File(path).exists()) {
                    return path
                }
            }
        }
        throw IllegalStateException(
            "Could not find $name in any of: ${LIB_SEARCH_PATHS.joinToString()}. " +
                "Install via Homebrew (brew install sdl3) or build from source (bash sdl3/build_sdl.sh)."
        )
    }

    fun registerSDLDylibs() {
        // Define which dylib names each module needs (just the filenames)
        val dylibNames = when (project.name) {
            "kengine-network" -> listOf(
                "libSDL3.0.dylib",
                "libSDL3_net.0.dylib",
                "libSDL3_image.0.dylib",
                "libSDL3_ttf.0.dylib"
            )
            "kengine-physics" -> listOf(
                "libSDL3.0.dylib",
                "libchipmunk.dylib"
            )
            "kengine-sound" -> listOf(
                "libSDL3.0.dylib",
                "libSDL3_mixer.0.dylib",
                "libSDL3_image.0.dylib",
                "libSDL3_ttf.0.dylib"
            )
            else -> listOf(
                "libSDL3.0.dylib",
                "libSDL3_image.0.dylib",
                "libSDL3_mixer.0.dylib",
                "libSDL3_ttf.0.dylib"
            )
        }

        // Resolve each dylib name to its actual path
        val dylibsToCopy = dylibNames.map { resolveDylib(it) }

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

                val taskName = generateTaskName(project, dylibName, targetDir)

                if (project.tasks.findByName(taskName) == null) {
                    project.tasks.register<Copy>(taskName) {
                        description = "Copy $dylibName to $targetDir for module ${project.name}"
                        from(dylibPath)
                        into(toDir)

                        doFirst {
                            println("[${project.name}] Copying $dylibPath to $toDir")
                            project.mkdir(toDir)

                            val targetFile = project.file("$toDir/$dylibName")
                            if (targetFile.exists()) {
                                targetFile.delete()
                            }
                        }

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
        val moduleName = project.name.capitalize()
        val prefix = dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()
        return if (targetDir.contains("debugTest"))
            "copy${moduleName}${prefix}ToDebugTestFrameworks"
        else
            "copy${moduleName}${prefix}ToFrameworks"
    }
}
