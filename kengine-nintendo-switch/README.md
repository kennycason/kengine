# Kengine Nintendo Switch Prototype

Experimental Nintendo Switch homebrew build harness for proving whether Kotlin/Native can participate in a libnx application.
The current prototype compiles small shared `:kengine-core` lifecycle, input, and render-command contracts plus the `:games:nintendo-switch-demo` game into the Switch static library while keeping the existing SDL-backed engine loop in `:kengine`.

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
```

## Milestones

Validate the libnx shell without Kotlin:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:buildSwitchCOnlyNro
```

This artifact still uses the text console and prints the original smoke-test diagnostics.

Compile the Kotlin/Native static-library probe and shared kengine core sources:

```bash
jenv exec ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:compileSwitchKotlinStatic
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

Build the game-facing demo artifact:

```bash
jenv exec ./gradlew -Pkengine.switch=true :games:nintendo-switch-demo:buildSwitchNro
```

Game artifact:

```text
games/nintendo-switch-demo/build/switch/nintendo-switch-demo.nro
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
com.kengine.render.RenderContext
com.kengine.render.RenderCommandBuffer
com.kengine.render.RenderCommandType
```

`GameLoop` and the SDL contexts still live in `:kengine`; the Switch module is only consuming portable lifecycle, input, and render-context contracts for now.
The desktop path uses `PortableGameAdapter` plus `RenderContextSdlRenderer` in `:kengine` to execute the same render commands through SDL.

The current successful artifact is:

```text
libnx C main()
  -> calls generated Kotlin/Native static-library API
  -> starts a Kotlin KengineSwitchRuntime
  -> translates libnx buttons into com.kengine.input.InputState
  -> updates and draws the :games:nintendo-switch-demo PortableGame every app-loop frame
  -> builds the frame through com.kengine.render.RenderContext
  -> copies Kotlin render commands into a C-owned command buffer
  -> renders a software framebuffer through libnx
  -> packages as .nro
```

Runtime controls for `build/switch/kengine-nintendo-switch.nro`:

```text
D-pad / left stick: move the square
A: shift the color palette faster
B: pulse the square size
+: exit
```
