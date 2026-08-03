# Hextris Desktop

SDL desktop host for the shared portable Hextris game in `:games:hextris-core`. This module owns the native executable and launcher; gameplay, layout, sprite names, and asset declarations live in core and are loaded from `HextrisGame.assets`.

The desktop Gradle file points at core for runtime assets:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("kengine.packaging")
}

group = "kengine.hextris-desktop"
version = "1.0.0"

configureKenginePortableGameAssets(project(":games:hextris-core"))
```

The launcher is just a normal Kengine context running the portable game:

```kotlin
import com.kengine.PortableGameRunner
import com.kengine.createGameContext
import com.kengine.log.Logger
import hextris.HextrisGame

fun main() {
    createGameContext(
        title = "Kengine - Hextris Desktop",
        width = 1280,
        height = 720,
        logLevel = Logger.Level.INFO
    ) {
        PortableGameRunner(
            frameRate = 60,
            commandCapacity = 1024
        ) {
            HextrisGame()
        }
    }
}
```

Run:

```bash
./gradlew :games:hextris-desktop:runDebugExecutableMacosArm64
```

Build:

```bash
./gradlew :games:hextris-desktop:linkDebugExecutableMacosArm64
```

The desktop window is 1280x720 to match the Switch layout.

Controls:

```text
Left/right arrows or A/D: move
Down arrow / S: soft drop
Up arrow / W: hard drop
Space / J / E / right shift: rotate clockwise
B / K / Y / I / Q / left shift: rotate counter-clockwise
X / U: rotate 180 degrees
Return / Escape: pause
Tab / Backspace: reset
```
