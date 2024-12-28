import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "kengine.image-shuffle"
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
            resources.srcDir("assets")
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
            linkerOpts(
                "-L/usr/local/lib",
                "-L/opt/homebrew/lib",
                "-lSDL3",  // Note: you had SDL2 here, need SDL3
                "-lSDL3_image",
                "-lSDL3_mixer",
                "-lSDL3_net",
                "-lSDL3_ttf",
                "-lchipmunk",
                "-framework", "Cocoa",
                "-framework", "IOKit",
                "-framework", "CoreVideo",
                "-framework", "CoreAudio",
                "-framework", "AudioToolbox",
                // set runtime library paths
                "-Wl,-rpath,@executable_path/Frameworks",
                "-Wl,-rpath,/usr/local/lib",
                "-Wl,-rpath,/opt/homebrew/lib"
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
                        "-g", // enable debug symbols
                        "-ea" // enable assertions
                    )
                )
            }
        }
    }
}

tasks.register<Copy>("copyReleaseAssets") {
    from("assets")
    into("$buildDir/bin/native/releaseExecutable/assets")
}
tasks.register<Copy>("copyDebugAssets") {
    from("assets")
    into("$buildDir/bin/native/debugExecutable/assets")
}
tasks.named("build") {
    dependsOn("copyReleaseAssets")
    dependsOn("copyDebugAssets")
}
