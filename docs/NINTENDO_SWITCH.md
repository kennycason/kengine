# Nintendo Switch

Kengine's Switch work is now an experimental, opt-in 2D homebrew backend. It no longer depends on waiting for SDL3-on-libnx support: the Switch host is a small C/libnx application that owns the framebuffer, input, audio output, and NRO packaging, while Kotlin/Native owns portable game state and emits command buffers.

Switch modules are only included when Gradle is run with:

```bash
-Pkengine.switch=true
```

## Current Direction

Near-term Switch support is focused on proving a complete 2D game loop:

- launch game-specific `.nro` artifacts,
- update pure Kotlin `PortableGame` state,
- read controller input,
- draw software-framebuffer 2D commands,
- render sprites, sprite sheets, text, lines, fills, and gradients,
- play music and one-shot sound commands,
- write and reload save data.

Networking and 3D are intentionally deferred until this 2D baseline is boring and repeatable.

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

Audio is command-buffer based. Music assets are converted to 48 kHz stereo PCM at build time and mixed through `audout`; known Hextris sound effects are currently procedural one-shot voices.

## Known Gaps

- Switch save data has a homebrew smoke-test backend, but has not been manually validated in Ryujinx or on hardware yet.
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

## Local Java

Switch Gradle tasks require a real JDK 17. The repository `.java-version` currently selects `17.0`, but local shell setup can still override it.

If Gradle fails with:

```text
JAVA_HOME=/Users/kenny/.jenv/versions/system
```

the problem is local shell state, not the Switch build. In the current environment, `JENV_FORCEJAVAHOME=true` exports that invalid `JAVA_HOME`, and `PATH` resolves `/usr/bin/java` before jenv shims. Use an explicit per-command Java home until shell init is fixed:

```bash
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:switchToolchainInfo
```

Longer-term, fix shell init so `~/.jenv/shims` appears before `/usr/bin` and so `JAVA_HOME` is not forced to `.jenv/versions/system`.

## Verification

Useful build checks:

```bash
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:switchToolchainInfo
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew -Pkengine.switch=true :kengine-nintendo-switch:switchGameInfo
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew :kengine-core:allTests :games:hextris-core:allTests :games:nintendo-switch-demo:allTests
JAVA_HOME="$(jenv prefix 17.0)" ./gradlew -Pkengine.switch=true :games:nintendo-switch-demo:buildSwitchNro :games:hextris-switch:buildSwitchNro
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

The next Switch task should be manual validation of Hextris high-score persistence in Ryujinx, then on hardware when available.

Concrete order:

1. Launch `games/hextris-switch/build/switch/hextris-switch.nro` in Ryujinx.
2. Create a score above `0` and confirm `BEST` updates.
3. Exit cleanly with `Minus + Plus`.
4. Relaunch and confirm `BEST` reloads from storage.
5. Inspect the emulator SD-card filesystem for `switch/kengine/saves/hextris.high-score.dat` if reload fails.
6. Decide whether to keep the SD-card homebrew backend for now or move to a true mounted save-data backend next.

After that, the next 2D work should be a feature matrix pass: sprite transparency/tint/scaling/clipping, text glyph coverage, audio edge cases, input mapping, and lifecycle/reset behavior.
