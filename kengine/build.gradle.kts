plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
//    id("kengine.native") // Uncomment if needed later
    id("kengine.assets")
    id("kengine.sdl-dylib")
}

repositories {
    mavenCentral()
}

kotlin {
    // JVM and JS targets
    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    val publishAllNativeTargets = providers.gradleProperty("kengine.publish.allNativeTargets")
        .map(String::toBoolean)
        .getOrElse(false)
    val nativeTargets = if (publishAllNativeTargets) {
        listOf(macosArm64(), linuxX64(), mingwX64())
    } else {
        listOf(
            when (KengineHostTarget.name) {
                "macosArm64" -> macosArm64()
                "macosX64" -> macosX64()
                "linuxX64" -> linuxX64()
                "linuxArm64" -> linuxArm64()
                "mingwX64" -> mingwX64()
                else -> throw GradleException("Host target [${KengineHostTarget.name}] is not supported.")
            }
        )
    }
    sourceSets.maybeCreate("nativeMain").dependsOn(sourceSets.getByName("commonMain"))
    sourceSets.maybeCreate("nativeTest").dependsOn(sourceSets.getByName("commonTest"))
    nativeTargets.forEach { nativeTarget ->
        sourceSets.getByName("${nativeTarget.name}Main").dependsOn(sourceSets.getByName("nativeMain"))
        sourceSets.getByName("${nativeTarget.name}Test").dependsOn(sourceSets.getByName("nativeTest"))
    }

    nativeTargets.forEach { nativeTarget ->
        nativeTarget.apply {
            binaries {
                staticLib()
                if (!PlatformConfig.isWindows) {
                    sharedLib {
                        baseName = "kengine"
                    }
                }
                all {
                    linkerOpts(PlatformConfig.sharedLibLinkerOpts("SDL3", "SDL3_image", "SDL3_ttf"))
                }
            }

            compilations["main"].cinterops {
                val sdl3 by creating {
                    defFile = file("src/nativeInterop/cinterop/sdl3.def")
                    compilerOpts(PlatformConfig.compilerOpts)
                }
                val sdl3image by creating {
                    defFile = file("src/nativeInterop/cinterop/sdl3_image.def")
                    compilerOpts(PlatformConfig.compilerOpts)
                }
                val sdl3ttf by creating {
                    defFile = file("src/nativeInterop/cinterop/sdl3_ttf.def")
                    compilerOpts(PlatformConfig.compilerOpts)
                }
            }

            compilations["main"].compileTaskProvider.configure {
                kotlinOptions {
                    freeCompilerArgs += listOf(
                        "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                        "-opt-in=kotlin.ExperimentalStdlibApi",
                        // "-g",  // Enable debug symbols (removed due to conflict with -opt)
                        "-ea"  // Enable assertions
                    )
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting

        nativeMain {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)

                api(libs.kotlinxSerializationJson) // Expose API dependencies for reuse
                api(libs.kotlinxCoroutinesCore)
                implementation(project(":kengine-reactive"))
            }
        }

        nativeTest {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":kengine-test"))
            }
        }
    }
}
