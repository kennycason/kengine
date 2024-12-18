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
    val nativeTarget = when {
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
                    // Set runtime library paths
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
            val chipmunk by creating {
                defFile = file("src/nativeInterop/cinterop/chipmunk.def")
                compilerOpts("-I/usr/local/include") // Adjust if Chipmunk headers are elsewhere
            }
        }

        compilations["main"].compileTaskProvider.configure {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.ExperimentalStdlibApi",
                    "-g",  // Enable debug symbols
                    "-ea"  // Enable assertions
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

val copyDylibs = listOf(
    "/usr/local/lib/libSDL3.0.dylib" to "${buildDir}/bin/native/Frameworks",
    "/usr/local/lib/libSDL3.0.dylib" to "${buildDir}/bin/native/debugTest/Frameworks",
    "/usr/local/lib/libSDL3_image.dylib" to "${buildDir}/bin/native/Frameworks",
    "/usr/local/lib/libSDL3_image.dylib" to "${buildDir}/bin/native/debugTest/Frameworks"
)

copyDylibs.forEach { (fromPath, toPath) ->
    val dylibName = fromPath.substringAfterLast("/")
    val targetDir = toPath.substringAfter("${buildDir}/bin/native/")

    // generate a descriptive task name
    val taskName = when {
        targetDir.contains("debugTest") -> "copy${dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()}ToDebugTestFrameworks"
        else -> "copy${dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()}ToFrameworks"
    }

    tasks.register<Copy>(taskName) {
        description = "Copy $dylibName to ${targetDir}"
        from(fromPath)
        into(toPath)
    }
}

tasks.named("nativeTest") {
    dependsOn(
        copyDylibs.map { (from, to) ->
            val dylibName = from.substringAfterLast("/")
            val targetDir = to.substringAfter("${buildDir}/bin/native/")
            when {
                targetDir.contains("debugTest") -> "copy${dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()}ToDebugTestFrameworks"
                else -> "copy${dylibName.removePrefix("lib").removeSuffix(".dylib").capitalize()}ToFrameworks"
            }
        }
    )
}
