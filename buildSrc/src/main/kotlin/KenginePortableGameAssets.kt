import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.io.File

fun Project.configureKenginePortableGameAssets(vararg assetSourceProjects: Project) {
    val assetProjects = assetSourceProjects.distinct()
    val debugAssets = registerPortableGameAssetCopyTask(assetProjects, "copyDebugPortableGameAssets", "debugExecutable")
    val releaseAssets = registerPortableGameAssetCopyTask(assetProjects, "copyReleasePortableGameAssets", "releaseExecutable")

    tasks.named("build") {
        dependsOn(debugAssets, releaseAssets)
    }

    tasks.matching { task ->
        task.name.startsWith("run") && task.name.contains("Executable")
    }.configureEach {
        dependsOn(debugAssets)
    }

    tasks.matching { it.name == "packageMacApp" }.configureEach {
        dependsOn(releaseAssets)
        doLast {
            val appName = name.replaceFirstChar { it.uppercase() }
            copyPortableGameAssetDirectories(
                assetProjects,
                layout.buildDirectory.dir("dist/$appName.app/Contents/Resources").get().asFile
            )
        }
    }

    tasks.matching { it.name == "packageLinux" }.configureEach {
        dependsOn(releaseAssets)
        doLast {
            copyPortableGameAssetDirectories(
                assetProjects,
                layout.buildDirectory.dir("dist/$name-linux/bin").get().asFile
            )
        }
    }

    tasks.matching { it.name == "packageWindows" }.configureEach {
        dependsOn(releaseAssets)
        doLast {
            copyPortableGameAssetDirectories(
                assetProjects,
                layout.buildDirectory.dir("dist/$name-windows").get().asFile
            )
        }
    }
}

private fun Project.registerPortableGameAssetCopyTask(
    assetSourceProjects: List<Project>,
    taskName: String,
    buildType: String
) = tasks.register<Copy>(taskName) {
    copySpecFromPortableGameAssetProjects(assetSourceProjects)
    into(layout.buildDirectory.dir(KengineHostTarget.binPath(buildType)))
}

private fun CopySpec.copySpecFromPortableGameAssetProjects(assetSourceProjects: List<Project>) {
    assetSourceProjects.forEach { sourceProject ->
        val assetsDir = sourceProject.layout.projectDirectory.dir("assets").asFile
        if (assetsDir.isDirectory) {
            from(assetsDir) {
                into("assets")
            }
        }

        val soundDir = sourceProject.layout.projectDirectory.dir("sound").asFile
        if (soundDir.isDirectory) {
            from(soundDir) {
                into("sound")
            }
        }
    }
}

private fun Project.copyPortableGameAssetDirectories(assetSourceProjects: List<Project>, destination: File) {
    copy {
        copySpecFromPortableGameAssetProjects(assetSourceProjects)
        into(destination)
    }
}
