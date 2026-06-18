plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "kengine.chat-demo"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
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
            linkerOpts(PlatformConfig.sharedLibLinkerOpts("SDL3", "SDL3_net"))
        }
        binaries {
            executable("chatServer") {
                entryPoint = "chatdemo.server.main"
            }
            executable("chatClient") {
                entryPoint = "chatdemo.client.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kengine-network"))
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
            }
        }
    }
}
