
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.io.File

class SdlDylibCopier(private val project: Project) {

    private val isMacOS = PlatformConfig.isMacOS
    private val isLinux = PlatformConfig.isLinux

    private val searchPaths: List<String> = when {
        isMacOS -> listOf("/opt/homebrew/lib", "/usr/local/lib")
        isLinux -> listOf("/usr/local/lib", "/usr/lib/x86_64-linux-gnu", "/usr/lib")
        else -> listOf("/usr/local/lib")
    }

    /**
     * Resolve a shared library by base name (e.g. "SDL3"), searching platform-specific locations.
     * On macOS looks for .dylib, on Linux looks for .so.
     */
    private fun resolveLib(baseName: String): String {
        val candidates = if (isMacOS) {
            listOf("lib${baseName}.0.dylib", "lib${baseName}.dylib")
        } else {
            listOf("lib${baseName}.so.0", "lib${baseName}.so")
        }
        for (candidate in candidates) {
            for (searchPath in searchPaths) {
                val path = "$searchPath/$candidate"
                if (File(path).exists()) {
                    return path
                }
            }
        }
        throw IllegalStateException(
            "Could not find lib$baseName in any of: ${searchPaths.joinToString()}. " +
                if (isMacOS) "Install via Homebrew (brew install sdl3) or build from source (bash sdl3/build_sdl.sh)."
                else "Build from source (bash sdl3/build_sdl.sh) or install via your package manager."
        )
    }

    fun registerSDLDylibs() {
        val libBaseNames = when (project.name) {
            "kengine-network" -> listOf("SDL3", "SDL3_net", "SDL3_image", "SDL3_ttf")
            "kengine-physics" -> listOf("SDL3", "chipmunk")
            "kengine-sound" -> listOf("SDL3", "SDL3_mixer", "SDL3_image", "SDL3_ttf")
            else -> listOf("SDL3", "SDL3_image", "SDL3_mixer", "SDL3_ttf")
        }

        val libsToCopy = libBaseNames.map { resolveLib(it) }

        val libTargetDirs = listOf(
            "${project.buildDir}/bin/native/Frameworks",
            "${project.buildDir}/bin/native/debugExecutable/Frameworks",
            "${project.buildDir}/bin/native/debugTest/Frameworks"
        )
        registerCopyTasks(project, libsToCopy, libTargetDirs)
    }

    private fun registerCopyTasks(
        project: Project,
        libsToCopy: List<String>,
        libTargetDirs: List<String>
    ) {
        libsToCopy.forEach { libPath ->
            val libName = libPath.substringAfterLast("/")

            libTargetDirs.forEach { toDir ->
                val targetDir = toDir.substringAfter("${project.buildDir}/bin/native/")

                val taskName = generateTaskName(project, libName, targetDir)

                if (project.tasks.findByName(taskName) == null) {
                    project.tasks.register<Copy>(taskName) {
                        description = "Copy $libName to $targetDir for module ${project.name}"
                        from(libPath)
                        into(toDir)

                        doFirst {
                            println("[${project.name}] Copying $libPath to $toDir")
                            project.mkdir(toDir)

                            val targetFile = project.file("$toDir/$libName")
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

        project.tasks.named("nativeTest") {
            dependsOn(
                libsToCopy.flatMap { libPath ->
                    val libName = libPath.substringAfterLast("/")
                    libTargetDirs.map { toDir ->
                        val targetDir = toDir.substringAfter("${project.buildDir}/bin/native/")
                        generateTaskName(project, libName, targetDir)
                    }
                }
            )
        }
    }

    private fun generateTaskName(project: Project, libName: String, targetDir: String): String {
        val moduleName = project.name.capitalize()
        val prefix = libName
            .removePrefix("lib")
            .removeSuffix(".0.dylib")
            .removeSuffix(".dylib")
            .removeSuffix(".so.0")
            .removeSuffix(".so")
            .capitalize()
        return if (targetDir.contains("debugTest"))
            "copy${moduleName}${prefix}ToDebugTestFrameworks"
        else
            "copy${moduleName}${prefix}ToFrameworks"
    }
}
