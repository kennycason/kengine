plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

repositories {
    mavenCentral()
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
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.kotlinxCoroutinesCore)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        nativeMain {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":kengine-test"))
            }
        }

        nativeTest {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
