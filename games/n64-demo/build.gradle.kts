plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.n64-game")
}

group = "kengine.n64.demo"
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

    when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64()
        hostOs == "Mac OS X" && !isArm64 -> macosX64()
        hostOs == "Linux" && isArm64 -> linuxArm64()
        hostOs == "Linux" && !isArm64 -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS [$hostOs] is not supported in Kotlin/Native.")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":kengine-core"))
        }
    }
}

kengineN64 {
    artifactBaseName.set("n64-demo")
    displayName.set("Kengine N64 Demo")
    mainClass.set("n64demo.N64DemoGame")

    sprite("pokeball") {
        source.set(project.file("assets/sprites/pokeball.bmp"))
    }

    sound("finish") {
        source.set(project.file("assets/sounds/finish.wav"))
    }

    sound("chord") {
        source.set(project.file("assets/sounds/chord.wav"))
    }
}
