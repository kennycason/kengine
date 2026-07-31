plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.assets")
    id("kengine.portable-assets")
    id("kengine.nintendo-switch-game")
}

group = "kengine.nintendo-switch-2d-diagnostics"
version = "1.0.0"

repositories {
    mavenCentral()
}

val diagnosticsSpriteSheet = layout.projectDirectory.file("assets/sprites/diagnostics_sprites.png")
val diagnosticsIcon = layout.projectDirectory.file("assets/icons/nintendo-switch-2d-diagnostics.jpg")
val diagnosticsMusic = layout.projectDirectory.file("sound/music-loop.wav")
val diagnosticsBlip = layout.projectDirectory.file("sound/sfx/blip.wav")
val diagnosticsChord = layout.projectDirectory.file("sound/sfx/chord.wav")
val diagnosticsNoise = layout.projectDirectory.file("sound/sfx/noise.wav")

kenginePortableAssets {
    packageName.set("switchdiagnostics")
    objectName.set("Switch2dDiagnosticsAssets")

    spriteSheet("sprites") {
        id.set("diagnostics/sprites")
        source.set(diagnosticsSpriteSheet)
        tileWidth.set(32)
        tileHeight.set(32)
        columns.set(4)
    }

    music("pulse") {
        id.set("diagnostics/pulse")
        source.set(diagnosticsMusic)
    }

    sound("blip") {
        id.set("diagnostics/blip")
        source.set(diagnosticsBlip)
    }

    sound("chord") {
        id.set("diagnostics/chord")
        source.set(diagnosticsChord)
    }

    sound("noise") {
        id.set("diagnostics/noise")
        source.set(diagnosticsNoise)
    }
}

kengineNintendoSwitch {
    artifactBaseName.set("nintendo-switch-2d-diagnostics")
    displayName.set("Kengine 2D Diagnostics")
    iconSource.set(diagnosticsIcon)
    mainClass.set("switchdiagnostics.Switch2dDiagnosticsGame")

    spriteSheet("sprites") {
        id.set("diagnostics/sprites")
        source.set(diagnosticsSpriteSheet)
        tileWidth.set(32)
        tileHeight.set(32)
        columns.set(4)
    }

    musicSource.set(diagnosticsMusic)

    sound("blip") {
        id.set("diagnostics/blip")
        source.set(diagnosticsBlip)
    }

    sound("chord") {
        id.set("diagnostics/chord")
        source.set(diagnosticsChord)
    }

    sound("noise") {
        id.set("diagnostics/noise")
        source.set(diagnosticsNoise)
    }
}

tasks.matching { task ->
    task.name.startsWith("run") && task.name.contains("Executable")
}.configureEach {
    dependsOn("copyDebugAssets")
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
