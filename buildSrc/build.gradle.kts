plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal() // This is needed for kotlin-dsl plugin dependencies
}

gradlePlugin {
    plugins {
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
