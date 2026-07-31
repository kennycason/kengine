plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.portable-assets")
}

group = "kengine.hextris-core"
version = "1.0.0"

repositories {
    mavenCentral()
}

kenginePortableAssets {
    packageName.set("hextris")
    objectName.set("HextrisAssets")

    spriteSheet("blocks") {
        id.set("hextris/block-sprites")
        source.set(layout.projectDirectory.file("assets/sprites/block_sprites.png"))
        tileWidth.set(24)
        tileHeight.set(24)
        columns.set(6)
    }

    music("theme") {
        id.set("hextris/techno-boss-worm")
        source.set(layout.projectDirectory.file("sound/techno_boss_worm.ogg"))
    }

    sound("rotate") {
        id.set("hextris/rotate")
        source.set(layout.projectDirectory.file("sound/sfx/rotate.wav"))
    }

    sound("hard-drop") {
        id.set("hextris/hard-drop")
        source.set(layout.projectDirectory.file("sound/sfx/hard-drop.wav"))
    }

    sound("lock") {
        id.set("hextris/lock")
        source.set(layout.projectDirectory.file("sound/sfx/lock.wav"))
    }

    sound("line-clear") {
        id.set("hextris/line-clear")
        source.set(layout.projectDirectory.file("sound/sfx/line-clear.wav"))
    }

    sound("game-over") {
        id.set("hextris/game-over")
        source.set(layout.projectDirectory.file("sound/sfx/game-over.wav"))
    }

    sound("pause") {
        id.set("hextris/pause")
        source.set(layout.projectDirectory.file("sound/sfx/pause.wav"))
    }

    sound("reset") {
        id.set("hextris/reset")
        source.set(layout.projectDirectory.file("sound/sfx/reset.wav"))
    }
}

kotlin {
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

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kengine-core"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
