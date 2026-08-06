plugins {
    id("kengine.n64-game")
}

group = "kengine.boxxle-n64"
version = "1.0.0"

val boxxleCore = project(":games:boxxle-core")

kengineN64 {
    artifactBaseName.set("boxxle-n64")
    displayName.set("Boxxle N64")
    mainClass.set("boxxle.BoxxleN64Game")
    gameSourceProject(boxxleCore)
    assetsFrom(boxxleCore)
    music("main") {
        id.set("boxxle/main")
        source.set(boxxleCore.layout.projectDirectory.file("assets/sounds/main.wav"))
    }
}
