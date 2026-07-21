plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.sdl-dylib")
}

group = "kengine.3d-importer"
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

    val nativeTarget = when (KengineHostTarget.name) {
        "macosArm64" -> macosArm64()
        "macosX64" -> macosX64()
        "linuxX64" -> linuxX64()
        "linuxArm64" -> linuxArm64()
        "mingwX64" -> mingwX64()
        else -> throw GradleException("Host target [${KengineHostTarget.name}] is not supported.")
    }

    sourceSets.maybeCreate("nativeMain").dependsOn(sourceSets.getByName("commonMain"))
    sourceSets.maybeCreate("nativeTest").dependsOn(sourceSets.getByName("commonTest"))
    sourceSets.getByName("${nativeTarget.name}Main").dependsOn(sourceSets.getByName("nativeMain"))
    sourceSets.getByName("${nativeTarget.name}Test").dependsOn(sourceSets.getByName("nativeTest"))

    nativeTarget.apply {
        binaries.all {
            linkerOpts(PlatformConfig.sharedLibLinkerOpts("SDL3", "SDL3_image"))
        }
        binaries {
            executable {
                entryPoint = "com.kengine.three.importer.cli.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val nativeMain by getting {
            kotlin.srcDir("src/nativeMain/kotlin")
            dependencies {
                implementation(project(":kengine-3d"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeTest by getting {
            kotlin.srcDir("src/nativeTest/kotlin")
        }
    }
}
