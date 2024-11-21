plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "kengine-sdl"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
                linkerOpts("-L/opt/homebrew/lib", "-lSDL2")
            }
        }

        compilations["main"].cinterops {
            val sdl2 by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2.def")
                compilerOpts("-I/opt/homebrew/include/SDL2")
                linkerOpts("-L/opt/homebrew/lib", "-lSDL2")
            }
        }
    }

    targets {
        all {
            compilations.all {
                kotlinOptions {
                    freeCompilerArgs += listOf(
                        "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                        "-opt-in=kotlin.ExperimentalStdlibApi"
                    )
                }
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
            }
        }
    }
}
