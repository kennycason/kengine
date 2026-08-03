plugins {
    id("kengine.nintendo-switch-game")
}

group = "kengine.hextris-switch"
version = "1.0.0"

kengineNintendoSwitch {
    artifactBaseName.set("hextris-switch")
    displayName.set("Hextris Switch")
    iconSource.set(layout.projectDirectory.file("assets/icon.jpg"))
    mainClass.set("hextris.HextrisGame")
    gameSourceProject(project(":games:hextris-core"))
    assetsFrom(project(":games:hextris-core"))
}
