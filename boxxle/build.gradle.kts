import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "com.boxxle"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    // detect the host platform and configure the appropriate target
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"

    when {
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
        val commonMain by getting

        // add dependencies to the platform-specific source set
        val nativeMain = sourceSets.maybeCreate("nativeMain").apply {
            dependsOn(commonMain)
        }

        getByName("nativeMain") {
            dependencies {
                implementation(project(":kengine"))
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
        kotlinOptions {
            freeCompilerArgs += listOf(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-g",
                "-ea"
            )
        }
    }
}