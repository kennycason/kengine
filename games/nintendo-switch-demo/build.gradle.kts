import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "kengine.nintendo-switch-demo"
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
            linkerOpts(PlatformConfig.sharedLibLinkerOpts("SDL3", "SDL3_image", "SDL3_mixer", "SDL3_net", "SDL3_ttf", "chipmunk"))
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
                api(project(":kengine-core"))
            }
        }
        val nativeMain by getting {
            dependencies {
                implementation(project(":kengine"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val switchArtifactBaseName = "nintendo-switch-demo"
val switchOutputDir = layout.buildDirectory.dir("switch")
val switchNro = switchOutputDir.map { it.file("$switchArtifactBaseName.nro") }
val switchBackendProject = rootProject.findProject(":kengine-nintendo-switch")

if (switchBackendProject == null) {
    tasks.register("buildSwitchNro") {
        group = "switch"
        description = "Builds the Nintendo Switch NRO for this demo."

        doFirst {
            throw GradleException("Switch backend is not enabled. Re-run with -Pkengine.switch=true.")
        }
    }
} else {
    val backendNro = switchBackendProject.layout.buildDirectory.file("switch/kengine-nintendo-switch.nro")

    tasks.register<Copy>("packageSwitchNro") {
        group = "switch"
        description = "Copies the backend-built NRO into this game module's build directory."
        dependsOn(":kengine-nintendo-switch:buildSwitchNro")

        from(backendNro)
        into(switchOutputDir)
        rename { "$switchArtifactBaseName.nro" }

        outputs.file(switchNro)
    }

    tasks.register("buildSwitchNro") {
        group = "switch"
        description = "Builds the Nintendo Switch NRO for this demo."
        dependsOn("packageSwitchNro")
    }
}
