plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
//    id("kengine.native") // Uncomment if needed later
    id("kengine.assets")
    id("kengine.sdl-dylib")
}

group = "kengine"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    // JVM and JS targets
    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    // native target detection
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
    }

    val vulkanSdkPath = System.getenv("VULKAN_SDK")
        ?: project.findProperty("vulkanSdkPath")
        ?: error("VULKAN_SDK env var AND gradle.properties vulkanSdkPath not set")

    nativeTarget.apply {
        binaries {
            staticLib()
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
                    "-L$vulkanSdkPath/lib",
                    "-lvulkan",
                    "-framework", "QuartzCore", // Required for MoltenVK (Metal support)
                    "-framework", "Cocoa",
                    "-framework", "IOKit",
                    "-framework", "CoreVideo",
                    "-framework", "CoreAudio",
                    "-framework", "AudioToolbox",
                    // Set runtime library paths
                    "-Wl,-rpath,@executable_path/Frameworks",
                    "-Wl,-rpath,/usr/local/lib",
                    "-Wl,-rpath,/opt/homebrew/lib"
                )
            }
        }

        // C interop configurations
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
            val vulkan by creating {
                defFile = file("src/nativeInterop/cinterop/vulkan.def")
                compilerOpts("-I$vulkanSdkPath/include")
                linkerOpts(
                    "-L${vulkanSdkPath}/lib",   // library path
                    "-lvulkan",                 // vulkan loader (only if explicitly required)
                    "-framework", "QuartzCore"  // metal backend dependency
                )
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
        nativeMain {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)

                api(libs.kotlinxSerializationJson) // expose API dependencies for reuse
                api(libs.kotlinxCoroutinesCore)
                implementation(project(":kengine-test"))
            }
        }

        nativeTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
