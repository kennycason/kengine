import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "kengine.phsyics-demo"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"

    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native") {
            configureTarget()
        }
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native") {
            configureTarget()
        }
        hostOs == "Linux" && isArm64 -> linuxArm64("native") {
            configureTarget()
        }
        hostOs == "Linux" && !isArm64 -> linuxX64("native") {
            configureTarget()
        }
        else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kengine"))
            }
        }
        val nativeMain by getting {
            dependsOn(commonMain)
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

fun KotlinNativeTarget.configureTarget() {
    binaries {
        executable {
            entryPoint = "main"
            linkerOpts("-L/opt/homebrew/lib", "-lSDL2", "-lSDL2_mixer")
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
