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
    // Define JVM target
    jvm()

    // Define JS target with both browser and Node.js support
    js(IR) {
        browser()
        nodejs()
    }

    // Determine the host OS and architecture
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"

    // Define the native target based on the host OS and architecture
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    // Configure the native target
    nativeTarget.apply {
        // Define the shared library binary
        binaries {
            sharedLib {
                baseName = "kengine"
                linkerOpts(
                    // Linker options for SDL3, SDL3_image, and Chipmunk
                    "-L/usr/local/lib",
                    "-L/opt/homebrew/lib", // Add Homebrew lib path if using Homebrew on macOS
                    "-lSDL3",
                    "-lSDL3_image",
                    "-lchipmunk", // Link against Chipmunk library
                    "-framework", "Cocoa",
                    "-framework", "IOKit",
                    "-framework", "CoreVideo",
                    // Set runtime library paths
                    "-Wl,-rpath,@executable_path/Frameworks",
                    "-Wl,-rpath,/usr/local/lib",
                    "-Wl,-rpath,/opt/homebrew/lib" // Add rpath for Homebrew lib if needed
                )
            }
        }

        // Configure CInterop for SDL3, SDL3_image, and Chipmunk
        compilations["main"].cinterops {
            // SDL3 CInterop
            val sdl3 by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3.def")
                compilerOpts("-I/usr/local/include")
            }
            // SDL3_image CInterop
            val sdl3image by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_image.def")
                compilerOpts("-I/usr/local/include")
            }
            // Chipmunk CInterop
            val chipmunk by creating {
                defFile = file("src/nativeInterop/cinterop/chipmunk.def")
                compilerOpts("-I/usr/local/include") // Adjust if Chipmunk headers are elsewhere
            }
        }

        // Set compiler options for the main compilation
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

    // Define source sets
    sourceSets {
        // Common main source set
        val commonMain by getting {
            dependencies {
                implementation(project(":kengine-test"))
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
            }
        }

        // Common test source set
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // Reference the existing nativeMain source set
        val nativeMain by getting {
            dependsOn(commonMain)
            // Removed the incorrect dependencies block
        }

        // Reference the existing nativeTest source set
        val nativeTest by getting {
            dependsOn(commonTest)
            // Add native-specific test dependencies here if needed
        }
    }
}

// Define a list of Copy operations as pairs of from and to paths
val copyDylibs = listOf(
    "/usr/local/lib/libSDL3.0.dylib" to "${buildDir}/bin/native/Frameworks",
    "/usr/local/lib/libSDL3.0.dylib" to "${buildDir}/bin/native/debugTest/Frameworks",
    "/usr/local/lib/libSDL3_image.dylib" to "${buildDir}/bin/native/Frameworks",
    "/usr/local/lib/libSDL3_image.dylib" to "${buildDir}/bin/native/debugTest/Frameworks"
)

// Register Copy tasks by iterating over the copyDylibs list
copyDylibs.forEach { (fromPath, toPath) ->
    val dylibName = fromPath.substringAfterLast("/")
    val targetDir = toPath.substringAfter("${buildDir}/bin/native/")

    // Generate a descriptive task name
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

// Make the nativeTest task depend on all Copy tasks
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
