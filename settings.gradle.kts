pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kengine"

val isPlaydateEnabled = System.getenv("PLAYDATE_SDK_PATH") != null
println("Playdate enabled: $isPlaydateEnabled")

val modules = mutableListOf("kengine")
if (isPlaydateEnabled) {
    modules.add("kengine-playdate")
}
modules.addAll(listOf("games:boxxle", "games:helloworld", "games:image-shuffle"))

include(modules)