plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

// copies SDL3 libs
apply<SdlDylibPlugin>()

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
    val nativeTarget = when { // NOTE: linkerOpts require compiling platform to be Mac OS
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    nativeTarget.apply {
        binaries {
            sharedLib {
                baseName = "kengine"
                linkerOpts(
                    "-L/usr/local/lib",
                    "-L/opt/homebrew/lib",
                    "-lSDL3",
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


        compilations["main"].cinterops {
            val sdl3 by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3.def")
                compilerOpts("-I/usr/local/include")
            }
            val sdl3image by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_image.def")
                compilerOpts("-I/usr/local/include")
            }
            val sdl3mixer by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_mixer.def")
                compilerOpts("-I/usr/local/include")
            }
            val sdl3net by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_net.def")
                compilerOpts("-I/usr/local/include")
            }
            val sdl3ttf by creating {
                defFile = file("src/nativeInterop/cinterop/sdl3_ttf.def")
                compilerOpts("-I/usr/local/include")
            }
            val chipmunk by creating {
                defFile = file("src/nativeInterop/cinterop/chipmunk.def")
                compilerOpts("-I/usr/local/include")
            }
        }

        compilations["main"].compileTaskProvider.configure {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.ExperimentalStdlibApi",
                    "-g",  // enable debug symbols
                    "-ea"  // enable assertions
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kengine-test"))
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val nativeMain by getting {
            dependsOn(commonMain)
        }

        val nativeTest by getting {
            dependsOn(commonTest)
        }
    }
}
