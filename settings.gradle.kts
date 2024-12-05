pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kengine"

val isPlaydateEnabled = System.getenv("PLAYDATE_SDK_PATH") != null
println("Playdate enabled: $isPlaydateEnabled")
val modules = mutableListOf("kengine", "boxxle", "helloworld")
if (isPlaydateEnabled) {
    modules.add("kengine-playdate")
}

include(modules)