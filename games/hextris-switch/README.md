# Hextris Switch

Nintendo Switch NRO host for the shared portable Hextris game in `:games:hextris-core`. This module declares Switch artifact metadata and the launcher icon; source, sprite, music, and SFX assets are imported from core through `gameSourceProject(...)` and `assetsFrom(...)`.

The full Switch Gradle config is intentionally just metadata plus the shared core module:

```kotlin
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
```

Build:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :games:hextris-switch:buildSwitchNro
```

Output:

```text
games/hextris-switch/build/switch/hextris-switch.nro
```

Controls:

```text
D-pad left/right: move
D-pad down: soft drop
D-pad up: hard drop
A / R: rotate clockwise
B / L / Y: rotate counter-clockwise
X: rotate 180 degrees
Start: pause
Select: reset
Minus + Plus: exit Switch runtime
```
