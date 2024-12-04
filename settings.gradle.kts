pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "kengine"

val isPlaydateEnabled = true
val modules = mutableListOf("kengine", "boxxle", "helloworld")
if (isPlaydateEnabled) {
    modules.add("isPlaydateEnabled")
}

include(modules)