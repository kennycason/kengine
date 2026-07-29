# Kengine Nintendo Switch Prototype

Experimental Nintendo Switch homebrew build harness for proving whether Kotlin/Native can participate in a libnx application.
The current prototype discovers game modules that apply `kengine.nintendo-switch-game`, compiles their portable `:kengine-core` lifecycle, input, audio-command, and render-command contracts into a Switch static library, then packages each game as a libnx `.nro`.

This module is opt-in and is not part of the normal repo build:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:switchToolchainInfo
```

## Toolchain

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
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:buildSwitchCOnlyNro
```

This artifact still uses the text console and prints the original smoke-test diagnostics.

Compile the Kotlin/Native static-library probe and shared kengine core sources:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:compileNintendoSwitchDemoKotlinStatic
```

Use a local Kotlin/Native compiler fork:

```bash
./kengine-kotlin/setup-kotlin-fork.sh
./kengine-kotlin/build-kotlin-native-dist.sh
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:switchToolchainInfo
```

Attempt the Kotlin-linked NRO:

```bash
jenv exec ./kengine-kotlin/build-kotlin-native-dist.sh
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:buildSwitchNro
```

List registered Switch games:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:switchGameInfo
```

Build every registered Switch game:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:buildSwitchGameNros
```

Build the game-facing demo artifact:

```bash
jenv exec ./gradlew -Pkengine.switch=true :games:nintendo-switch-demo:buildSwitchNro
```

Game artifact:

```text
games/nintendo-switch-demo/build/switch/nintendo-switch-demo.nro
```

Build the game-facing Hextris Switch artifact:

```bash
jenv exec ./gradlew -Pkengine.switch=true :games:hextris-switch:buildSwitchNro
```

This build converts `games/hextris-switch/sound/techno_boss_worm.ogg` to 48 kHz stereo PCM and `games/hextris-switch/assets/sprites/block_sprites.png` to raw RGBA, then embeds both in the NRO. Hextris SFX are currently short procedural voices mixed into the same audio stream from portable `playSound` commands.

Game artifact:

```text
games/hextris-switch/build/switch/hextris-switch.nro
```

The same pure Kotlin game can also run through the normal Kengine SDL host:

```bash
jenv exec ./gradlew :games:nintendo-switch-demo:runDebugExecutableMacosArm64
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
com.kengine.render.RenderContext
com.kengine.render.RenderCommandBuffer
com.kengine.render.RenderCommandType
```

`GameLoop` and the SDL contexts still live in `:kengine`; the Switch module is only consuming portable lifecycle, input, audio-command, and render-context contracts for now.
The desktop path uses `PortableGameAdapter` plus `RenderContextSdlRenderer` in `:kengine` to execute the same render commands through SDL. Portable audio commands are currently captured there as a no-op surface until the SDL audio adapter is wired in.

## Game Wiring

Game modules opt into Switch packaging with the convention plugin:

```kotlin
plugins {
    id("kengine.nintendo-switch-game")
}

kengineNintendoSwitch {
    displayName.set("Hextris Switch")
    mainClass.set("hextrisswitch.HextrisSwitchGame")
    musicSource.set(layout.projectDirectory.file("sound/techno_boss_worm.ogg"))
    blockSpriteSheetSource.set(layout.projectDirectory.file("assets/sprites/block_sprites.png"))
}
```

The game module gets a stable task:

```bash
jenv exec ./gradlew -Pkengine.switch=true :games:hextris-switch:buildSwitchNro
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
  -> mixes requested one-shot SFX into the active PCM stream
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
