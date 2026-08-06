plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.portable-assets")
}

group = "kengine.boxxle-core"
version = "1.0.0"

repositories {
    mavenCentral()
}

kenginePortableAssets {
    packageName.set("boxxle")
    objectName.set("BoxxleAssets")

    spriteSheet("tiles") {
        id.set("boxxle/tiles")
        source.set(layout.projectDirectory.file("assets/sprites/boxxle.bmp"))
        tileWidth.set(32)
        tileHeight.set(32)
        columns.set(4)
    }

    music("main") {
        id.set("boxxle/main")
        source.set(layout.projectDirectory.file("assets/sounds/main.wav"))
    }

    music("title") {
        id.set("boxxle/title")
        source.set(layout.projectDirectory.file("assets/sounds/title.wav"))
    }

    sound("finish") {
        id.set("boxxle/finish")
        source.set(layout.projectDirectory.file("assets/sounds/finish.wav"))
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
