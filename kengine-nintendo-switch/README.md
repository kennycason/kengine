# Kengine Nintendo Switch Prototype

Experimental Nintendo Switch homebrew build harness for proving whether Kotlin/Native can participate in a libnx application.
The current prototype discovers game modules that apply `kengine.nintendo-switch-game`, compiles their portable `:kengine-core` lifecycle, input, audio-command, and render-command contracts into a Switch static library, then packages each game as a libnx `.nro`.

This module is opt-in and is not part of the normal repo build:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:switchToolchainInfo
```

## Toolchain

Normal Kengine modules use the stock Kotlin Gradle plugin. This module uses the Kengine Kotlin/Native fork only for its direct `kotlinc-native` and `cinterop` calls.

The fork helper writes the machine-local compiler path to `kengine-kotlin/local.properties`:

```properties
kengine.switch.kotlinNativeHome=/path/to/kengine-kotlin-nintendo-switch/kotlin-native/dist
kengine.switch.kotlinTarget=switch_arm64
```

For one-off overrides, use `-Pkengine.switch.kotlinNativeHome=/path/to/kotlin-native/dist` and `-Pkengine.switch.kotlinTarget=switch_arm64`.

The macOS setup script installs and verifies the public homebrew toolchain:

```bash
./kengine-nintendo-switch/setup-switch-build-macos.sh
```

For unattended setup:

```bash
./kengine-nintendo-switch/setup-switch-build-macos.sh --yes
```

Install the public homebrew toolchain first:

```bash
export DEVKITPRO=/opt/devkitpro
export DEVKITA64=$DEVKITPRO/devkitA64
export PATH=$DEVKITA64/bin:$DEVKITPRO/tools/bin:$PATH
```

Required tools:

```text
aarch64-none-elf-gcc
nacptool
elf2nro
ffmpeg
```

## Milestones

Validate the libnx shell without Kotlin:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:buildSwitchCOnlyNro
```

This artifact still uses the text console and prints the original smoke-test diagnostics.

Compile the Kotlin/Native static-library probe and shared kengine core sources:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:compileNintendoSwitchDemoKotlinStatic
```

Use a local Kotlin/Native compiler fork:

```bash
./kengine-kotlin/setup-kotlin-fork.sh
./kengine-kotlin/build-kotlin-native-dist.sh
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:switchToolchainInfo
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:validateSwitchKotlinToolchain
```

Attempt the Kotlin-linked NRO:

```bash
./kengine-kotlin/build-kotlin-native-dist.sh
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:buildSwitchNro
```

List registered Switch games:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:switchGameInfo
```

Build every registered game-facing Switch NRO:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:buildSwitchGameNros
```

Build the game-facing demo artifact:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :games:nintendo-switch-demo:buildSwitchNro
```

Game artifact:

```text
games/nintendo-switch-demo/build/switch/nintendo-switch-demo.nro
```

Build the game-facing Hextris Switch artifact:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :games:hextris-switch:buildSwitchNro
```

This build imports shared source and assets from `:games:hextris-core`, converts `games/hextris-core/sound/techno_boss_worm.ogg` and declared SFX WAVs to 48 kHz stereo PCM, converts declared sprite assets to raw RGBA, converts the configured icon to a 256x256 JPEG, then embeds/packages them in the NRO.

Game artifact:

```text
games/hextris-switch/build/switch/hextris-switch.nro
```

Build the 2D diagnostics Switch artifact:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :games:nintendo-switch-2d-diagnostics:buildSwitchNro
```

This build exercises the portable 2D contract directly: sprite alpha/tint/scale/clipping/offscreen frames, sprite-sheet frame selection, text glyphs, lines, fills, gradients, SFX overlap, music stop/restart, lifecycle cleanup, and render-command overflow behavior.

Game artifact:

```text
games/nintendo-switch-2d-diagnostics/build/switch/nintendo-switch-2d-diagnostics.nro
```

The same pure Kotlin game can also run through the normal Kengine SDL host:

```bash
./gradlew :games:hextris-desktop:runDebugExecutableMacosArm64
```

## Current Shape

The Kotlin probe uses the local fork's experimental `switch_arm64` Kotlin/Native target. This is still a prototype target, but it now packages through libnx as an `.nro`.

The shared surface is currently intentionally tiny:

```kotlin
com.kengine.Game
com.kengine.PortableGame
com.kengine.input.InputButton
com.kengine.input.InputState
com.kengine.audio.AudioAssetId
com.kengine.audio.AudioCommandBuffer
com.kengine.audio.AudioContext
com.kengine.audio.AudioCommandType
com.kengine.storage.PortableStorage
com.kengine.render.RenderContext
com.kengine.render.RenderCommandBuffer
com.kengine.render.RenderCommandType
```

