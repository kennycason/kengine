import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "kengine.boxxle"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"

    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native") { configureTarget() }
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native") { configureTarget() }
        hostOs == "Linux" && isArm64 -> linuxArm64("native") { configureTarget() }
        hostOs == "Linux" && !isArm64 -> linuxX64("native") { configureTarget() }
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
            linkerOpts("-L/opt/homebrew/lib", "-lSDL2", "-lSDL2_mixer")
        }
    }
    compilations.all {
        compileTaskProvider.configure {
            compilerOptions {
                val includeBinariesArgs = buildIncludeBinaryArgsForAssets()
                val compilerArgs = listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.ExperimentalStdlibApi",
                    "-g", // enable debug symbols
                    "-ea" // enable assertions
                ) + includeBinariesArgs
                freeCompilerArgs.addAll(compilerArgs)
            }
        }
    }
}

// NOTE copy assets alongside executable. This is a workaround until I figure out how to embed data files into the exe file.
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

private fun buildIncludeBinaryArgsForAssets(): List<String> {
    val assetsDir = File(project.projectDir, "assets")
    if (!(assetsDir.exists() && assetsDir.isDirectory)) {
        println("Warning: 'assets' directory not found or is not a directory.")
        return listOf()
    }
    return assetsDir
        .walkTopDown()
        .filter { it.isFile }
        .map { "-include-binary=${it.absolutePath}" }
        .toList()
}


