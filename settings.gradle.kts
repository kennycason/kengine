pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kengine"

val isPlaydateEnabled = false // System.getenv("PLAYDATE_SDK_PATH") != null
println("Playdate enabled: $isPlaydateEnabled")

val modules = mutableListOf(
    "kengine",
    "kengine-test",
    "kengine-reactive",
    "kengine-network",
    "kengine-physics",
    "kengine-sound"
)

if (isPlaydateEnabled) {
    modules.add("kengine-playdate")
}

modules.addAll(
    listOf(
        "games:boxxle",
        "games:helloworld",
        "games:image-shuffle",
        "games:osc3x-synth",
        "games:osc3x-synth-v2",
        "games:physics-demo"
    )
)

include(*modules.toTypedArray())
