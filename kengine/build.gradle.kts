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
    val hostOs = System.getProperty("os.name").also { println("OS: $it") }
    val isArm64 = System.getProperty("os.arch").also { println("Arch: $it") } == "aarch64"
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            staticLib()
            sharedLib {
                baseName = "kengine"
            }
            all {
                linkerOpts(PlatformConfig.sharedLibLinkerOpts("SDL3", "SDL3_image", "SDL3_ttf"))
            }
        }

        compilations["main"].cinterops {
            val sdl3 by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3.def")
                compilerOpts(PlatformConfig.compilerOpts)
            }
            val sdl3image by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_image.def")
                compilerOpts(PlatformConfig.compilerOpts)
            }
            val sdl3ttf by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_ttf.def")
                compilerOpts(PlatformConfig.compilerOpts)
            }
        }

        compilations["main"].compileTaskProvider.configure {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.ExperimentalStdlibApi",
                    // "-g",  // Enable debug symbols (removed due to conflict with -opt)
                    "-ea"  // Enable assertions
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting

        nativeMain {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)

                api(libs.kotlinxSerializationJson) // Expose API dependencies for reuse
                api(libs.kotlinxCoroutinesCore)
                implementation(project(":kengine-reactive"))
            }
        }

        nativeTest {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":kengine-test"))
            }
        }
    }
}

