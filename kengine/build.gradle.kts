plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "kengine"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val nativeTarget = when { // NOTE: linkerOpts require compiling platform to be Mac OS
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    nativeTarget.apply {
        binaries {
            sharedLib {
                baseName = "kengine"
                linkerOpts(
                    "-L/usr/local/lib",
                    "-L/opt/homebrew/lib",
                    "-lSDL3",
                    "-lSDL3_image",
                    "-lSDL3_mixer",
                    "-lSDL3_net",
                    "-lSDL3_ttf",
                    "-lchipmunk",
                    "-framework", "Cocoa",
                    "-framework", "IOKit",
                    "-framework", "CoreVideo",
                    "-framework", "CoreAudio",
                    "-framework", "AudioToolbox",
                    // set runtime library paths
                    "-Wl,-rpath,@executable_path/Frameworks",
                    "-Wl,-rpath,/usr/local/lib",
                    "-Wl,-rpath,/opt/homebrew/lib"
                )
            }
        }


        compilations["main"].cinterops {
            val sdl3 by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3.def")
                compilerOpts("-I/usr/local/include")
            }
            val sdl3image by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_image.def")
                compilerOpts("-I/usr/local/include")
            }
            val sdl3mixer by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_mixer.def")
                compilerOpts("-I/usr/local/include")
            }
            val sdl3net by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_net.def")
                compilerOpts("-I/usr/local/include")
            }
            val sdl3ttf by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_ttf.def")
                compilerOpts("-I/usr/local/include")
            }
            val chipmunk by creating {
                defFile = file("src/nativeInterop/cinterop/chipmunk.def")
                compilerOpts("-I/usr/local/include")
            }
        }

        compilations["main"].compileTaskProvider.configure {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.ExperimentalStdlibApi",
                    "-g",  // enable debug symbols
                    "-ea"  // enable assertions
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kengine-test"))
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val nativeMain by getting {
            dependsOn(commonMain)
        }

        val nativeTest by getting {
            dependsOn(commonTest)
        }
    }
}

SdlDylibCopier(project).registerSDLDylibs()

class SdlDylibCopier(private val project: Project) {

    fun registerSDLDylibs() {
        val dylibsToCopy = listOf(
            "/usr/local/lib/libSDL3.0.dylib",
            "/usr/local/lib/libSDL3_image.0.dylib",
            "/usr/local/lib/libSDL3_mixer.0.dylib",
            "/usr/local/lib/libSDL3_net.dylib",
            "/usr/local/lib/libSDL3_ttf.0.dylib"
        )
        val dylibTargetDirs = listOf(
            "${buildDir}/bin/native/Frameworks",
            "${buildDir}/bin/native/debugExecutable/Frameworks",
            "${buildDir}/bin/native/debugTest/Frameworks"
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