`GameLoop` and the SDL contexts still live in `:kengine`; the Switch module is only consuming portable lifecycle, input, audio-command, and render-context contracts for now.
The desktop path uses `PortableGameAdapter` plus `RenderContextSdlRenderer` in `:kengine` to execute the same render commands through SDL. Portable audio commands are currently captured there as a no-op surface until the SDL audio adapter is wired in.

The current 2D Switch focus is to make this portable surface complete enough for real games before expanding into networking or 3D. Storage is intentionally exposed as small named records; arbitrary file paths should stay out of portable game code.

## Storage

Switch runtime startup attaches `SwitchPortableStorage` to each `PortableGame`. The storage ABI is declared in `src/main/c/kengine_switch_storage.h`, then each game build generates a matching Kotlin/Native cinterop klib before compiling the game static library.

Current homebrew behavior:

```text
PortableGame.attachStorage(SwitchPortableStorage(namespace))
  -> Kotlin calls generated cinterop bindings
  -> C host validates keys and writes temp files
  -> records land under sdmc:/switch/kengine/saves/<namespace>.<key>.dat
```

The implementation supports `load`, `save`, `delete`, and `exists` for records up to 64 KB. Hextris uses `hextris.high-score.dat` for its visible `BEST` value, and that flow has been validated across Ryujinx game sessions. Hardware validation is still pending.

## Game Wiring

Portable game modules can declare shared asset names with `kengine.portable-assets`:

```kotlin
plugins {
    id("kengine.portable-assets")
}

kenginePortableAssets {
    packageName.set("hextris")
    objectName.set("HextrisAssets")

    spriteSheet("blocks") {
        id.set("hextris/block-sprites")
        source.set(layout.projectDirectory.file("assets/sprites/block_sprites.png"))
        tileWidth.set(24)
        tileHeight.set(24)
        columns.set(6)
    }

    music("theme") {
        id.set("hextris/techno-boss-worm")
        source.set(layout.projectDirectory.file("sound/techno_boss_worm.ogg"))
    }

    sound("rotate") {
        id.set("hextris/rotate")
        source.set(layout.projectDirectory.file("sound/sfx/rotate.wav"))
    }
}
```

The generated asset object implements `com.kengine.assets.PortableAssetCatalog`; portable games can expose it through `PortableGame.assets` so desktop hosts can load the same catalog without repeating sprite IDs in the launcher.

Plain image sprites use the same shared asset DSL without tile metadata:

```kotlin
kenginePortableAssets {
    sprite("title_screen_bg") {
        id.set("title-screen-bg")
        source.set(layout.projectDirectory.file("assets/sprites/title_screen_bg.png"))
    }
}
```

Switch host modules opt into NRO packaging with `kengine.nintendo-switch-game`:

```kotlin
plugins {
    id("kengine.nintendo-switch-game")
}

kengineNintendoSwitch {
    artifactBaseName.set("hextris-switch")
    displayName.set("Hextris Switch")
    iconSource.set(layout.projectDirectory.file("assets/icon.jpg"))
    mainClass.set("hextris.HextrisGame")
    gameSourceProject(project(":games:hextris-core"))
    assetsFrom(project(":games:hextris-core"))
}
```

The game module gets a stable task:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :games:hextris-switch:buildSwitchNro
```

The backend registers matching tasks from the game metadata, such as `:kengine-nintendo-switch:buildHextrisSwitchNro`, and writes intermediate outputs under `kengine-nintendo-switch/build/switch/games/<artifact>/`.

The current successful artifact is:

```text
libnx C main()
  -> calls generated Kotlin/Native static-library API
  -> starts a Kotlin KengineSwitchRuntime with a generated per-game PortableGame factory
  -> translates libnx buttons into com.kengine.input.InputState
  -> updates, emits audio commands, and draws the selected PortableGame every app-loop frame
  -> copies Kotlin audio commands into a C-owned command buffer
  -> keeps requested music loops playing through libnx audout
  -> mixes requested declared one-shot SFX into the active PCM stream
  -> builds the frame through com.kengine.render.RenderContext
  -> copies Kotlin render commands into a C-owned command buffer
  -> renders a software framebuffer through libnx
  -> packages as .nro
```

Runtime controls for `games/nintendo-switch-demo/build/switch/nintendo-switch-demo.nro`:

```text
D-pad / left stick: move the square
A: shift the color palette faster
B: pulse the square size
X: shift the color palette faster
Y: reverse the palette shift
L / ZL: slow manual movement
R / ZR: speed up manual movement
Minus: select
Minus + Plus: exit
```
