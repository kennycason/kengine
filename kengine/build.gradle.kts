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
        hostOs == "Mac OS X" && isArm64 -> macosArm64()
        hostOs == "Mac OS X" && !isArm64 -> macosX64()
        hostOs == "Linux" && isArm64 -> linuxArm64()
        hostOs == "Linux" && !isArm64 -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            sharedLib {
                baseName = "kengine" // generates libkengine.dylib, .so, .dll
                linkerOpts("-L/opt/homebrew/lib", "-lSDL2", "-lSDL2_mixer")
            }
        }
        compilations["main"].cinterops {
            val sdl2 by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2.def")
                compilerOpts("-I/opt/homebrew/include/SDL2")
            }
            val sdl2Mixer by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2_mixer.def")
                compilerOpts("-I/opt/homebrew/include/SDL2")
            }
        }
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(
                        listOf(
                            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                            "-opt-in=kotlin.ExperimentalStdlibApi",
                            "-g", // enable debug symbols
                            "-ea" // enable assertions
                        )
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
