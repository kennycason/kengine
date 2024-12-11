// build.gradle.kts

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

    // **Updated Native Target Naming: Named as "native" for consistency**
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native") // Named "native"
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native") // Named "native"
        hostOs == "Linux" && isArm64 -> linuxArm64("native")    // Named "native"
        hostOs == "Linux" && !isArm64 -> linuxX64("native")    // Named "native"
        hostOs.startsWith("Windows") -> mingwX64("native")     // Named "native"
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            sharedLib {
                baseName = "kengine" // Generates libkengine.dylib, libkengine.so, kengine.dll
                linkerOpts(
                    "-L/opt/homebrew/lib",
                    "-lSDL2",
                    "-lSDL2_mixer",
                    "-lSDL2_net",
                    "-lSDL2_ttf",
                    "-L${projectDir}/build"
                )
            }
        }
        compilations["main"].cinterops {
            val sdl2 by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2.def")
            }
            val sdl2Mixer by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2_mixer.def")
            }
            val sdl2gfx by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2_gfx.def")
            }
            val sdl2net by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2_net.def")
            }
            val sdl2ttf by creating {
                defFile = file("src/nativeInterop/cinterop/sdl2_ttf.def")
            }
            val box2d by creating {
                defFile = file("src/nativeInterop/cinterop/box2d.def")
                includeDirs.headerFilterOnly(file("src/nativeInterop/cinterop/include"))
            }
        }
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(
                        listOf(
                            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                            "-opt-in=kotlin.ExperimentalStdlibApi",
                            "-g",  // Enable debug symbols
                            "-ea"  // Enable assertions
                        )
                    )
                }
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
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// **Retrieve the native target name, which is now "native"**
val nativeTargetName = kotlin.targets.find { it.name == "native" }?.name?.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
} ?: throw GradleException("Native target not found.")

// **Register the buildCpp task**
tasks.register<Exec>("buildCpp") {
    workingDir = file("src/main/cpp")
    commandLine("sh", "-c", """
        mkdir -p build
        cd build
        cmake .. -DCMAKE_BUILD_TYPE=Release -DBOX2D_BUILD_EXAMPLES=OFF -DBOX2D_BUILD_UNIT_TESTS=OFF
        cmake --build . --config Release
    """.trimIndent())
}

// **Ensure the C++ build runs before Kotlin compilation**
tasks.named("compile${nativeTargetName}MainKotlin") {
    dependsOn("buildCpp")
}