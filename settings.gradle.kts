pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kengine"

// Playdate requires explicit opt-in via -Pkengine.playdate=true
// Kotlin/Native's linuxArm32Hfp emits ARMv4 code incompatible with Playdate's Cortex-M7 (ARMv7E-M)
val isPlaydateEnabled = extra.properties["kengine.playdate"]?.toString()?.toBoolean() == true
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
        "games:antfarm",
        "games:boxxle",
        "games:helloworld",
        "games:hextris",
        "games:image-shuffle",
        "games:osc3x-synth",
        "games:osc3x-synth-v2",
        "games:physics-demo"
    )
)

include(*modules.toTypedArray())
