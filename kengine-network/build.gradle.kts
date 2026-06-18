plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    id("kengine.sdl-dylib")
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            "-opt-in=kotlin.ExperimentalStdlibApi"
        )
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
    if (publishAllNativeTargets) {
        val commonMain = sourceSets.getByName("commonMain")
        val commonTest = sourceSets.getByName("commonTest")
        nativeTargets.forEach { nativeTarget ->
            sourceSets.getByName("${nativeTarget.name}Main").apply {
                dependsOn(commonMain)
                kotlin.srcDir("src/nativeMain/kotlin")
                dependencies {
                    implementation(project(":kengine"))
                    implementation(project(":kengine-reactive"))
                    api(libs.kotlinxSerializationJson)
                    api(libs.kotlinxCoroutinesCore)
                }
            }
            sourceSets.getByName("${nativeTarget.name}Test").apply {
                dependsOn(commonTest)
                kotlin.srcDir("src/nativeTest/kotlin")
                dependencies {
                    implementation(project(":kengine-test"))
                    implementation(kotlin("test"))
                }
            }
        }
    } else {
        sourceSets.maybeCreate("nativeMain").dependsOn(sourceSets.getByName("commonMain"))
        sourceSets.maybeCreate("nativeTest").dependsOn(sourceSets.getByName("commonTest"))
        nativeTargets.forEach { nativeTarget ->
            sourceSets.getByName("${nativeTarget.name}Main").dependsOn(sourceSets.getByName("nativeMain"))
            sourceSets.getByName("${nativeTarget.name}Test").dependsOn(sourceSets.getByName("nativeTest"))
        }
    }

    nativeTargets.forEach { nativeTarget ->
        nativeTarget.apply {
            binaries.all {
                linkerOpts(PlatformConfig.sharedLibLinkerOpts("SDL3", "SDL3_net"))
            }

            compilations["main"].cinterops {
                val sdl3net by creating {
                    defFile = file("src/nativeInterop/cinterop/sdl3_net.def")
                    compilerOpts(PlatformConfig.compilerOpts)
                }
            }

            compilations["main"].compileTaskProvider.configure {
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

        if (!publishAllNativeTargets) {
            nativeMain {
                dependsOn(commonMain)
                dependencies {
                    implementation(project(":kengine"))
                    implementation(project(":kengine-reactive"))
                    api(libs.kotlinxSerializationJson)
                    api(libs.kotlinxCoroutinesCore)
                }
            }

            nativeTest {
                dependsOn(commonTest)
                dependencies {
                    implementation(project(":kengine-test"))
                    implementation(kotlin("test"))
                }
            }
        }
    }
}
