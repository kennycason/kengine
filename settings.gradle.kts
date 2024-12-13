pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kengine"

val isPlaydateEnabled = System.getenv("PLAYDATE_SDK_PATH") != null
println("Playdate enabled: $isPlaydateEnabled")

val modules = mutableListOf(
    "kengine",
    "kengine-test"
)

if (isPlaydateEnabled) {
    modules.add("kengine-playdate")
}

modules.addAll(
    listOf(
        "games:boxxle",
        "games:helloworld",
        "games:image-shuffle",
        "games:physics-demo"
    )
)

include(modules)