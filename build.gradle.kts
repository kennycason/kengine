plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

allprojects {
    repositories {
        mavenCentral()
    }
}
