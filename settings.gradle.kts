pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kengine"

// Playdate requires explicit opt-in via -Pkengine.enablePlaydate=true.
// Kotlin/Native's linuxArm32Hfp emits ARMv4 code incompatible with Playdate's Cortex-M7 (ARMv7E-M)
val isPlaydateEnabled = extra.properties["kengine.enablePlaydate"]?.toString()?.toBoolean() == true
println("Playdate enabled: $isPlaydateEnabled")

val isSwitchEnabled = extra.properties["kengine.enableNintendoSwitch"]?.toString()?.toBoolean() == true
println("Switch tasks enabled: $isSwitchEnabled")

val isN64Enabled = extra.properties["kengine.enableNintendo64"]?.toString()?.toBoolean() == true
println("N64 tasks enabled: $isN64Enabled")

val modules = mutableListOf(
    "kengine-kotlin",
    "kengine-math",
    "kengine-core",
    "kengine",
    "kengine-test",
    "kengine-reactive",
    "kengine-3d",
    "kengine-3d-ui",
    "kengine-3d-importer",
    "kengine-3d-model-viewer",
    "kengine-network",
    "kengine-physics",
    "kengine-sound",
    "kengine-nintendo-switch",
    "kengine-n64"
)

if (isPlaydateEnabled) {
    modules.add("kengine-playdate")
}

modules.addAll(
    listOf(
        "games:antfarm",
        "games:boxxle-core",
        "games:boxxle-desktop",
        "games:boxxle-n64",
        "games:chat-demo",
        "games:kengine-3d-demos",
        "games:kengine-3d-space-shooter",
        "games:helloworld",
        "games:hextris-core",
        "games:hextris-desktop",
        "games:hextris-switch",
        "games:image-shuffle",
        "games:mario-3d",
        "games:nintendo-switch-demo",
        "games:osc3x-synth",
        "games:osc3x-synth-v2",
        "games:physics-demo",
        "games:rubiks-cube-3d",
        "games:nintendo-switch-2d-diagnostics",
        "games:n64-demo",
        "games:snake-n64"
    )
)

include(*modules.toTypedArray())
