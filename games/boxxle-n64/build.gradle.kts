plugins {
    id("kengine.n64-game")
}

group = "kengine.boxxle-n64"
version = "1.0.0"

kengineN64 {
    artifactBaseName.set("boxxle-n64")
    displayName.set("Boxxle N64")
    mainClass.set("boxxle.BoxxleN64Game")
    gameSourceProject(project(":games:boxxle-core"))
    assetsFrom(project(":games:boxxle-core"))
}
