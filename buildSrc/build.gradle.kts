plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.0")
}

gradlePlugin {
    plugins {
        create("kengineNative") {
            id = "kengine.native"
            implementationClass = "KengineNativePlugin"
        }
        create("kengineAssets") {
            id = "kengine.assets"
            implementationClass = "KengineAssetPlugin"
        }
        create("sdlDylib") {
            id = "kengine.sdl-dylib"
            implementationClass = "SdlDylibPlugin"
        }
    }
}
