# Nintendo Switch

Kengine's Switch work is now an experimental, opt-in 2D homebrew backend. It no longer depends on waiting for SDL3-on-libnx support: the Switch host is a small C/libnx application that owns the framebuffer, input, audio output, and NRO packaging, while Kotlin/Native owns portable game state and emits command buffers.

Switch modules are only included when explicitly enabled:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:switchToolchainInfo
```

## Current Direction

Near-term Switch support is focused on proving a complete 2D game loop:

- launch game-specific `.nro` artifacts,
- update pure Kotlin `PortableGame` state,
- read controller input,
- draw software-framebuffer 2D commands,
- render sprites, sprite sheets, text, lines, fills, and gradients,
- play music and one-shot sound commands,
- write and reload save data,
- package NRO metadata and launcher icons per game.

Networking and 3D are intentionally deferred until this 2D baseline is boring and repeatable.

## Kotlin Toolchain Boundary

Normal Kengine modules continue to use the stock Kotlin Gradle plugin pinned in `gradle/libs.versions.toml`. The Switch backend is the only place that uses the Kengine Kotlin/Native fork, and it does that by directly invoking a configured `kotlinc-native` executable.

The fork helper writes the machine-local compiler path to `kengine-kotlin/local.properties`:

```properties
kengine.switch.kotlinNativeHome=/path/to/kengine-kotlin-nintendo-switch/kotlin-native/dist
kengine.switch.kotlinTarget=switch_arm64
```

For one-off overrides, use `-Pkengine.switch.kotlinNativeHome=/path/to/kotlin-native/dist` and `-Pkengine.switch.kotlinTarget=switch_arm64`.

## Working Surface

The shared portable API currently used by Switch lives in `:kengine-core`:

- `Game` / `PortableGame`
- `InputButton` / `InputState`
- `RenderContext`, `RenderCommandBuffer`, `RenderCommandType`
- `AudioContext`, `AudioCommandBuffer`, `AudioCommandType`
- `PortableAssetCatalog`
- `PortableStorage`

The Switch host currently handles these render commands:

- `clear`
- `fillRect`
- `verticalGradient`
- `drawLine`
- `drawSprite`
- `drawText`

Sprite assets are converted during the Gradle build into raw RGBA blobs and embedded into the NRO. Sprite sheets use the same asset declaration path and select frames through the portable render command's `frame` field. This avoids runtime PNG/BMP decoding on Switch for now.

Audio is command-buffer based. Music and declared sound assets are converted to 48 kHz stereo PCM at build time and mixed through `audout`; undeclared sound IDs are reported and skipped. Procedural SFX generation lives in `:kengine-sound` for creating source assets, not as a hidden Switch runtime fallback.

NRO metadata is generated from each `kengineNintendoSwitch` block's `displayName`, `author`, and `version`. Game icons can be configured with `iconSource`; the build converts the source image to the 256x256 JPEG passed to `elf2nro`.

## Known Gaps

- Switch save data has been manually validated in Ryujinx for Hextris high-score persistence, but has not been validated on hardware yet.
- Runtime asset loading from arbitrary files is not part of the portable Switch path.
- The existing `com.kengine.file.File` helper lives in `:kengine`, is SDL/native-host oriented, and is read/path focused; it is not available to `:kengine-core` portable games.
- SDL UI, tiled maps, font contexts, texture-manager APIs, and richer graphics primitives are not yet behind the portable command-buffer surface.
- Portable audio commands are still no-op on the desktop adapter until the SDL audio adapter is wired in.
- Mouse, touch, analog axes, rumble, online services, and 3D rendering are deferred.

## Save Data

The first portable storage path is implemented. Portable games now get a `PortableStorage` instance through `PortableGame.attachStorage(storage)`, and storage is exposed as small named records instead of arbitrary file paths.

For the public libnx homebrew path, filesystem access is explicit: `fsdev` can mount SD card storage, save data, temporary storage, cache storage, and other filesystems, and savedata writes require an explicit `fsdevCommitDevice()` after writing. See the libnx filesystem device docs: https://switchbrew.github.io/libnx/fs__dev_8h.html

For any commercial Switch SDK path, save-data rules, quotas, user/account behavior, and certification details must be checked in Nintendo's developer documentation. Those details are not public repo assumptions.

Current Kengine storage shape:

- `:kengine-core` defines `PortableStorage`, `NoOpPortableStorage`, `InMemoryPortableStorage`, and portable key validation.
- Record keys are restricted to `[A-Za-z0-9._-]` and 64 characters.
- Records are bounded to 64 KB by default.
- The API supports `load`, `save`, `delete`, `exists`, plus string helpers.
- Desktop storage writes to an app-data directory under `~/.kengine/saves/<namespace>/`.
- Switch storage uses a generated cinterop klib for the C host storage ABI.
- The current Switch homebrew backend stores records under `sdmc:/switch/kengine/saves/<namespace>.<key>.dat`.
- Switch writes use temp-file replacement and call `fsdevCommitDevice()` after save/delete.
- Hextris uses this path for high-score persistence and draws it as `BEST`.
- Hextris high-score persistence has been validated across Ryujinx game sessions.

## Verification

Useful build checks:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:switchToolchainInfo
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:validateSwitchKotlinToolchain
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:switchGameInfo
./gradlew :kengine-core:allTests :games:hextris-core:allTests :games:nintendo-switch-demo:allTests
./gradlew -Pkengine.enableNintendoSwitch=true :kengine-nintendo-switch:buildSwitchGameNros
```

Current registered Switch game artifacts:

```text
games/nintendo-switch-demo/build/switch/nintendo-switch-demo.nro
games/hextris-switch/build/switch/hextris-switch.nro
```

Manual Ryujinx validation should cover:

- demo launch and clean exit with `Minus + Plus`,
- D-pad / left-stick movement,
- face-button color and size behavior,
- `L` / `R` speed changes,
- Hextris board rendering,
- Hextris sprite blocks,
- score/level/lines text,
- movement, soft drop, hard drop, rotation, pause, and reset,
- music loop and one-shot sound effects,
- Hextris save, quit, relaunch, and `BEST` reload.

## Next Step

The next Switch task should be a focused 2D diagnostics pass that exercises platform behavior beyond the happy-path Hextris loop.

Concrete order:

1. Add a small Switch-focused diagnostics scene or expand `:games:nintendo-switch-demo`.
2. Cover sprite transparency, tint, scaling, clipping/offscreen draws, and sprite-sheet frame selection.
3. Cover text glyphs, line/fill/gradient edge cases, command-buffer overflow, and lifecycle cleanup/restart.
4. Cover declared SFX playback overlap, sound-only playback, music restart/stop behavior, and volume changes.
5. Validate the diagnostics NRO in Ryujinx, then on hardware when available.

Homebrew SD-card saves are the intended storage target for now.
