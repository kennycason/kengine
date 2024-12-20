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

val dylibsToCopy = listOf(
    "/usr/local/lib/libSDL3.0.dylib",
    "/usr/local/lib/libSDL3_image.0.dylib",
    "/usr/local/lib/libSDL3_mixer.0.dylib",
    "/usr/local/lib/libSDL3_net.0.dylib",
    "/usr/local/lib/libSDL3_ttf.0.dylib"
)
val dylibTargetDirs = listOf(
    "${buildDir}/bin/native/Frameworks",
    "${buildDir}/bin/native/debugTest/Frameworks"
)

dylibsToCopy.forEach { dylibPath ->
    val dylibName = dylibPath.substringAfterLast("/")
    dylibTargetDirs.forEach { toDir ->
        val targetDir = toDir.substringAfter("${buildDir}/bin/native/")

        // generate a descriptive task name
        val taskName = if (targetDir.contains("debugTest"))
            "copy${dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()}ToDebugTestFrameworks"
        else "copy${dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()}ToFrameworks"
        tasks.register<Copy>(taskName) {
            description = "Copy $dylibName to $targetDir"
            from(dylibPath)
            into(toDir)
            doFirst {
                println("Copying $dylibPath to $toDir")
            }
        }
    }
}

tasks.named("nativeTest") {
    dependsOn(
        dylibsToCopy.flatMap { dylibPath ->
            val dylibName = dylibPath.substringAfterLast("/")
            dylibTargetDirs.map { toDir ->
                val targetDir = toDir.substringAfter("${buildDir}/bin/native/")
                if (targetDir.contains("debugTest"))
                    "copy${dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()}ToDebugTestFrameworks"
                else
                    "copy${dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()}ToFrameworks"
            }
        }
    )
}
