import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "kengine.playdate"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val nativeTarget: KotlinNativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            sharedLib {
                baseName = "kengine-playdate" // Generates libkengine-playdate.dylib/.so/.dll

                val sdkPath = System.getenv("PLAYDATE_SDK_PATH")
                    ?: throw GradleException("Environment variable PLAYDATE_SDK_PATH is not set.")

                linkerOpts(
                    "-L/opt/homebrew/lib",
                    "-lSDL2",
                    "-lSDL2_mixer",
                    "-lSDL2_ttf",
                    "-I${sdkPath}/C_API",
                    "-I${project.file("src/nativeInterop/cinterop").absolutePath}" // Include bridge headers
                )
            }
        }
        compilations["main"].cinterops {
            val playdate by creating {
                // Updated from 'defFile' to 'definitionFile'
                definitionFile = file("src/nativeInterop/cinterop/playdate.def")
                val sdkPath = System.getenv("PLAYDATE_SDK_PATH")
                    ?: throw GradleException("Environment variable PLAYDATE_SDK_PATH is not set.")

                compilerOpts(
                    "-I${sdkPath}/C_API", // Include Playdate SDK headers
                    "-I${project.file("src/nativeInterop/cinterop").absolutePath}", // Include bridge headers
                    "-DTARGET_PLAYDATE=1", // Example macro definition
                    "-DTARGET_EXTENSION=1"
                )
            }
        }
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(
                        listOf(
                            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                            "-opt-in=kotlin.ExperimentalStdlibApi",
                            "-g", // Enable debug symbols
                            "-ea" // Enable assertions
                        )
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kengine"))
            }
        }
//        val nativeMain by creating { // Create 'nativeMain' source set
//            dependsOn(commonMain)
//            // Optionally, you can specify additional dependencies or configurations here
//        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
//
//fun KotlinNativeTarget.configureLibraryTarget() {
//    binaries {
//        sharedLib {
//            baseName = "kengine-playdate" // generates libkengine-playdate.dylib/.so/.dll
//            linkerOpts(
//                "-L/opt/homebrew/lib",
//                "-lSDL2",
//                "-lSDL2_mixer",
//                "-lSDL2_ttf"
//            )
//        }
//    }
//
//    compilations["main"].cinterops {
//        val playdate by creating {
//            defFile = file("src/nativeInterop/cinterop/playdate.def")
//
//            // ensure PLAYDATE_SDK_PATH is set
//            val sdkPath = System.getenv("PLAYDATE_SDK_PATH")
//                ?: throw GradleException("Environment variable PLAYDATE_SDK_PATH is not set.")
//
//            // add compiler options for cinterop
//            compilerOpts(
//                "-I${sdkPath}/C_API",
//                "-I${project.file("src/nativeInterop/cinterop").absolutePath}", // Include bridge headers
//                "-DTARGET_PLAYDATE=1", // Define macros as per common.mk
//                "-DTARGET_EXTENSION=1",
//                "-mfloat-abi=hard",
//                "-mfpu=fpv5-sp-d16",
//                "-D__FPU_USED=1",
//                "-g", // enable debug symbols
//                "-ea" // enable assertions
//            )
//        }
//    }
//
//    compilations.all {
//        compileTaskProvider.configure {
//            compilerOptions {
//                freeCompilerArgs.addAll(
//                    listOf(
//                        "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
//                        "-opt-in=kotlin.ExperimentalStdlibApi",
//                        "-g", // enable debug symbols
//                        "-ea" // enable assertions
//                    )
//                )
//            }
//        }
//    }
//}

// pd_api.h pd_api/*