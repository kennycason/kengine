import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.packaging")
}

group = "kengine.hextris-desktop"
version = "1.0.0"

repositories {
    mavenCentral()
}

val hextrisCoreProject = project(":games:hextris-core")
val copyDebugPortableAssets by tasks.registering(Copy::class) {
    from(hextrisCoreProject.layout.projectDirectory.dir("assets")) {
        into("assets")
    }
    from(hextrisCoreProject.layout.projectDirectory.dir("sound")) {
        into("sound")
    }
    into(layout.buildDirectory.dir(KengineHostTarget.binPath("debugExecutable")))
}

val copyReleasePortableAssets by tasks.registering(Copy::class) {
    from(hextrisCoreProject.layout.projectDirectory.dir("assets")) {
        into("assets")
    }
    from(hextrisCoreProject.layout.projectDirectory.dir("sound")) {
        into("sound")
    }
    into(layout.buildDirectory.dir(KengineHostTarget.binPath("releaseExecutable")))
}

tasks.named("build") {
    dependsOn(copyDebugPortableAssets, copyReleasePortableAssets)
}

tasks.matching { task ->
    task.name.startsWith("run") && task.name.contains("Executable")
}.configureEach {
    dependsOn(copyDebugPortableAssets)
}

tasks.matching { it.name == "packageMacApp" }.configureEach {
    dependsOn(copyReleasePortableAssets)
    doLast {
        copy {
            from(hextrisCoreProject.layout.projectDirectory.dir("assets")) {
                into("assets")
            }
            from(hextrisCoreProject.layout.projectDirectory.dir("sound")) {
                into("sound")
            }
            into(layout.buildDirectory.dir("dist/Hextris-desktop.app/Contents/Resources"))
        }
    }
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
            linkerOpts(PlatformConfig.sharedLibLinkerOpts(
                "SDL3", "SDL3_image", "SDL3_mixer", "SDL3_net", "SDL3_ttf", "chipmunk"
            ))
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
                implementation(project(":games:hextris-core"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by getting {
            dependencies {
                implementation(project(":kengine"))
            }
        }
    }
}
