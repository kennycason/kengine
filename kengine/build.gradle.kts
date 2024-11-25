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
        hostOs == "Mac OS X" && isArm64 -> macosArm64()
        hostOs == "Mac OS X" && !isArm64 -> macosX64()
        hostOs == "Linux" && isArm64 -> linuxArm64()
        hostOs == "Linux" && !isArm64 -> linuxX64()
        isMingwX64 -> mingwX64()
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        // library module, we don't need to configure binaries here.
//        binaries {
//            executable {
//                entryPoint = "main"
//                linkerOpts("-L/opt/homebrew/lib", "-lSDL2")
//            }
//        }

        compilations["main"].cinterops {
            val sdl2 by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2.def")
                compilerOpts("-I/opt/homebrew/include/SDL2")
//                linkerOpts("-L/opt/homebrew/lib", "-lSDL2")
            }
            val sdl2Mixer by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2_mixer.def")
                compilerOpts("-I/opt/homebrew/include/SDL2")
//                linkerOpts("-L/opt/homebrew/lib", "-lSDL2_mixer")
            }
        }
    }

    targets {
        all {
            compilations.all {
                val linkerOptions = listOf(
                    "-L/opt/homebrew/lib",
                    "-lSDL2",
                    "-lSDL2_mixer"
                )
                kotlinOptions.freeCompilerArgs += linkerOptions.flatMap { listOf("-linker-option", it) }

                kotlinOptions {
                    freeCompilerArgs += listOf(
                        "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                        "-opt-in=kotlin.ExperimentalStdlibApi",
                        "-g", // enable debug symbols
                        "-ea" // enable assertions
                    )
                }
            }
        }
    }

    sourceSets {
        val mainSourceSet = getByName("${nativeTarget.name}Main") {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
            }
        }
    }
//    sourceSets {
//        val nativeMain by getting {
//            dependencies {
//                implementation(libs.kotlinxSerializationJson)
//            }
//        }
//    }
}