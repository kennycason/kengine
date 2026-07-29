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

- Save files / file writing are not supported yet.
- Runtime asset loading from arbitrary files is not part of the portable Switch path.
- The existing `com.kengine.file.File` helper lives in `:kengine`, is SDL/native-host oriented, and is read/path focused; it is not available to `:kengine-core` portable games.
- SDL UI, tiled maps, font contexts, texture-manager APIs, and richer graphics primitives are not yet behind the portable command-buffer surface.
- Portable audio commands are still no-op on the desktop adapter until the SDL audio adapter is wired in.
- Mouse, touch, analog axes, rumble, online services, and 3D rendering are deferred.

## Save Data

The next important platform feature is a portable storage API. We should not expose arbitrary POSIX-style writes directly to portable Kotlin games. The host platform should own storage policy, mount points, atomic writes, quotas, and error translation.

For the public libnx homebrew path, filesystem access is explicit: `fsdev` can mount SD card storage, save data, temporary storage, cache storage, and other filesystems, and savedata writes require an explicit `fsdevCommitDevice()` after writing. See the libnx filesystem device docs: https://switchbrew.github.io/libnx/fs__dev_8h.html

For any commercial Switch SDK path, save-data rules, quotas, user/account behavior, and certification details must be checked in Nintendo's developer documentation. Those details are not public repo assumptions.

Proposed Kengine storage shape:

- Add a `:kengine-core` storage contract with small named records, not paths.
- Restrict record keys to a conservative namespace such as `[A-Za-z0-9._-]`.
- Start with bounded values, for example 16-64 KB per record.
- Support `load`, `save`, `delete`, and `exists`.
- Make host implementations responsible for atomic writes and commit behavior.
- Add a desktop host implementation backed by an app-data directory.
- Add a Switch host implementation backed by a mounted save filesystem or, for homebrew-only smoke tests, an explicit SD-card directory.
- Add a simple demo/Hextris high-score save and reload path as the validation case.

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
- after storage lands: save, quit, relaunch, reload.

## Next Step

The next Switch task should be: add the portable save/storage contract and validate it with a visible 2D game behavior, such as Hextris high score persistence.

Concrete order:

1. Add the storage API and tests in `:kengine-core`.
2. Add a desktop implementation so the contract can be exercised without Switch tooling.
3. Wire a tiny save/reload path into `:games:hextris-core` or the Switch demo.
4. Implement the Switch host storage backend with conservative key validation, bounded writes, atomic replacement, and required commit behavior.
5. Add storage to the Ryujinx manual checklist.

After that, the next 2D work should be a feature matrix pass: sprite transparency/tint/scaling/clipping, text glyph coverage, audio edge cases, input mapping, and lifecycle/reset behavior.
