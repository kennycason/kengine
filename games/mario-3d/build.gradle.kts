plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.assets")
    id("kengine.packaging")
}

group = "kengine.mario-3d"
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

    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64()
        hostOs == "Mac OS X" && !isArm64 -> macosX64()
        hostOs == "Linux" && isArm64 -> linuxArm64()
        hostOs == "Linux" && !isArm64 -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
    }

    sourceSets.maybeCreate("nativeMain").dependsOn(sourceSets.getByName("commonMain"))
    sourceSets.getByName("${nativeTarget.name}Main").dependsOn(sourceSets.getByName("nativeMain"))
    sourceSets.maybeCreate("nativeTest").dependsOn(sourceSets.getByName("commonTest"))
    sourceSets.getByName("${nativeTarget.name}Test").dependsOn(sourceSets.getByName("nativeTest"))

    nativeTarget.apply {
        binaries.all {
            linkerOpts(PlatformConfig.sharedLibLinkerOpts("SDL3", "SDL3_image", "SDL3_ttf"))
        }
        binaries {
            executable {
                entryPoint = "main"
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
        val nativeMain by getting {
            dependencies {
                implementation(project(":kengine"))
                implementation(project(":kengine-3d"))
                implementation(project(":kengine-3d-ui"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.matching { it.name == "linkDebugExecutable${KengineHostTarget.taskSuffix}" }.configureEach {
    finalizedBy("copyDebugAssets")
}
tasks.matching { it.name == "runDebugExecutable${KengineHostTarget.taskSuffix}" }.configureEach {
    dependsOn("copyDebugAssets")
}
tasks.matching { it.name == "linkReleaseExecutable${KengineHostTarget.taskSuffix}" }.configureEach {
    finalizedBy("copyReleaseAssets")
}
