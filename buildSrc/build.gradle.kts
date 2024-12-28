plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
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
